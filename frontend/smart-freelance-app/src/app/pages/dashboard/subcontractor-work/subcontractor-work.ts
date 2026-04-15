import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubcontractService, Subcontract, SubcontractMessage, SubcontractorScore } from '../../../core/services/subcontract.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  validateDeliverableSubmission,
  validatePromptReason,
  SUBMIT_URL_MAX,
  SUBMIT_NOTE_MAX
} from '../../../core/validation/subcontract-validation';

@Component({
  selector: 'app-subcontractor-work',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subcontractor-work.html',
  styleUrls: ['./subcontractor-work.scss']
})
export class SubcontractorWork implements OnInit {
  readonly submitUrlMaxLen = SUBMIT_URL_MAX;
  readonly submitNoteMaxLen = SUBMIT_NOTE_MAX;

  subcontracts: Subcontract[] = [];
  selected: Subcontract | null = null;
  score: SubcontractorScore | null = null;
  loading = true;
  profileError: string | null = null;

  get currentUserId(): number { return this.auth.getUserId() ?? 0; }

  submitUrl = '';
  submitNote = '';
  chatMessages: SubcontractMessage[] = [];
  chatInput = '';
  chatSending = false;
  chatLoading = false;
  /** Erreurs soumission livrable */
  submitAttempted = false;
  submitFieldErrors: Record<string, string> = {};

  constructor(private svc: SubcontractService, private auth: AuthService) {}

  ngOnInit() {
    setTimeout(() => this.bootstrapData(), 0);
  }

  private bootstrapData() {
    const uid = this.auth.getUserId();
    if (uid && uid > 0) {
      this.profileError = null;
      this.load();
      return;
    }
    this.auth.fetchUserProfile().subscribe({
      next: () => {
        const resolved = this.auth.getUserId();
        if (resolved && resolved > 0) {
          // Defer to next tick to avoid NG0100 in dev mode.
          setTimeout(() => {
            this.profileError = null;
            this.load();
          }, 0);
        } else {
          this.loading = false;
          this.profileError = "Profil utilisateur introuvable. Veuillez vous reconnecter.";
        }
      },
      error: () => {
        this.loading = false;
        this.profileError = "Impossible de charger votre profil. Veuillez vous reconnecter.";
      }
    });
  }

