import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MeetingService } from '../../../core/services/meeting.service';
import { AuthService } from '../../../core/services/auth.service';
import { Meeting, MeetingComment, MeetingTranscript, MeetingSummary } from '../../../core/models/meeting.models';

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

  // Comments
  comments = signal<MeetingComment[]>([]);
  newComment = '';
  submittingComment = signal(false);
  editingCommentId = signal<number | null>(null);
  editingContent = '';

  // Transcript
  transcripts = signal<MeetingTranscript[]>([]);
  liveTranscript = '';
  savingTranscript = signal(false);
  transcriptSaved = signal(false);

  // AI Summary
  summary = signal<MeetingSummary | null>(null);
  generatingSummary = signal(false);
  summaryError = signal<string | null>(null);

  // Speech-to-text (shared for both comments and transcript)
  private recognition: any = null;
  isListening = signal(false);
  listeningFor = signal<'comment' | 'transcript' | null>(null);
  speechSupported = 'webkitSpeechRecognition' in window || 'SpeechRecognition' in window;

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
      if (!m) { this.error.set('Meeting not found.'); return; }
      this.loadComments(m.id);
      this.loadTranscripts(m.id);
      this.loadSummary(m.id);
    });
    this.initSpeechRecognition();
  }

  // ── Meeting actions ────────────────────────────────────────────────────────

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

  // ── Comments ───────────────────────────────────────────────────────────────

  loadComments(meetingId: number) {
    this.meetingService.getComments(meetingId).subscribe(c => this.comments.set(c));
  }

  submitComment() {
    const m = this.meeting();
    const uid = this.userId();
    const content = this.newComment.trim();
    if (!m || !uid || !content) return;

    this.submittingComment.set(true);
    const userName = this.auth.getDisplayName();
    this.meetingService.addComment(m.id, uid, userName, content).subscribe({
      next: comment => {
        this.comments.update(list => [...list, comment]);
        this.newComment = '';
        this.submittingComment.set(false);
        if (this.isListening()) this.stopListening();
      },
      error: () => this.submittingComment.set(false),
    });
  }

  startEdit(comment: MeetingComment) {
    this.editingCommentId.set(comment.id);
    this.editingContent = comment.content;
  }

  cancelEdit() {
    this.editingCommentId.set(null);
    this.editingContent = '';
  }

  saveEdit(commentId: number) {
    const content = this.editingContent.trim();
    if (!content) return;
    this.meetingService.updateComment(commentId, content).subscribe({
      next: updated => {
        this.comments.update(list => list.map(c => c.id === commentId ? updated : c));
        this.cancelEdit();
      },
    });
  }

  deleteComment(commentId: number) {
    this.meetingService.deleteComment(commentId).subscribe({
      next: () => this.comments.update(list => list.filter(c => c.id !== commentId)),
    });
  }

  isOwnComment(comment: MeetingComment): boolean {
    return comment.userId === this.userId();
  }

  // ── Transcript ─────────────────────────────────────────────────────────────

  loadTranscripts(meetingId: number) {
    this.meetingService.getTranscripts(meetingId).subscribe(t => this.transcripts.set(t));
  }

  loadSummary(meetingId: number) {
    this.meetingService.getSummary(meetingId).subscribe(s => this.summary.set(s));
  }

  saveTranscript() {
    const m = this.meeting();
    if (!m || !this.liveTranscript.trim()) return;
    this.savingTranscript.set(true);
    this.meetingService.saveTranscript(m.id, this.liveTranscript.trim()).subscribe({
      next: () => {
        this.savingTranscript.set(false);
        this.transcriptSaved.set(true);
        this.loadTranscripts(m.id);
        setTimeout(() => this.transcriptSaved.set(false), 3000);
      },
      error: () => this.savingTranscript.set(false),
    });
  }

  generateSummary() {
    const m = this.meeting();
    if (!m) return;
    this.generatingSummary.set(true);
    this.summaryError.set(null);
    this.meetingService.generateSummary(m.id).subscribe({
      next: s => { this.summary.set(s); this.generatingSummary.set(false); },
      error: () => {
        this.generatingSummary.set(false);
        this.summaryError.set('Failed to generate summary. Make sure Ollama is running.');
      },
    });
  }

  startTranscriptListening() {
    if (!this.recognition) return;
    this.liveTranscript = '';
    this.listeningFor.set('transcript');
    this.recognition.start();
    this.isListening.set(true);
  }

  // ── Speech-to-text (Web Speech API) ───────────────────────────────────────

  private initSpeechRecognition() {
    if (!this.speechSupported) return;
    const SpeechRecognition = (window as any).SpeechRecognition ?? (window as any).webkitSpeechRecognition;
    this.recognition = new SpeechRecognition();
    this.recognition.lang = 'en-US';
    this.recognition.continuous = true;
    this.recognition.interimResults = true;

    this.recognition.onresult = (event: any) => {
      let transcript = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
      }
      if (this.listeningFor() === 'transcript') {
        this.liveTranscript += ' ' + transcript;
      } else {
        this.newComment = transcript;
      }
    };

    this.recognition.onend = () => this.isListening.set(false);
    this.recognition.onerror = () => this.isListening.set(false);
  }

  toggleListening() {
    if (this.isListening()) {
      this.stopListening();
    } else {
      this.newComment = '';
      this.listeningFor.set('comment');
      this.recognition?.start();
      this.isListening.set(true);
    }
  }

  stopListening() {
    this.recognition?.stop();
    this.isListening.set(false);
    this.listeningFor.set(null);
  }

  // ── Formatting helpers ─────────────────────────────────────────────────────

  statusClass(status: string): string {
    return ({ PENDING: 'badge-warning', ACCEPTED: 'badge-success', DECLINED: 'badge-danger', CANCELLED: 'badge-secondary', COMPLETED: 'badge-info' } as any)[status] ?? '';
  }

  typeLabel(type: string): string {
    return ({ VIDEO_CALL: '📹 Video Call', VOICE_CALL: '📞 Voice Call', IN_PERSON: '🤝 In Person' } as any)[type] ?? type;
  }

  formatDate(dt: string): string {
    return new Date(dt).toLocaleDateString('en-GB', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' });
  }

  formatTime(dt: string): string {
    return new Date(dt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }

  formatCommentTime(dt: string): string {
    return new Date(dt).toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  duration(start: string, end: string): string {
    const mins = (new Date(end).getTime() - new Date(start).getTime()) / 60000;
    if (mins < 60) return `${mins} min`;
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return m === 0 ? `${h}h` : `${h}h ${m}min`;
  }
}
