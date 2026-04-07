import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MeetingService } from '../../../core/services/meeting.service';
import { AuthService } from '../../../core/services/auth.service';
import { Meeting } from '../../../core/models/meeting.models';

@Component({
  selector: 'app-meeting-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './meeting-detail.html',
  styleUrl: './meeting-detail.scss',
})
export class MeetingDetail implements OnInit {
  meeting = signal<Meeting | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionError = signal<string | null>(null);
  busy = signal(false);

  cancelReason = '';
  declineReason = '';
  showCancelDialog = signal(false);
  showDeclineDialog = signal(false);

  userId = computed(() => this.auth.getUserId());
  isClient = computed(() => this.auth.isClient());
  isFreelancer = computed(() => this.auth.isFreelancer());

  isMyMeeting = computed(() => {
    const m = this.meeting();
    const uid = this.userId();
    if (!m || !uid) return false;
    return m.clientId === uid || m.freelancerId === uid;
  });

  canAccept = computed(() => {
    const m = this.meeting();
    const uid = this.userId();
    return m?.status === 'PENDING' && m.freelancerId === uid;
  });

  canDecline = computed(() => {
    const m = this.meeting();
    const uid = this.userId();
    return m?.status === 'PENDING' && m.freelancerId === uid;
  });

  canCancel = computed(() => {
    const m = this.meeting();
    if (!m) return false;
    return (m.status === 'PENDING' || m.status === 'ACCEPTED') &&
      (m.clientId === this.userId() || m.freelancerId === this.userId());
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private meetingService: MeetingService,
    private auth: AuthService,
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('meetingId'));
    this.meetingService.getById(id).subscribe(m => {
      this.meeting.set(m);
      this.loading.set(false);
      if (!m) this.error.set('Meeting not found.');
    });
  }

  accept() {
    const m = this.meeting();
    if (!m) return;
    this.busy.set(true);
    this.actionError.set(null);
    this.meetingService.accept(m.id).subscribe({
      next: updated => { this.meeting.set(updated); this.busy.set(false); },
      error: err => { this.busy.set(false); this.actionError.set(err?.error?.message ?? 'Action failed.'); },
    });
  }

  submitDecline() {
    const m = this.meeting();
    if (!m) return;
    this.busy.set(true);
    this.actionError.set(null);
    this.meetingService.decline(m.id, this.declineReason || undefined).subscribe({
      next: updated => {
        this.meeting.set(updated);
        this.busy.set(false);
        this.showDeclineDialog.set(false);
        this.declineReason = '';
      },
      error: err => { this.busy.set(false); this.actionError.set(err?.error?.message ?? 'Action failed.'); },
    });
  }

  submitCancel() {
    const m = this.meeting();
    if (!m) return;
    this.busy.set(true);
    this.actionError.set(null);
    this.meetingService.cancel(m.id, this.cancelReason || undefined).subscribe({
      next: updated => {
        this.meeting.set(updated);
        this.busy.set(false);
        this.showCancelDialog.set(false);
        this.cancelReason = '';
      },
      error: err => { this.busy.set(false); this.actionError.set(err?.error?.message ?? 'Action failed.'); },
    });
  }

  joinMeeting() {
    const link = this.meeting()?.meetLink;
    if (link) window.open(link, '_blank', 'noopener,noreferrer');
  }

  statusClass(status: string): string {
    return { PENDING: 'badge-warning', ACCEPTED: 'badge-success', DECLINED: 'badge-danger', CANCELLED: 'badge-secondary', COMPLETED: 'badge-info' }[status] ?? '';
  }

  typeLabel(type: string): string {
    return { VIDEO_CALL: '📹 Video Call', VOICE_CALL: '📞 Voice Call', IN_PERSON: '🤝 In Person' }[type] ?? type;
  }

  formatDate(dt: string): string {
    return new Date(dt).toLocaleDateString('en-GB', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' });
  }

  formatTime(dt: string): string {
    return new Date(dt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }

  duration(start: string, end: string): string {
    const mins = (new Date(end).getTime() - new Date(start).getTime()) / 60000;
    if (mins < 60) return `${mins} min`;
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return m === 0 ? `${h}h` : `${h}h ${m}min`;
  }
}
