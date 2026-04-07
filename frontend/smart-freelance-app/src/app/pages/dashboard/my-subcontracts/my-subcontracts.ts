import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubcontractService, Subcontract, SubcontractRequest, DeliverableRequest, FreelancerHistory, AuditTimelineEntry } from '../../../core/services/subcontract.service';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-my-subcontracts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-subcontracts.html',
  styleUrls: ['./my-subcontracts.scss']
})
export class MySubcontracts implements OnInit {
  subcontracts: Subcontract[] = [];
  selected: Subcontract | null = null;
  loading = true;
  showForm = false;
  showDeliverableForm = false;

  activeTab: 'list' | 'history' = 'list';
  history: FreelancerHistory | null = null;
  loadingHistory = false;
  historyFilter = '';
  filteredTimeline: AuditTimelineEntry[] = [];

  get currentUserId(): number { return this.auth.getUserId() ?? 0; }

  form: SubcontractRequest = { subcontractorId: 0, projectId: 0, title: '', category: 'DEVELOPMENT' };
  delForm: DeliverableRequest = { title: '' };

  categories = ['DEVELOPMENT', 'DESIGN', 'TESTING', 'CONTENT', 'CONSULTING'];

  freelancers: User[] = [];
  filteredFreelancers: User[] = [];
  freelancerSearch = '';
  selectedFreelancer: User | null = null;
  loadingFreelancers = true;

  myProjects: Project[] = [];

  constructor(
    private svc: SubcontractService,
    private userSvc: UserService,
    private projectSvc: ProjectService,
    private auth: AuthService
  ) {}

  ngOnInit() {
    this.load();
    this.loadingFreelancers = true;
    this.userSvc.getAll().subscribe({
      next: users => {
        this.freelancers = users.filter(u => u.role === 'FREELANCER' && u.id !== this.currentUserId);
        this.filteredFreelancers = [...this.freelancers];
        this.loadingFreelancers = false;
      },
      error: () => { this.loadingFreelancers = false; }
    });
  }

