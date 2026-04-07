import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { Ticket, TicketService } from '../../../../core/services/ticket.service';

@Component({
  selector: 'app-ticket-user',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ticket-user.html',
  styleUrl: './ticket-user.scss',
})
export class TicketUser implements OnInit {
  tickets: Ticket[] = [];
  loading = true;
  errorMessage = '';

  constructor(
    private ticketService: TicketService,
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    const userId = this.auth.getUserId();
    if (!userId) {
      this.loading = false;
      this.errorMessage = 'Missing user profile. Please re-login.';
      this.cdr.detectChanges();
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.ticketService.getByUserId(userId).subscribe({
      next: (tickets) => {
        this.tickets = tickets ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load tickets.';
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

  badgeClassStatus(status: string): string {
    return status === 'OPEN' ? 'bg-success' : 'bg-danger';
  }

  badgeClassPriority(p: string): string {
    if (p === 'HIGH') return 'bg-danger';
    if (p === 'MEDIUM') return 'bg-warning text-dark';
    return 'bg-primary';
  }
}

