import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../../core/services/auth.service';
import { Ticket, TicketService } from '../../../../core/services/ticket.service';
import { ReplyService, TicketReply } from '../../../../core/services/reply.service';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class TicketDetail implements OnInit {
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private ticketService: TicketService,
    private replyService: ReplyService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
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
  }

  load(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.ticketService.getById(id).subscribe({
      next: (t) => {
        this.ticket = t;
        if (!t) {
          this.errorMessage = 'Ticket not found.';
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }
        this.loadReplies(t.id);
      },
      error: () => {
        this.errorMessage = 'Failed to load ticket.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadReplies(ticketId: number): void {
    this.replyService.getByTicketId(ticketId).subscribe({
      next: (r) => {
        this.replies = r ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load replies.';
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
    const msg = String(this.replyForm.value.message || '').trim();
    if (!msg) return;
    this.sending = true;
    this.replyService.create({ ticketId: this.ticket.id, message: msg }).subscribe({
      next: (created) => {
        this.sending = false;
        if (created) {
          this.replyForm.reset({ message: '' });
          this.load(this.ticket!.id);
        } else this.errorMessage = 'Failed to send reply.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.sending = false;
        this.errorMessage = 'Failed to send reply.';
        this.cdr.detectChanges();
      },
    });
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
        if (updated && this.ticket?.id) {
          this.editingReplyId = null;
          this.load(this.ticket.id);
        } else this.errorMessage = 'Failed to update reply.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.savingEdit = false;
        this.errorMessage = 'Failed to update reply.';
        this.cdr.detectChanges();
      },
    });
  }

  deleteReply(r: TicketReply): void {
    if (!this.canEditReply(r)) return;
    this.deletingReplyId = r.id;
    this.replyService.delete(r.id).subscribe({
      next: (ok) => {
        this.deletingReplyId = null;
        if (ok && this.ticket?.id) this.load(this.ticket.id);
        else this.errorMessage = 'Failed to delete reply.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deletingReplyId = null;
        this.errorMessage = 'Failed to delete reply.';
        this.cdr.detectChanges();
      },
    });
  }

  statusBadge(status: string): string {
    return status === 'OPEN' ? 'bg-success' : 'bg-danger';
  }

  priorityBadge(p: string): string {
    if (p === 'HIGH') return 'bg-danger';
    if (p === 'MEDIUM') return 'bg-warning text-dark';
    return 'bg-primary';
  }

  isMine(r: TicketReply): boolean {
    const myId = this.auth.getUserId();
    return myId != null && r.authorUserId === myId;
  }
}

