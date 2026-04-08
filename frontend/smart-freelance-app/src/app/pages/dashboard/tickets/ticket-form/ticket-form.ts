import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/services/auth.service';
import { Ticket, TicketService } from '../../../../core/services/ticket.service';
import { ToastService } from '../../../../core/services/toast.service';
import { toastSuccessWithOptionalFilterNote } from '../../../../core/utils/content-sanitized-notice.util';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';

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
  syncingProfile = false;
  errorMessage = '';

  editingTicket: Ticket | null = null;

  constructor(
    private fb: FormBuilder,
    private ticketService: TicketService,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    private toast: ToastService,
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
      if (id) {
        this.loadTicket(id);
        return;
      }
    }
    this.ensureProfileForCreate();
  }

  /** Ticket-service resolves the user via user-service using JWT email; frontend userId must match for list UX. */
  private ensureProfileForCreate(): void {
    if (this.auth.getUserId() != null) return;
    this.syncingProfile = true;
    this.errorMessage = '';
    this.auth
      .fetchUserProfile()
      .pipe(finalize(() => {
        this.syncingProfile = false;
        this.cdr.detectChanges();
      }))
      .subscribe((profile) => {
        if (!profile?.id) {
          this.errorMessage =
            'Your profile could not be loaded. Sign out, sign in again, then create a ticket.';
          this.toast.error(this.errorMessage);
        }
      });
  }

  loadTicket(id: number): void {
    this.loading = true;
    this.ticketService.getById(id).subscribe({
      next: (t) => {
        this.loading = false;
        this.editingTicket = t;
        this.form.patchValue({
          subject: t.subject,
          priority: t.priority,
        });
        if (t.status === 'CLOSED') this.form.disable();
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        this.loading = false;
        this.errorMessage = messageFromHttpError(err, 'Failed to load ticket.');
        this.toast.error(this.errorMessage);
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

    if (!this.auth.getToken()) {
      this.errorMessage = 'You are not signed in.';
      this.toast.error(this.errorMessage);
      return;
    }

    if (!this.editingTicket?.id && this.auth.getUserId() == null) {
      this.syncingProfile = true;
      this.auth
        .fetchUserProfile()
        .pipe(finalize(() => {
          this.syncingProfile = false;
          this.cdr.detectChanges();
        }))
        .subscribe((profile) => {
          if (!profile?.id) {
            this.errorMessage =
              'Your profile could not be loaded. Sign out and sign in again, then retry.';
            this.toast.error(this.errorMessage);
            return;
          }
          this.runSave(subject, priority);
        });
      return;
    }

    this.runSave(subject, priority);
  }

  private runSave(subject: string, priority: 'LOW' | 'MEDIUM' | 'HIGH' | null): void {
    this.saving = true;
    this.errorMessage = '';

    if (this.editingTicket?.id) {
      this.ticketService.update(this.editingTicket.id, { subject, priority }).subscribe({
        next: (t) => {
          this.saving = false;
          toastSuccessWithOptionalFilterNote(this.toast, subject, t.subject, 'Ticket updated.');
          this.router.navigate(['/dashboard/tickets', t.id]);
        },
        error: (err: unknown) => {
          this.saving = false;
          this.errorMessage = messageFromHttpError(err, 'Failed to update ticket.');
          this.toast.error(this.errorMessage);
          this.cdr.detectChanges();
        },
      });
      return;
    }

    this.ticketService.create({ subject }).subscribe({
      next: (t) => {
        this.saving = false;
        toastSuccessWithOptionalFilterNote(this.toast, subject, t.subject, 'Ticket created.');
        this.router.navigate(['/dashboard/tickets', t.id]);
      },
      error: (err: unknown) => {
        this.saving = false;
        this.errorMessage = messageFromHttpError(err, 'Failed to create ticket.');
        this.toast.error(this.errorMessage);
        this.cdr.detectChanges();
      },
    });
  }
}
