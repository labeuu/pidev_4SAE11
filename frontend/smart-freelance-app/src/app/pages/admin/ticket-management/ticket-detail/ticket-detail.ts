import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../../core/services/auth.service';
import { Ticket, TicketService } from '../../../../core/services/ticket.service';
import { ReplyService, TicketReply } from '../../../../core/services/reply.service';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { toastSuccessWithOptionalFilterNote } from '../../../../core/utils/content-sanitized-notice.util';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';

@Component({
  selector: 'app-admin-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class AdminTicketDetail implements OnInit {
  ticket: Ticket | null = null;
  replies: TicketReply[] = [];
  ownerLabel = '';
  loading = true;
  errorMessage = '';

  replyForm: FormGroup;
  sending = false;
  closing = false;
  deleting = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private ticketService: TicketService,
    private replyService: ReplyService,
    private userService: UserService,
    private auth: AuthService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {
    this.replyForm = this.fb.group({
      message: ['', [Validators.required, Validators.maxLength(2000)]],
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/admin/tickets']);
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.ticketService.getById(id).subscribe({
      next: (t) => {
        this.ticket = t;
        this.resolveOwnerLabel(t.userId);
        this.updateReplyFormDisabledState();
        this.loadReplies(t.id);
      },
      error: (err: unknown) => {
        this.ticket = null;
        this.errorMessage = messageFromHttpError(err, 'Failed to load ticket.');
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private resolveOwnerLabel(userId: number): void {
    this.ownerLabel = `User #${userId}`;
    this.userService.getById(userId).subscribe({
      next: (u) => {
        if (u) {
          const name = `${u.firstName} ${u.lastName}`.trim();
          this.ownerLabel = name ? `${name} (${u.email})` : u.email;
        }
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges(),
    });
  }

  loadReplies(ticketId: number): void {
    this.replyService.getByTicketId(ticketId).subscribe({
      next: (r) => {
        this.replies = r;
        this.loading = false;
        this.updateReplyFormDisabledState();
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        this.errorMessage = messageFromHttpError(err, 'Failed to load replies.');
        this.loading = false;
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
        this.load(this.ticket!.id);
      },
      error: (err: unknown) => {
        this.sending = false;
        this.updateReplyFormDisabledState();
        this.errorMessage = messageFromHttpError(err, 'Failed to send reply.');
        this.cdr.detectChanges();
      },
    });
  }

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

  closeTicket(): void {
    if (!this.ticket?.id || this.ticket.status === 'CLOSED') return;
    if (!confirm('Close this ticket? The customer will not be able to add new replies.')) return;
    this.closing = true;
    this.ticketService.close(this.ticket.id).subscribe({
      next: (t) => {
        this.closing = false;
        this.ticket = t;
        this.updateReplyFormDisabledState();
        this.toast.success('Ticket closed.');
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        this.closing = false;
        this.errorMessage = messageFromHttpError(err, 'Failed to close ticket.');
        this.cdr.detectChanges();
      },
    });
  }

  deleteTicket(): void {
    if (!this.ticket?.id) return;
    if (!confirm('Delete this ticket and all replies permanently? This cannot be undone.')) return;
    this.deleting = true;
    this.ticketService.delete(this.ticket.id).subscribe({
      next: () => {
        this.deleting = false;
        this.toast.success('Ticket deleted.');
        this.router.navigate(['/admin/tickets']);
      },
      error: (err: unknown) => {
        this.deleting = false;
        this.errorMessage = messageFromHttpError(err, 'Failed to delete ticket.');
        this.cdr.detectChanges();
      },
    });
  }

  isMe(r: TicketReply): boolean {
    const myId = this.auth.getUserId();
    return myId != null && r.authorUserId === myId;
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