  load() {
    if (!this.currentUserId || this.currentUserId <= 0) {
      this.loading = false;
      return;
    }
    this.loading = true;
    this.svc.getScore(this.currentUserId).subscribe({ next: s => this.score = s, error: () => {} });
    this.svc.getBySubcontractor(this.currentUserId).subscribe({
      next: data => { this.subcontracts = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  select(s: Subcontract) {
    this.selected = s;
    this.submitUrl = '';
    this.submitNote = '';
    this.submitAttempted = false;
    this.submitFieldErrors = {};
    this.chatInput = '';
    this.loadChatMessages();
  }
  closeDetail() {
    this.selected = null;
    this.chatMessages = [];
    this.chatInput = '';
  }

  accept(id: number) { this.svc.accept(id).subscribe(() => this.reload()); }
  reject(id: number) {
    const reason = prompt('Raison du refus :');
    if (reason === null) return;
    const err = validatePromptReason(reason);
    if (err) {
      alert(err);
      return;
    }
    this.svc.reject(id, reason.trim()).subscribe(() => this.reload());
  }

  onSubmitFieldChange(): void {
    if (!this.submitAttempted) return;
    this.submitFieldErrors = validateDeliverableSubmission(this.submitUrl, this.submitNote).errors;
  }

  submitDeliverable(deliverableId: number) {
    if (!this.selected) return;
    this.submitAttempted = true;
    const v = validateDeliverableSubmission(this.submitUrl, this.submitNote);
    this.submitFieldErrors = v.errors;
    if (!v.valid) return;
    this.svc.submitDeliverable(this.selected.id, deliverableId, {
      submissionUrl: this.submitUrl.trim(),
      submissionNote: (this.submitNote ?? '').trim()
    }).subscribe(() => {
      this.submitUrl = '';
      this.submitNote = '';
      this.submitAttempted = false;
      this.submitFieldErrors = {};
      this.reload();
    });
  }

  loadChatMessages() {
    if (!this.selected || !this.currentUserId) return;
    this.chatLoading = true;
    this.svc.getMessages(this.selected.id, this.currentUserId).subscribe({
      next: msgs => {
        this.chatMessages = msgs;
        this.chatLoading = false;
      },
      error: () => {
        this.chatLoading = false;
      }
    });
  }

  sendChatMessage() {
    if (!this.selected || !this.currentUserId || this.chatSending) return;
    const text = this.chatInput.trim();
    if (!text) return;
    this.chatSending = true;
    this.svc.sendMessage(this.selected.id, this.currentUserId, text).subscribe({
      next: msg => {
        this.chatMessages = [...this.chatMessages, msg];
        this.chatInput = '';
        this.chatSending = false;
      },
      error: () => {
        this.chatSending = false;
      }
    });
  }

  private reload() {
    this.load();
    if (this.selected) {
      this.svc.getById(this.selected.id).subscribe(s => this.selected = s);
    }
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'badge-neutral',
      PENDING: 'badge-neutral',
      PROPOSED: 'badge-cyan',
      SUBMITTED: 'badge-cyan',
      ACCEPTED: 'badge-blue',
      IN_PROGRESS: 'badge-amber',
      COMPLETED: 'badge-green',
      APPROVED: 'badge-green',
      REJECTED: 'badge-red',
      CANCELLED: 'badge-slate',
      CLOSED: 'badge-light'
    };
    return map[status] || 'badge-neutral';
  }

  scoreColor(): string {
    if (!this.score) return '#6c757d';
    if (this.score.score >= 70) return '#28a745';
    if (this.score.score >= 50) return '#ffc107';
    return '#dc3545';
  }

  decisionScore(s: Subcontract): number {
    let score = 62;
    if ((s.budget ?? 0) >= 800) score += 12;
    else if ((s.budget ?? 0) < 300) score -= 10;

    const days = this.daysUntil(s.deadline);
    if (days !== null) {
      if (days <= 3) score -= 16;
      else if (days <= 7) score -= 8;
      else if (days >= 21) score += 6;
    }

    const scopeLen = (s.scope ?? '').trim().length;
    if (scopeLen >= 120) score += 8;
    else if (scopeLen < 30) score -= 8;

    const deliverables = s.deliverables?.length ?? 0;
    if (deliverables >= 3) score += 8;
    else if (deliverables === 0) score -= 10;

    return Math.max(0, Math.min(100, score));
  }

  decisionLabel(s: Subcontract): 'ACCEPTER' | 'A ÉVALUER' | 'RISQUÉ' {
    const v = this.decisionScore(s);
    if (v >= 70) return 'ACCEPTER';
    if (v >= 45) return 'A ÉVALUER';
    return 'RISQUÉ';
  }

  decisionClass(s: Subcontract): string {
    const label = this.decisionLabel(s);
    if (label === 'ACCEPTER') return 'decision-good';
    if (label === 'A ÉVALUER') return 'decision-medium';
    return 'decision-risk';
  }

  decisionHints(s: Subcontract): string[] {
    const hints: string[] = [];
    const days = this.daysUntil(s.deadline);
    if ((s.budget ?? 0) >= 800) hints.push('Budget attractif pour cette mission.');
    if ((s.budget ?? 0) < 300) hints.push('Budget faible : vérifiez le rapport effort/rémunération.');
    if (days !== null && days <= 7) hints.push('Délai court : confirmez votre disponibilité.');
    if (((s.scope ?? '').trim().length) < 30) hints.push('Périmètre peu détaillé : demandez des clarifications.');
    if ((s.deliverables?.length ?? 0) === 0) hints.push('Aucun livrable défini : ajoutez des jalons avant acceptation.');
    if (hints.length === 0) hints.push('Mission globalement équilibrée selon les informations disponibles.');
    return hints.slice(0, 3);
  }

  readinessChecks(s: Subcontract): { ok: boolean; text: string }[] {
    return [
      { ok: (s.budget ?? 0) > 0, text: 'Budget défini' },
      { ok: !!(s.deadline && this.daysUntil(s.deadline)! > 0), text: 'Date limite valide' },
      { ok: (s.deliverables?.length ?? 0) > 0, text: 'Livrables présents' },
      { ok: ((s.scope ?? '').trim().length) >= 30, text: 'Périmètre suffisamment précis' }
    ];
  }

  private daysUntil(deadline?: string | null): number | null {
    if (!deadline) return null;
    const d = new Date(deadline);
    if (Number.isNaN(d.getTime())) return null;
    const today = new Date();
    const dayMs = 24 * 60 * 60 * 1000;
    const a = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
    const b = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
    return Math.round((b - a) / dayMs);
  }
}
