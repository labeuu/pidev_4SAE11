import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterModule } from '@angular/router';

import { Ticket, TicketService } from '../../../../core/services/ticket.service';
import { UserService } from '../../../../core/services/user.service';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';

type StatusFilter = 'ALL' | 'OPEN' | 'CLOSED';

@Component({
  selector: 'app-admin-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.scss',
})
export class AdminTicketList implements OnInit {
  tickets: Ticket[] = [];
  unreadByTicket: Record<number, number> = {};
  /** userId -> "First Last" or email snippet */
  userLabels = new Map<number, string>();
  loading = true;
  errorMessage = '';
  statusFilter: StatusFilter = 'ALL';

  constructor(
    private ticketService: TicketService,
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.loading = true;
    this.errorMessage = '';
    this.ticketService.getAll().subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.resolveUserLabels(tickets);
        this.ticketService.getUnreadCounts().subscribe({
          next: (rows) => {
            this.unreadByTicket = Object.fromEntries(rows.map((r) => [r.ticketId, r.unreadCount]));
          },
          error: () => {
            this.unreadByTicket = {};
          },
          complete: () => {
            this.loading = false;
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

  setFilter(f: StatusFilter): void {
    this.statusFilter = f;
  }

  get filteredTickets(): Ticket[] {
    if (this.statusFilter === 'ALL') return this.tickets;
    return this.tickets.filter((t) => t.status === this.statusFilter);
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