  load() {
    this.loading = true;
    this.svc.getByFreelancer(this.currentUserId).subscribe({
      next: data => { this.subcontracts = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  openForm() {
    this.form = { subcontractorId: 0, projectId: 0, title: '', category: 'DEVELOPMENT' };
    this.freelancerSearch = '';
    this.selectedFreelancer = null;
    this.filteredFreelancers = [...this.freelancers];
    this.showForm = true;
    this.projectSvc.getByClientId(this.currentUserId).subscribe({
      next: p => this.myProjects = p,
      error: () => this.myProjects = []
    });
  }
  closeForm() { this.showForm = false; }

  onFreelancerSearch() {
    const q = this.freelancerSearch.toLowerCase().trim();
    if (q.length === 0) {
      this.filteredFreelancers = [...this.freelancers];
    } else {
      this.filteredFreelancers = this.freelancers.filter(f =>
        (f.firstName + ' ' + f.lastName).toLowerCase().includes(q) || f.email.toLowerCase().includes(q)
      );
    }
  }

  selectFreelancer(f: User) {
    this.selectedFreelancer = f;
    this.form.subcontractorId = f.id;
    this.freelancerSearch = f.firstName + ' ' + f.lastName;
  }

  clearFreelancer() {
    this.selectedFreelancer = null;
    this.form.subcontractorId = 0;
    this.freelancerSearch = '';
  }

  createSubcontract() {
    if (!this.form.subcontractorId || !this.form.projectId || !this.form.title) return;
    this.svc.create(this.currentUserId, this.form).subscribe(() => { this.showForm = false; this.load(); });
  }

  select(s: Subcontract) { this.selected = s; }
  closeDetail() { this.selected = null; }

  propose(id: number) { this.svc.propose(id).subscribe(() => this.reload()); }
  startWork(id: number) { this.svc.startWork(id).subscribe(() => this.reload()); }
  complete(id: number) { this.svc.complete(id).subscribe(() => this.reload()); }
  close(id: number) { this.svc.close(id).subscribe(() => this.reload()); }
  cancel(id: number) {
    const reason = prompt('Raison de l\'annulation :');
    if (reason !== null) this.svc.cancel(id, reason).subscribe(() => this.reload());
  }
  deleteSubcontract(id: number) {
    if (confirm('Supprimer cette sous-traitance ?')) this.svc.delete(id).subscribe(() => { this.selected = null; this.load(); });
  }

  openDeliverableForm() { this.delForm = { title: '' }; this.showDeliverableForm = true; }
  closeDeliverableForm() { this.showDeliverableForm = false; }

  addDeliverable() {
    if (!this.selected) return;
    this.svc.addDeliverable(this.selected.id, this.delForm).subscribe(() => {
      this.showDeliverableForm = false;
      this.svc.getById(this.selected!.id).subscribe(s => this.selected = s);
      this.load();
    });
  }

  reviewDeliverable(deliverableId: number, approved: boolean) {
    if (!this.selected) return;
    const note = approved ? '' : (prompt('Raison du rejet :') || '');
    this.svc.reviewDeliverable(this.selected.id, deliverableId, { approved, reviewNote: note }).subscribe(() => {
      this.svc.getById(this.selected!.id).subscribe(s => this.selected = s);
      this.load();
    });
  }

  deleteDeliverable(deliverableId: number) {
    if (!this.selected || !confirm('Supprimer ce livrable ?')) return;
    this.svc.deleteDeliverable(this.selected.id, deliverableId).subscribe(() => {
      this.svc.getById(this.selected!.id).subscribe(s => this.selected = s);
      this.load();
    });
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

  progressPercent(s: Subcontract): number {
    return s.totalDeliverables > 0 ? Math.round(s.approvedDeliverables / s.totalDeliverables * 100) : 0;
  }

  avatarColor(id: number): string {
    const colors = ['#007bff', '#28a745', '#dc3545', '#ffc107', '#17a2b8', '#6f42c1', '#e83e8c', '#fd7e14', '#20c997', '#6610f2'];
    return colors[id % colors.length];
  }

  // ── Métier 5 — Historique ──────────────────────────

  switchTab(tab: 'list' | 'history') {
    this.activeTab = tab;
    if (tab === 'history' && !this.history) {
      this.loadHistory();
    }
  }

  loadHistory() {
    this.loadingHistory = true;
    this.svc.getFreelancerHistory(this.currentUserId).subscribe({
      next: h => {
        this.history = h;
        this.filteredTimeline = h.timeline;
        this.loadingHistory = false;
      },
      error: () => this.loadingHistory = false
    });
  }

  filterTimeline() {
    if (!this.history) return;
    const q = this.historyFilter.toLowerCase().trim();
    if (!q) {
      this.filteredTimeline = this.history.timeline;
      return;
    }
    this.filteredTimeline = this.history.timeline.filter(e =>
      e.actionLabel.toLowerCase().includes(q) ||
      e.subcontractTitle.toLowerCase().includes(q) ||
      e.actorName.toLowerCase().includes(q) ||
      (e.detail && e.detail.toLowerCase().includes(q))
    );
  }

  topActions(): { label: string; count: number; color: string }[] {
    if (!this.history) return [];
    const colorMap: Record<string, string> = {
      CREATED: '#007bff', PROPOSED: '#17a2b8', ACCEPTED: '#28a745', REJECTED: '#dc3545',
      STARTED: '#ffc107', COMPLETED: '#28a745', CANCELLED: '#dc3545',
      DELIVERABLE_ADDED: '#007bff', DELIVERABLE_SUBMITTED: '#17a2b8',
      DELIVERABLE_APPROVED: '#28a745', DELIVERABLE_REJECTED: '#dc3545'
    };
    return Object.entries(this.history.eventsByAction)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([k, v]) => ({ label: k, count: v, color: colorMap[k] || '#495057' }));
  }

  formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  relativeTime(dateStr: string): string {
    const now = Date.now();
    const then = new Date(dateStr).getTime();
    const diff = now - then;
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return "À l'instant";
    if (mins < 60) return `Il y a ${mins} min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `Il y a ${hours}h`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `Il y a ${days}j`;
    return this.formatDate(dateStr);
  }

  private reload() {
    this.load();
    if (this.selected) {
      this.svc.getById(this.selected.id).subscribe(s => this.selected = s);
    }
  }
}
