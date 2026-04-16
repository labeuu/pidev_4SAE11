import { Component, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, interval } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Ticket, TicketListQuery, TicketPriority, TicketService, TicketStatus } from '../../../../core/services/ticket.service';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';

@Component({
  selector: 'app-admin-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, FormsModule],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.scss',
})
export class AdminTicketList implements OnInit, OnDestroy {
  tickets: Ticket[] = [];
  unreadByTicket: Record<number, number> = {};
  /** userId -> "First Last" or email snippet */
  userLabels = new Map<number, string>();
  loading = true;
  errorMessage = '';
  searchTerm = '';
  selectedStatus: 'ALL' | TicketStatus = 'ALL';
  selectedPriority: 'ALL' | TicketPriority = 'ALL';
  selectedSortBy: 'lastActivityAt' | 'createdAt' | 'priority' = 'lastActivityAt';
  selectedSortDir: 'asc' | 'desc' = 'desc';
  page = 0;
  size = 10;
  totalPages = 0;
  totalElements = 0;
  lastUpdatedAt: Date | null = null;
  private readonly searchInput$ = new Subject<string>();
  private searchSub?: Subscription;
  private pollSub?: Subscription;

  constructor(
    private readonly ticketService: TicketService,
    private readonly userService: UserService,
    private readonly toast: ToastService,
    private readonly router: Router,
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
      status: this.selectedStatus === 'ALL' ? undefined : this.selectedStatus,
      priority: this.selectedPriority === 'ALL' ? undefined : this.selectedPriority,
      sortBy: this.selectedSortBy,
      sortDir: this.selectedSortDir,
      page: this.page,
      size: this.size,
    };
  }

  loadTickets(showLoading = true): void {
    if (showLoading) {
      this.loading = true;
    }
    this.errorMessage = '';
    this.ticketService.getAll(this.buildQuery()).subscribe({
      next: (response) => {
        this.tickets = response.items;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.page = response.currentPage;
        this.resolveUserLabels(response.items);
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

  onSearchTermChange(value: string): void {
    this.searchInput$.next(value);
  }

  setStatusFilter(value: 'ALL' | TicketStatus): void {
    this.selectedStatus = value;
    this.page = 0;
    this.loadTickets();
  }

  setPriorityFilter(value: 'ALL' | TicketPriority): void {
    this.selectedPriority = value;
    this.page = 0;
    this.loadTickets();
  }

  setSort(sortBy: 'lastActivityAt' | 'createdAt' | 'priority'): void {
    if (this.selectedSortBy === sortBy) {
      this.selectedSortDir = this.selectedSortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.selectedSortBy = sortBy;
      this.selectedSortDir = 'desc';
    }
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

  userLabel(userId: number): string {
    return this.userLabels.get(userId) ?? `User #${userId}`;
  }

  private resolveUserLabels(tickets: Ticket[]): void {
    this.userLabels.clear();
    const ids = [...new Set(tickets.map((t) => t.userId))];
    let pending = ids.length;
    if (pending === 0) return;
    for (const id of ids) {
      this.userService.getById(id).subscribe({
        next: (u) => {
          if (u) {
            const name = `${u.firstName} ${u.lastName}`.trim();
            this.userLabels.set(id, name || u.email);
          } else {
            this.userLabels.set(id, `User #${id}`);
          }
          pending--;
          if (pending === 0) this.cdr.detectChanges();
        },
        error: () => {
          this.userLabels.set(id, `User #${id}`);
          pending--;
          if (pending === 0) this.cdr.detectChanges();
        },
      });
    }
  }

  openTicket(t: Ticket): void {
    this.router.navigate(['/admin/tickets', t.id]);
  }

  assignToMe(t: Ticket): void {
    this.ticketService.assign(t.id).subscribe({
      next: () => {
        this.toast.success('Ticket assigned to you.');
        this.loadTickets(false);
      },
      error: (err: unknown) => {
        this.toast.error(messageFromHttpError(err, 'Failed to assign ticket.'));
      },
    });
  }

  unassign(t: Ticket): void {
    this.ticketService.unassign(t.id).subscribe({
      next: () => {
        this.toast.success('Ticket unassigned.');
        this.loadTickets(false);
      },
      error: (err: unknown) => {
        this.toast.error(messageFromHttpError(err, 'Failed to unassign ticket.'));
      },
    });
  }

  closeTicket(t: Ticket): void {
    if (t.status === 'CLOSED') return;
    this.ticketService.close(t.id).subscribe({
      next: () => {
        this.toast.success('Ticket closed.');
        this.loadTickets(false);
      },
      error: (err: unknown) => {
        this.toast.error(messageFromHttpError(err, 'Failed to close ticket.'));
      },
    });
  }

  reopenTicket(t: Ticket): void {
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
