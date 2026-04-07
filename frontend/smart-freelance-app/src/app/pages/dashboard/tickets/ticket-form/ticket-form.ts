import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { Ticket, TicketService } from '../../../../core/services/ticket.service';

@Component({
  selector: 'app-ticket-form',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './ticket-form.html',
  styleUrl: './ticket-form.scss',
})
export class TicketForm implements OnInit {
  form: FormGroup;
  loading = false;
  saving = false;
  errorMessage = '';

  editingTicket: Ticket | null = null;

  constructor(
    private fb: FormBuilder,
    private ticketService: TicketService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      subject: ['', [Validators.required, Validators.maxLength(200)]],
      priority: [null as 'LOW' | 'MEDIUM' | 'HIGH' | null],
    });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      if (id) this.loadTicket(id);
    }
  }

  loadTicket(id: number): void {
    this.loading = true;
    this.ticketService.getById(id).subscribe({
      next: (t) => {
        this.loading = false;
        this.editingTicket = t;
        if (!t) {
          this.errorMessage = 'Ticket not found.';
          this.cdr.detectChanges();
          return;
        }
        this.form.patchValue({
          subject: t.subject,
          priority: t.priority,
        });
        if (t.status === 'CLOSED') this.form.disable();
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to load ticket.';
        this.cdr.detectChanges();
      },
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const subject = String(this.form.value.subject || '').trim();
    const priority = this.form.value.priority as 'LOW' | 'MEDIUM' | 'HIGH' | null;
    if (!subject) return;

    this.saving = true;
    this.errorMessage = '';

    if (this.editingTicket?.id) {
      this.ticketService.update(this.editingTicket.id, { subject, priority }).subscribe({
        next: (t) => {
          this.saving = false;
          if (t) this.router.navigate(['/dashboard/tickets', t.id]);
          else this.errorMessage = 'Failed to update ticket.';
          this.cdr.detectChanges();
        },
        error: () => {
          this.saving = false;
          this.errorMessage = 'Failed to update ticket.';
          this.cdr.detectChanges();
        },
      });
      return;
    }

    this.ticketService.create({ subject }).subscribe({
      next: (t) => {
        this.saving = false;
        if (t) this.router.navigate(['/dashboard/tickets', t.id]);
        else this.errorMessage = 'Failed to create ticket.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to create ticket.';
        this.cdr.detectChanges();
      },
    });
  }
}

