import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ContractService, Contract, ContractStatus, ContractConflict, ConflictComment } from '../../../../core/services/contract.service';
import { UserService } from '../../../../core/services/user.service';
import { ConflictReport } from '../conflict-report/conflict-report';
import { ElevenLabsService } from '../../../../core/services/elevenlabs.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-contract-detail',
  imports: [CommonModule, NgTemplateOutlet, FormsModule, RouterLink, ConflictReport],
  templateUrl: './contract-detail.html',
  styleUrl: './contract-detail.scss',
  standalone: true,
})
export class ContractDetail implements OnInit {
  /** Dictée vocale → texte (ElevenLabs) — masquée si showAiUi = false */
  readonly showAiUi = environment.showAiUi;
  contract: Contract | null = null;
  conflicts: ContractConflict[] = [];
  isLoading = true;
  errorMsg: string | null = null;
  actionMsg: string | null = null;
  actionError: string | null = null;

  // Edit / propose changes
  showEditForm = false;
  editDraft: Partial<Contract> = {};
  isSavingEdit = false;

  // Signature
  showSignPad = false;
  signaturePreview: string | null = null;
  isSigningFile = false;

  // Conflict modal (child)
  showConflictModal = false;

  // PDF export
  isExportingPdf = false;

  // Resolved party names
  clientName: string | null = null;
  freelancerName: string | null = null;

  // Comments (keyed by conflictId)
  commentsMap: Record<number, ConflictComment[]> = {};
  newCommentText: Record<number, string> = {};
  postingComment: Record<number, boolean> = {};
  editingCommentId: number | null = null;
  editCommentText = '';

  // Voice recording (keyed by conflictId)
  isRecording: Record<number, boolean> = {};
  isTranscribing: Record<number, boolean> = {};
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: BlobPart[] = [];
  private activeRecordingConflictId: number | null = null;

