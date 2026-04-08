import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubcontractService, Subcontract, SubcontractorScore } from '../../../core/services/subcontract.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-subcontractor-work',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subcontractor-work.html',
  styleUrls: ['./subcontractor-work.scss']
})
export class SubcontractorWork implements OnInit {
  subcontracts: Subcontract[] = [];
  selected: Subcontract | null = null;
  score: SubcontractorScore | null = null;
  loading = true;

  get currentUserId(): number { return this.auth.getUserId() ?? 0; }

  submitUrl = '';
  submitNote = '';

  constructor(private svc: SubcontractService, private auth: AuthService) {}

  ngOnInit() {
    this.load();
    this.svc.getScore(this.currentUserId).subscribe({ next: s => this.score = s, error: () => {} });
  }

  load() {
    this.loading = true;
    this.svc.getBySubcontractor(this.currentUserId).subscribe({
      next: data => { this.subcontracts = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  select(s: Subcontract) {
    this.selected = s;
    this.submitUrl = '';
    this.submitNote = '';
  }
  closeDetail() { this.selected = null; }

  accept(id: number) { this.svc.accept(id).subscribe(() => this.reload()); }
  reject(id: number) {
    const reason = prompt('Raison du refus :');
    if (reason !== null) this.svc.reject(id, reason).subscribe(() => this.reload());
  }

  submitDeliverable(deliverableId: number) {
    if (!this.selected) return;
    this.svc.submitDeliverable(this.selected.id, deliverableId, {
      submissionUrl: this.submitUrl, submissionNote: this.submitNote
    }).subscribe(() => { this.submitUrl = ''; this.submitNote = ''; this.reload(); });
  }

  private reload() {
    this.load();
    if (this.selected) {
      this.svc.getById(this.selected.id).subscribe(s => this.selected = s);
    }
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'badge-secondary', PROPOSED: 'badge-info', ACCEPTED: 'badge-primary',
      REJECTED: 'badge-danger', IN_PROGRESS: 'badge-warning', COMPLETED: 'badge-success',
      CANCELLED: 'badge-dark', CLOSED: 'badge-light', PENDING: 'badge-secondary',
      SUBMITTED: 'badge-info', APPROVED: 'badge-success'
    };
    return map[status] || 'badge-secondary';
  }

  scoreColor(): string {
    if (!this.score) return '#6c757d';
    if (this.score.score >= 70) return '#28a745';
    if (this.score.score >= 50) return '#ffc107';
    return '#dc3545';
  }
}
