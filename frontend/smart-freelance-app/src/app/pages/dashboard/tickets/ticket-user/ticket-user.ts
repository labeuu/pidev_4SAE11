import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/services/auth.service';
import { Ticket, TicketService } from '../../../../core/services/ticket.service';
import { ToastService } from '../../../../core/services/toast.service';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';

type StatusFilter = 'ALL' | 'OPEN' | 'CLOSED';

@Component({
  selector: 'app-ticket-user',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ticket-user.html',
  styleUrl: './ticket-user.scss',
})
export class TicketUser implements OnInit {
  tickets: Ticket[] = [];
  statusFilter: StatusFilter = 'ALL';
  loading = true;
  syncingProfile = false;
  errorMessage = '';

  constructor(
    private ticketService: TicketService,
    private auth: AuthService,
    private router: Router,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.errorMessage = '';
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
          this.fetchTicketsForUser(userId);
        });
      return;
    }
    this.fetchTicketsForUser(userId);
  }

  private fetchTicketsForUser(userId: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.ticketService.getByUserId(userId).subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        this.errorMessage = messageFromHttpError(err, 'Failed to load tickets.');
        this.toast.error(this.errorMessage);
        this.tickets = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openTicket(t: Ticket): void {
    this.router.navigate(['/dashboard/tickets', t.id]);
  }

  newTicket(): void {
    this.router.navigate(['/dashboard/tickets/new']);
  }

  setFilter(f: StatusFilter): void {
    this.statusFilter = f;
  }

  get filteredTickets(): Ticket[] {
    if (this.statusFilter === 'ALL') return this.tickets;
    return this.tickets.filter((t) => t.status === this.statusFilter);
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
