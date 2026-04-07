import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

import { Ticket, TicketService } from '../../../../core/services/ticket.service';

@Component({
  selector: 'app-admin-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.scss',
})
export class AdminTicketList implements OnInit {
  tickets: Ticket[] = [];
  loading = true;
  errorMessage = '';

  constructor(
    private ticketService: TicketService,
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
    this.router.navigate(['/admin/tickets', t.id]);
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

