import { Component, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../../core/services/auth.service';
import { Ticket, TicketService } from '../../../../core/services/ticket.service';
import { ReplyService, TicketReply } from '../../../../core/services/reply.service';
import { ToastService } from '../../../../core/services/toast.service';
import { toastSuccessWithOptionalFilterNote } from '../../../../core/utils/content-sanitized-notice.util';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';
import { finalize } from 'rxjs/operators';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class TicketDetail implements OnInit, OnDestroy {
  ticket: Ticket | null = null;
  replies: TicketReply[] = [];
  loading = true;
  errorMessage = '';

  replyForm: FormGroup;
  sending = false;

  editingReplyId: number | null = null;
  editForm: FormGroup;
  savingEdit = false;
  deletingReplyId: number | null = null;
  lastUpdatedAt: Date | null = null;
  private pollSub?: Subscription;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly ticketService: TicketService,
    private readonly replyService: ReplyService,
    private readonly auth: AuthService,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.replyForm = this.fb.group({
      message: ['', [Validators.required, Validators.maxLength(2000)]],
    });
    this.editForm = this.fb.group({
      message: ['', [Validators.required, Validators.maxLength(2000)]],
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/dashboard/tickets']);
      return;
    }
    this.load(id);
    this.pollSub = interval(8000).subscribe(() => {
      if (!document.hidden && this.ticket?.id) {
        this.load(this.ticket.id, false);
      }
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  load(id: number, showLoading = true): void {
    if (showLoading) {
      this.loading = true;
    }
    this.errorMessage = '';
    this.ticketService.getById(id).subscribe({
      next: (t) => {
        this.ticket = t;
        this.updateReplyFormDisabledState();
        this.loadReplies(t.id);
      },
      error: (err: unknown) => {
        this.ticket = null;
        this.errorMessage = messageFromHttpError(err, 'Failed to load ticket.');
        this.toast.error(this.errorMessage);
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadReplies(ticketId: number): void {
    this.replyService.getByTicketId(ticketId).subscribe({
      next: (r) => {
        this.replies = r;
        this.ticketService
          .markRead(ticketId)
          .pipe(
            finalize(() => {
              this.loading = false;
              this.lastUpdatedAt = new Date();
              this.updateReplyFormDisabledState();
              this.cdr.detectChanges();
            })
          )
          .subscribe({ error: () => {} });
      },
      error: (err: unknown) => {
        this.errorMessage = messageFromHttpError(err, 'Failed to load replies.');
        this.toast.error(this.errorMessage);
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  reopenTicket(): void {
    if (!this.ticket?.id || !this.ticket.canReopen) return;
    this.ticketService.reopen(this.ticket.id).subscribe({
      next: (t) => {
        this.ticket = t;
        this.updateReplyFormDisabledState();
        this.toast.success('Ticket reopened.');
        this.load(this.ticket.id, false);
      },
      error: (err: unknown) => {
        this.errorMessage = messageFromHttpError(err, 'Could not reopen ticket.');
        this.toast.error(this.errorMessage);
        this.cdr.detectChanges();
      },
    });
  }

  sendReply(): void {
    if (!this.ticket?.id || this.replyForm.invalid) {
      this.replyForm.markAllAsTouched();
      return;
    }
    const msg = String(this.replyForm.getRawValue().message || '').trim();
    if (!msg) return;
    this.sending = true;
    this.updateReplyFormDisabledState();
    this.replyService.create({ ticketId: this.ticket.id, message: msg }).subscribe({
      next: (reply) => {
        this.sending = false;
        this.replyForm.reset({ message: '' });
        this.updateReplyFormDisabledState();
        toastSuccessWithOptionalFilterNote(this.toast, msg, reply.message, 'Reply sent.');
        this.load(this.ticket!.id, false);
      },
      error: (err: unknown) => {
        this.sending = false;
        this.updateReplyFormDisabledState();
        this.errorMessage = messageFromHttpError(err, 'Failed to send reply.');
        this.toast.error(this.errorMessage);
        this.cdr.detectChanges();
      },
    });
  }

  /** Avoid [disabled] on reactive form controls (Angular dev mode warning). */
  private updateReplyFormDisabledState(): void {
    const c = this.replyForm.get('message');
    if (!c) return;
    const block = this.sending || this.ticket?.status === 'CLOSED';
    if (block) {
      c.disable({ emitEvent: false });
    } else {
      c.enable({ emitEvent: false });
    }
  }

  canEditReply(r: TicketReply): boolean {
    const myId = this.auth.getUserId();
    return myId != null && r.authorUserId === myId;
  }

  startEdit(r: TicketReply): void {
    if (!this.canEditReply(r)) return;
    this.editingReplyId = r.id;
    this.editForm.patchValue({ message: r.message });
  }

  cancelEdit(): void {
    this.editingReplyId = null;
    this.editForm.reset({ message: '' });
  }

  saveEdit(): void {
    if (!this.editingReplyId || this.editForm.invalid) return;
    const msg = String(this.editForm.value.message || '').trim();
    if (!msg) return;
    this.savingEdit = true;
    this.replyService.update(this.editingReplyId, { message: msg }).subscribe({
      next: (updated) => {
        this.savingEdit = false;
        this.editingReplyId = null;
        toastSuccessWithOptionalFilterNote(this.toast, msg, updated.message, 'Reply updated.');
        if (this.ticket?.id) this.load(this.ticket.id, false);
      },
      error: (err: unknown) => {
        this.savingEdit = false;
        this.errorMessage = messageFromHttpError(err, 'Failed to update reply.');
        this.toast.error(this.errorMessage);
        this.cdr.detectChanges();
      },
    });
  }

  deleteReply(r: TicketReply): void {
    if (!this.canEditReply(r)) return;
    this.deletingReplyId = r.id;
    this.replyService.delete(r.id).subscribe({
      next: () => {
        this.deletingReplyId = null;
        this.toast.success('Reply deleted.');
        if (this.ticket?.id) this.load(this.ticket.id, false);
      },
      error: (err: unknown) => {
        this.deletingReplyId = null;
        this.errorMessage = messageFromHttpError(err, 'Failed to delete reply.');
        this.toast.error(this.errorMessage);
        this.cdr.detectChanges();
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

  isMine(r: TicketReply): boolean {
    const myId = this.auth.getUserId();
    return myId != null && r.authorUserId === myId;
  }

  timeline(t: Ticket): Array<{ label: string; date?: string | null }> {
    return [
      { label: 'Created', date: t.createdAt },
      { label: 'First support response', date: t.firstResponseAt },
      { label: 'Last activity', date: t.lastActivityAt },
      { label: 'Resolved', date: t.resolvedAt },
    ];
  }

  userNextAction(t: Ticket): string {
    if (t.status === 'CLOSED') {
      return t.canReopen ? 'Reopen this ticket if your issue is still unresolved.' : 'This thread is closed.';
    }
    if (!t.firstResponseAt) return 'Support has not replied yet. Add more details if needed.';
    return 'Reply with extra details if support requested more information.';
  }
}