  constructor(
    public auth: AuthService,
    private contractSvc: ContractService,
    private userSvc: UserService,
    private elevenlabs: ElevenLabsService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) { this.errorMsg = 'Invalid contract ID.'; this.isLoading = false; return; }
    this.loadContract(id);
  }

  loadContract(id: number) {
    this.isLoading = true;
    this.contractSvc.getById(id).subscribe({
      next: (c) => {
        this.contract = c;
        this.isLoading = false;
        this.loadConflicts(id);
        this.loadPartyNames(c);
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Contract not found or service unavailable.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadPartyNames(c: Contract) {
    if (c.clientId) {
      this.userSvc.getById(c.clientId).subscribe({
        next: (u) => {
          if (u) { this.clientName = `${u.firstName} ${u.lastName}`; this.cdr.detectChanges(); }
        },
        error: () => {}
      });
    }
    if (c.freelancerId) {
      this.userSvc.getById(c.freelancerId).subscribe({
        next: (u) => {
          if (u) { this.freelancerName = `${u.firstName} ${u.lastName}`; this.cdr.detectChanges(); }
        },
        error: () => {}
      });
    }
  }

  loadConflicts(contractId: number) {
    this.contractSvc.getConflicts(contractId).subscribe({
      next: (list) => {
        this.conflicts = Array.isArray(list) ? list : [];
        this.conflicts.forEach(cf => { if (cf.id) this.loadComments(cf.id); });
        this.cdr.detectChanges();
      },
      error: () => { this.conflicts = []; }
    });
  }

  loadComments(conflictId: number) {
    this.contractSvc.getComments(conflictId).subscribe({
      next: (list) => {
        this.commentsMap[conflictId] = Array.isArray(list) ? list : [];
        this.cdr.detectChanges();
      },
      error: () => { this.commentsMap[conflictId] = []; }
    });
  }

  postComment(conflictId: number) {
    const text = (this.newCommentText[conflictId] ?? '').trim();
    if (!text) return;
    const userId = this.auth.getUserId();
    if (!userId) return;
    this.postingComment[conflictId] = true;
    this.contractSvc.addComment(conflictId, userId, text).subscribe({
      next: (c) => {
        this.commentsMap[conflictId] = [...(this.commentsMap[conflictId] ?? []), c];
        this.newCommentText[conflictId] = '';
        this.postingComment[conflictId] = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.postingComment[conflictId] = false;
        this.cdr.detectChanges();
      }
    });
  }

  startEditComment(comment: ConflictComment) {
    this.editingCommentId = comment.id ?? null;
    this.editCommentText = comment.content;
  }

  saveEditComment(comment: ConflictComment) {
    if (!comment.id || !this.editCommentText.trim()) return;
    this.contractSvc.updateComment(comment.id, this.editCommentText.trim()).subscribe({
      next: (updated) => {
        const conflictId = comment.conflictId!;
        this.commentsMap[conflictId] = this.commentsMap[conflictId].map(c => c.id === updated.id ? updated : c);
        this.editingCommentId = null;
        this.editCommentText = '';
        this.cdr.detectChanges();
      },
      error: () => { this.actionError = 'Failed to update comment.'; this.cdr.detectChanges(); }
    });
  }

  cancelEditComment() { this.editingCommentId = null; this.editCommentText = ''; }

  deleteComment(comment: ConflictComment) {
    if (!comment.id) return;
    this.contractSvc.deleteComment(comment.id).subscribe({
      next: () => {
        const conflictId = comment.conflictId!;
        this.commentsMap[conflictId] = this.commentsMap[conflictId].filter(c => c.id !== comment.id);
        this.cdr.detectChanges();
      },
      error: () => { this.actionError = 'Failed to delete comment.'; this.cdr.detectChanges(); }
    });
  }

  commentsFor(conflictId: number): ConflictComment[] {
    return this.commentsMap[conflictId] ?? [];
  }

  // ── Voice recording → Speech-to-Text ─────────────────────────────

  async startRecording(conflictId: number) {
    if (this.mediaRecorder) return; // already recording
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.audioChunks = [];
      this.activeRecordingConflictId = conflictId;
      this.isRecording[conflictId] = true;
      this.mediaRecorder = new MediaRecorder(stream);
      this.mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) this.audioChunks.push(e.data);
      };
      this.mediaRecorder.onstop = () => {
        stream.getTracks().forEach(t => t.stop());
        const blob = new Blob(this.audioChunks, { type: 'audio/webm' });
        this.transcribeAudio(conflictId, blob);
        this.mediaRecorder = null;
        this.audioChunks = [];
        this.activeRecordingConflictId = null;
      };
      this.mediaRecorder.start();
      this.cdr.detectChanges();
    } catch {
      this.actionError = 'Microphone access denied.';
      this.cdr.detectChanges();
    }
  }

  stopRecording(conflictId: number) {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.isRecording[conflictId] = false;
      this.isTranscribing[conflictId] = true;
      this.cdr.detectChanges();
      this.mediaRecorder.stop();
    }
  }

  private transcribeAudio(conflictId: number, blob: Blob) {
    this.elevenlabs.transcribe(blob).subscribe({
      next: (text) => {
        if (text) {
          this.newCommentText[conflictId] = ((this.newCommentText[conflictId] ?? '') + ' ' + text).trimStart();
        }
        this.isTranscribing[conflictId] = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.actionError = 'Speech-to-text failed. Please try again.';
        this.isTranscribing[conflictId] = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Getters ──────────────────────────────────────────────────────

  get myRole(): 'CLIENT' | 'FREELANCER' | null {
    if (this.auth.isClient()) return 'CLIENT';
    if (this.auth.isFreelancer()) return 'FREELANCER';
    return null;
  }

  get canEdit(): boolean {
    return this.contract?.status === 'DRAFT' && this.auth.isClient();
  }

  get canSubmitForSignature(): boolean {
    return this.contract?.status === 'DRAFT' && this.auth.isClient();
  }

  get canProposeChanges(): boolean {
    return this.contract?.status === 'PENDING_SIGNATURE' && this.auth.isFreelancer();
  }

  get canSign(): boolean {
    const s = this.contract?.status;
    if (s !== 'PENDING_SIGNATURE') return false;
    if (this.auth.isClient() && !this.contract?.clientSignatureUrl) return true;
    if (this.auth.isFreelancer() && !this.contract?.freelancerSignatureUrl) return true;
    return false;
  }

  get canReject(): boolean {
    return this.contract?.status === 'PENDING_SIGNATURE' && this.auth.isFreelancer();
  }

  get canComplete(): boolean {
    return this.contract?.status === 'ACTIVE' && this.auth.isClient();
  }

  get canReportConflict(): boolean {
    const s = this.contract?.status;
    return s === 'ACTIVE' || s === 'IN_CONFLICT';
  }

  // ── Conflict grouping ─────────────────────────────────────────────
  get clientConflicts(): ContractConflict[] {
    return this.conflicts.filter(c => c.raisedById === this.contract?.clientId);
  }

  get freelancerConflicts(): ContractConflict[] {
    return this.conflicts.filter(c => c.raisedById === this.contract?.freelancerId);
  }

  get isSigned(): boolean {
    return !!(this.contract?.clientSignatureUrl && this.contract?.freelancerSignatureUrl);
  }

  // ── Actions ──────────────────────────────────────────────────────

  submitForSignature() {
    if (!this.contract?.id) return;
    this.contractSvc.updateStatus(this.contract.id, 'PENDING_SIGNATURE').subscribe({
      next: (c) => { this.contract = c; this.actionMsg = 'Sent for signature!'; this.cdr.detectChanges(); },
      error: () => { this.actionError = 'Failed to submit for signature.'; this.cdr.detectChanges(); }
    });
  }

  rejectContract() {
    if (!this.contract?.id) return;
    this.contractSvc.updateStatus(this.contract.id, 'DRAFT').subscribe({
      next: (c) => { this.contract = c; this.actionMsg = 'Contract sent back for revision.'; this.cdr.detectChanges(); },
      error: () => { this.actionError = 'Action failed.'; this.cdr.detectChanges(); }
    });
  }

  completeContract() {
    if (!this.contract?.id) return;
    this.contractSvc.updateStatus(this.contract.id, 'COMPLETED').subscribe({
      next: (c) => { this.contract = c; this.actionMsg = 'Contract marked as completed!'; this.cdr.detectChanges(); },
      error: () => { this.actionError = 'Failed to complete contract.'; this.cdr.detectChanges(); }
    });
  }

  // ── Edit / Propose Changes ────────────────────────────────────────

  openEdit() {
    if (!this.contract) return;
    this.editDraft = {
      title: this.contract.title,
      description: this.contract.description,
      terms: this.contract.terms,
      amount: this.contract.amount,
      startDate: this.contract.startDate,
      endDate: this.contract.endDate,
    };
    this.showEditForm = true;
  }

  cancelEdit() { this.showEditForm = false; }

  saveEdit() {
    if (!this.contract?.id) return;
    this.isSavingEdit = true;
    const payload: Partial<Contract> = {
      ...this.editDraft,
      status: 'DRAFT', // propose changes resets to DRAFT
    };
    this.contractSvc.update(this.contract.id, payload).subscribe({
      next: (c) => {
        this.contract = c;
        this.showEditForm = false;
        this.isSavingEdit = false;
        this.actionMsg = 'Changes saved. Contract is back in Draft.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.isSavingEdit = false;
        this.actionError = 'Failed to save changes.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Signature ────────────────────────────────────────────────────

  openSignPad() { this.showSignPad = true; this.signaturePreview = null; }
  closeSignPad() { this.showSignPad = false; this.signaturePreview = null; }

  onSignatureFile(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      this.signaturePreview = reader.result as string;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);
  }

  submitSignature() {
    if (!this.contract?.id || !this.signaturePreview || !this.myRole) return;
    this.isSigningFile = true;
    this.contractSvc.sign(this.contract.id, this.myRole, this.signaturePreview).subscribe({
      next: (c) => {
        this.contract = c;
        this.showSignPad = false;
        this.signaturePreview = null;
        this.isSigningFile = false;
        this.actionMsg = c.status === 'ACTIVE'
          ? '✅ Both signatures collected — contract is now Active!'
          : '✅ Signature submitted! Waiting for the other party.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.isSigningFile = false;
        this.actionError = 'Failed to submit signature.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Schedule meeting from conflict ───────────────────────────────

  scheduleMeeting() {
    if (!this.contract) return;
    if (this.auth.isClient()) {
      this.router.navigate(['/dashboard/meetings/schedule'], {
        queryParams: {
          contractId: this.contract.id,
          freelancerId: this.contract.freelancerId,
        },
      });
    } else {
      // Freelancers go to their meetings list; only clients can create meetings
      this.router.navigate(['/dashboard/meetings']);
    }
  }

  // ── Conflict modal ────────────────────────────────────────────────

  openConflictModal() { this.showConflictModal = true; }
  closeConflictModal() { this.showConflictModal = false; }

  onConflictReported() {
    this.showConflictModal = false;
    if (this.contract?.id) {
      this.loadConflicts(this.contract.id);
      // Also flag the contract as IN_CONFLICT
      this.contractSvc.updateStatus(this.contract.id, 'IN_CONFLICT').subscribe({
        next: (c) => { this.contract = c; this.cdr.detectChanges(); }
      });
    }
    this.actionMsg = 'Conflict reported successfully.';
  }

  // ── PDF Export ────────────────────────────────────────────────────

  exportPdf() {
    if (!this.contract?.id) return;
    this.isExportingPdf = true;
    this.contractSvc.exportPdf(this.contract.id, false, this.clientName, this.freelancerName).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contract-${this.contract!.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.isExportingPdf = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.actionError = 'Failed to export PDF. Please try again.';
        this.isExportingPdf = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────

  getStatusLabel(s?: ContractStatus): string {
    const m: Record<ContractStatus, string> = {
      DRAFT: 'Draft', PENDING_SIGNATURE: 'Pending Signature',
      ACTIVE: 'Active', IN_CONFLICT: 'In Conflict',
      COMPLETED: 'Completed', CANCELLED: 'Cancelled',
    };
    return s ? (m[s] ?? s) : '—';
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  dismissMsg() { this.actionMsg = null; this.actionError = null; }
}
