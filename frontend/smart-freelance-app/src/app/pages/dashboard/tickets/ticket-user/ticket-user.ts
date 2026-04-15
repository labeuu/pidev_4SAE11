import { Component, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, interval } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { Ticket, TicketListQuery, TicketPriority, TicketService, TicketStatus } from '../../../../core/services/ticket.service';
import { ToastService } from '../../../../core/services/toast.service';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs/operators';

@Component({
  selector: 'app-ticket-user',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './ticket-user.html',
  styleUrl: './ticket-user.scss',
})
export class TicketUser implements OnInit, OnDestroy {
  tickets: Ticket[] = [];
  /** ticketId -> unread admin reply count */
  unreadByTicket: Record<number, number> = {};
  statusFilter: 'ALL' | TicketStatus = 'ALL';
  priorityFilter: 'ALL' | TicketPriority = 'ALL';
  searchTerm = '';
  page = 0;
  size = 10;
  totalPages = 0;
  totalElements = 0;
  lastUpdatedAt: Date | null = null;
  loading = true;
  syncingProfile = false;
  errorMessage = '';
  private readonly searchInput$ = new Subject<string>();
  private searchSub?: Subscription;
  private pollSub?: Subscription;

  constructor(
    private readonly ticketService: TicketService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.searchSub = this.searchInput$
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((value) => {
        this.searchTerm = value;
        this.page = 0;
        this.loadTickets();
      });
    this.pollSub = interval(10000).subscribe(() => {
      if (!document.hidden) {
        this.loadTickets(false);
      }
    });
    this.loadTickets();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
    this.pollSub?.unsubscribe();
  }

  private buildQuery(): TicketListQuery {
    return {
      q: this.searchTerm || undefined,
      status: this.statusFilter === 'ALL' ? undefined : this.statusFilter,
      priority: this.priorityFilter === 'ALL' ? undefined : this.priorityFilter,
      sortBy: 'lastActivityAt',
      sortDir: 'desc',
      page: this.page,
      size: this.size,
    };
  }

  loadTickets(showLoading = true): void {
    this.errorMessage = '';
    if (showLoading) {
      this.loading = true;
    }
    let userId = this.auth.getUserId();
    if (userId == null) {
      this.loading = true;
      this.syncingProfile = true;
      this.auth
        .fetchUserProfile()
        .pipe(
          finalize(() => {
            this.syncingProfile = false;
            this.cdr.detectChanges();
          })
        )
        .subscribe((profile) => {
          userId = profile?.id ?? null;
          if (userId == null) {
            this.loading = false;
            this.errorMessage =
              'Your profile could not be loaded. Sign out, sign in again, then open Support.';
            this.toast.error(this.errorMessage);
            return;
          }
          this.fetchTicketsForUser(userId, showLoading);
        });
      return;
    }
    this.fetchTicketsForUser(userId, showLoading);
  }

  private fetchTicketsForUser(userId: number, showLoading: boolean): void {
    if (showLoading) {
      this.loading = true;
    }
    this.errorMessage = '';
    this.ticketService.getByUserId(userId, this.buildQuery()).subscribe({
      next: (response) => {
        this.tickets = response.items;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.page = response.currentPage;
        this.ticketService.getUnreadCounts().subscribe({
          next: (rows) => {
            this.unreadByTicket = Object.fromEntries(rows.map((r) => [r.ticketId, r.unreadCount]));
          },
          error: () => {
            this.unreadByTicket = {};
          },
          complete: () => {
            this.loading = false;
            this.lastUpdatedAt = new Date();
            this.cdr.detectChanges();
          },
        });
      },
      error: (err: unknown) => {
        this.errorMessage = messageFromHttpError(err, 'Failed to load tickets.');
        this.toast.error(this.errorMessage);
        this.tickets = [];
        this.unreadByTicket = {};
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  unreadCount(ticketId: number): number {
    return this.unreadByTicket[ticketId] ?? 0;
  }

  openTicket(t: Ticket): void {
    this.router.navigate(['/dashboard/tickets', t.id]);
  }

  newTicket(): void {
    this.router.navigate(['/dashboard/tickets/new']);
  }

  onSearchTermChange(value: string): void {
    this.searchInput$.next(value);
  }

  setFilter(f: 'ALL' | TicketStatus): void {
    this.statusFilter = f;
    this.page = 0;
    this.loadTickets();
  }

  setPriorityFilter(value: 'ALL' | TicketPriority): void {
    this.priorityFilter = value;
    this.page = 0;
    this.loadTickets();
  }

  prevPage(): void {
    if (this.page <= 0) return;
    this.page--;
    this.loadTickets();
  }

  nextPage(): void {
    if (this.page + 1 >= this.totalPages) return;
    this.page++;
    this.loadTickets();
  }

  markTicketRead(t: Ticket): void {
    this.ticketService.markRead(t.id).subscribe({
      next: () => {
        this.unreadByTicket[t.id] = 0;
      },
      error: () => {},
    });
  }

  reopenFromList(t: Ticket): void {
    if (!(t.canReopen ?? (t.reopenCount ?? 0) < 1)) return;
    this.ticketService.reopen(t.id).subscribe({
      next: () => {
        this.toast.success('Ticket reopened.');
        this.loadTickets(false);
      },
      error: (err: unknown) => {
        this.toast.error(messageFromHttpError(err, 'Failed to reopen ticket.'));
      },
    });
  }

  openFirstUnread(): void {
    const first = this.tickets.find((t) => this.unreadCount(t.id) > 0);
    if (!first) return;
    this.openTicket(first);
  }

  badgeStatusClasses(status: string): Record<string, boolean> {
    return {
      'tk-pill': true,
      'tk-pill--open': status === 'OPEN',
      'tk-pill--closed': status !== 'OPEN',
    };
  }

  badgePriorityClasses(p: string): Record<string, boolean> {
    return {
      'tk-pill': true,
      'tk-pill--high': p === 'HIGH',
      'tk-pill--medium': p === 'MEDIUM',
      'tk-pill--low': p === 'LOW' || (p !== 'HIGH' && p !== 'MEDIUM'),
    };
  }
}
