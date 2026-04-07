import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  VendorService,
  VendorApproval,
  VendorApprovalRequest,
  VendorApprovalStatus,
  VendorAuditEntry,
  VendorDecisionInsight,
} from '../../../core/services/vendor.service';
import { UserService, User } from '../../../core/services/user.service';
import { ToastService } from '../../../core/services/toast.service';
import { ProjectService, Project, ProjectApplication } from '../../../core/services/project.service';
import { ContractService, Contract } from '../../../core/services/contract.service';

/** Candidature enrichie avec le titre du projet (même client que l’agrément). */
export interface VendorContextApplication extends ProjectApplication {
  projectTitle: string;
}

export interface VendorContextTimelineItem {
  date: string;
  label: string;
  detail: string;
  kind: 'project' | 'application' | 'contract';
}

@Component({
  selector: 'app-vendor-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vendor-management.html',
  styleUrl: './vendor-management.scss',
})
export class VendorManagement implements OnInit {

  vendors: VendorApproval[] = [];
  reviewsDue: VendorApproval[] = [];
  /** MÉTIER 2 — révisions dont la date planifiée est déjà passée. */
  reviewsOverdue: VendorApproval[] = [];
  /** MÉTIER 5 — validUntil dans les 30 prochains jours. */
  expiringSoon: VendorApproval[] = [];
  loading = true;
  activeTab: 'all' | 'pending' | 'approved' | 'reviews' | 'expiring' = 'all';

  showCreateModal = false;
  showActionModal = false;
  actionType: 'approve' | 'reject' | 'suspend' | 'renew' = 'approve';
  selectedVendor: VendorApproval | null = null;
  actionReason = '';
  actionNotes = '';

  newRequest: VendorApprovalRequest = { organizationId: 0, freelancerId: 0, domain: '', professionalSector: '' };

  adminId = 1;

  clients: User[] = [];
  freelancers: User[] = [];
  userMap = new Map<number, string>();

  /** Panneau contexte : projets / candidatures / contrats entre le client et le freelancer */
  showContextModal = false;
  contextLoading = false;
  contextVendor: VendorApproval | null = null;
  contextProjects: Project[] = [];
  contextApplications: VendorContextApplication[] = [];
  contextContracts: Contract[] = [];
  contextTimeline: VendorContextTimelineItem[] = [];

  /** MÉTIER 4 — journal d'audit persisté (workflow). */
  showAuditModal = false;
  auditVendor: VendorApproval | null = null;
  auditEntries: VendorAuditEntry[] = [];
  auditLoading = false;

  /** Rapport décision admin : avis client→freelancer + projets communs + PDF. */
  showDecisionModal = false;
  decisionVendor: VendorApproval | null = null;
  decisionInsight: VendorDecisionInsight | null = null;
  decisionLoading = false;

  constructor(
    private vendorService: VendorService,
    private userService: UserService,
    private toast: ToastService,
    private projectService: ProjectService,
    private contractService: ContractService,
  ) {}

  private apiError(err: unknown, fallback: string): string {
    const e = err as { error?: { message?: string }; message?: string };
    return e?.error?.message || e?.message || fallback;
  }

  ngOnInit() {
    this.loadUsers();
    this.loadAll();
  }

  loadUsers() {
    this.userService.getAll().subscribe(users => {
      if (!users) return;
      this.clients = users.filter(u => u.role === 'CLIENT');
      this.freelancers = users.filter(u => u.role === 'FREELANCER');
      users.forEach(u => this.userMap.set(u.id, `${u.firstName} ${u.lastName}`));
    });
  }

  userName(id: number): string {
    return this.userMap.get(id) || `#${id}`;
  }

  loadAll() {
    this.loading = true;
    this.vendorService.getAll().subscribe(data => {
      this.vendors = data;
      this.loading = false;
    });
    this.vendorService.getReviewsDue().subscribe(data => this.reviewsDue = data);
    this.vendorService.getReviewsOverdue().subscribe(data => this.reviewsOverdue = data);
    this.vendorService.getExpiringSoon(30).subscribe(data => this.expiringSoon = data);
  }

  get filteredVendors(): VendorApproval[] {
    if (this.activeTab === 'pending') return this.vendors.filter(v => v.status === 'PENDING');
    if (this.activeTab === 'approved') return this.vendors.filter(v => v.status === 'APPROVED');
    if (this.activeTab === 'reviews') return this.reviewsDue;
    if (this.activeTab === 'expiring') return this.expiringSoon;
    return this.vendors;
  }

  get stats() {
    return {
      total: this.vendors.length,
      pending: this.vendors.filter(v => v.status === 'PENDING').length,
      approved: this.vendors.filter(v => v.status === 'APPROVED').length,
      suspended: this.vendors.filter(v => v.status === 'SUSPENDED').length,
      expired: this.vendors.filter(v => v.status === 'EXPIRED').length,
      reviewsDue: this.reviewsDue.length,
      reviewsOverdue: this.reviewsOverdue.length,
      expiringSoon: this.expiringSoon.length,
    };
  }

  statusClass(status: VendorApprovalStatus): string {
    const map: Record<VendorApprovalStatus, string> = {
      PENDING: 'badge--warning',
      APPROVED: 'badge--success',
      REJECTED: 'badge--error',
      SUSPENDED: 'badge--error',
      EXPIRED: 'badge--neutral',
    };
    return map[status] || '';
  }

  openCreate() {
    this.newRequest = { organizationId: 0, freelancerId: 0, domain: '', professionalSector: '' };
    this.showCreateModal = true;
  }

  submitCreate() {
    this.vendorService.create(this.newRequest).subscribe({
      next: () => {
        this.showCreateModal = false;
        this.toast.success('Agrément créé (en attente). Icône œil = contexte projets / contrats.');
        this.loadAll();
      },
      error: err => this.toast.error(this.apiError(err, 'Erreur lors de la création')),
    });
  }

  openAction(vendor: VendorApproval, type: 'approve' | 'reject' | 'suspend' | 'renew') {
    this.selectedVendor = vendor;
    this.actionType = type;
    this.actionReason = '';
    this.actionNotes = '';
    this.showActionModal = true;
  }

  submitAction() {
    if (!this.selectedVendor) return;
    const id = this.selectedVendor.id;

    const done = () => {
      this.showActionModal = false;
      this.toast.success('Action enregistrée.');
      this.loadAll();
    };
    const fail = (err: unknown) => this.toast.error(this.apiError(err, 'Action échouée'));

    switch (this.actionType) {
      case 'approve':
        this.vendorService.approve(id, this.adminId, this.actionNotes).subscribe({ next: done, error: fail });
        break;
      case 'reject':
        this.vendorService.reject(id, this.actionReason).subscribe({ next: done, error: fail });
        break;
      case 'suspend':
        this.vendorService.suspend(id, this.actionReason).subscribe({ next: done, error: fail });
        break;
      case 'renew':
        this.vendorService.renew(id, this.adminId).subscribe({ next: done, error: fail });
        break;
    }
  }

  resubmit(vendor: VendorApproval) {
    this.vendorService.resubmit(vendor.id).subscribe({
      next: () => {
        this.toast.success('Demande re-soumise.');
        this.loadAll();
      },
      error: err => this.toast.error(this.apiError(err, 'Re-soumission impossible')),
    });
  }

  deleteVendor(vendor: VendorApproval) {
    if (!confirm('Supprimer cet agrément ?')) return;
    this.vendorService.delete(vendor.id).subscribe({
      next: () => {
        this.toast.success('Agrément supprimé.');
        this.loadAll();
      },
      error: err => this.toast.error(this.apiError(err, 'Suppression impossible')),
    });
  }

  sendExpiryReminders() {
    this.vendorService.sendExpiryReminders().subscribe({
      next: res => {
        const n = res.remindersSent;
        this.toast.success(
          n === 0
            ? 'Aucun rappel à envoyer aujourd’hui (J-30 déjà traité ou aucune échéance).'
            : `${n} rappel(s) d’expiration envoyé(s).`,
        );
        this.loadAll();
      },
      error: err => this.toast.error(this.apiError(err, 'Impossible d’envoyer les rappels')),
    });
  }

  expireOutdated() {
    this.vendorService.expireOutdated().subscribe({
      next: res => {
        const n = res.expiredCount;
        this.toast.success(
          n === 0
            ? 'Aucun agrément à expirer pour le moment.'
            : `${n} agrément(s) passé(s) en expiré.`,
        );
        this.loadAll();
      },
      error: err => this.toast.error(this.apiError(err, 'Impossible de lancer l’expiration')),
    });
  }

  openContext(v: VendorApproval) {
    this.contextVendor = v;
    this.showContextModal = true;
    this.contextLoading = true;
    this.contextProjects = [];
    this.contextApplications = [];
    this.contextContracts = [];
    this.contextTimeline = [];

    const orgId = v.organizationId;
    const fid = v.freelancerId;

    forkJoin({
      projects: this.projectService.getByClientId(orgId).pipe(catchError(() => of([] as Project[]))),
      apps: this.projectService.getApplicationsByFreelancer(fid).pipe(catchError(() => of([] as ProjectApplication[]))),
      contracts: this.contractService.getByClient(orgId).pipe(
        map(list => (list || []).filter(c => c.freelancerId === fid)),
        catchError(() => of([] as Contract[])),
      ),
    }).subscribe({
      next: ({ projects, apps, contracts }) => {
        const plist = projects || [];
        const projectIds = new Set(plist.map(p => p.id).filter((id): id is number => id != null));
        const byId = new Map<number, Project>();
        plist.forEach(p => {
          if (p.id != null) byId.set(p.id, p);
        });

        const linkedApps = (apps || []).filter(a => projectIds.has(a.projectId));
        this.contextProjects = plist.filter(p => p.id != null && linkedApps.some(a => a.projectId === p.id));
        this.contextApplications = linkedApps.map(a => ({
          ...a,
          projectTitle: byId.get(a.projectId)?.title ?? `Projet #${a.projectId}`,
        }));
        this.contextContracts = contracts || [];
        this.contextTimeline = this.buildContextTimeline(this.contextProjects, this.contextApplications, this.contextContracts);
        this.contextLoading = false;
      },
      error: () => {
        this.contextLoading = false;
        this.toast.error('Impossible de charger le contexte projet.');
      },
    });
  }

  closeContext() {
    this.showContextModal = false;
    this.contextVendor = null;
  }

  openAudit(v: VendorApproval) {
    this.auditVendor = v;
    this.showAuditModal = true;
    this.auditLoading = true;
    this.auditEntries = [];
    this.vendorService.getAuditHistory(v.id).subscribe({
      next: rows => {
        this.auditEntries = rows;
        this.auditLoading = false;
      },
      error: () => {
        this.auditLoading = false;
        this.toast.error('Impossible de charger le journal d’audit.');
      },
    });
  }

  closeAudit() {
    this.showAuditModal = false;
    this.auditVendor = null;
    this.auditEntries = [];
  }

  openDecision(v: VendorApproval) {
    this.decisionVendor = v;
    this.showDecisionModal = true;
    this.decisionInsight = null;
    this.decisionLoading = true;
    this.vendorService.getDecisionInsight(v.id).subscribe({
      next: data => {
        this.decisionInsight = data;
        this.decisionLoading = false;
      },
      error: err => {
        this.decisionLoading = false;
        this.toast.error(this.apiError(err, 'Impossible de charger le rapport décision.'));
      },
    });
  }

  closeDecision() {
    this.showDecisionModal = false;
    this.decisionVendor = null;
    this.decisionInsight = null;
  }

  downloadDecisionPdf() {
    if (!this.decisionVendor) return;
    this.vendorService.downloadDecisionInsightPdf(this.decisionVendor.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `agrement-${this.decisionVendor!.id}-rapport.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.toast.success('PDF téléchargé.');
      },
      error: err => this.toast.error(this.apiError(err, 'Téléchargement PDF impossible')),
    });
  }

  /** Nombre d'avis pour une note (clé JSON souvent string). */
  countRatingStar(star: number): number {
    const d = this.decisionInsight?.ratingDistribution;
    if (!d) return 0;
    return d[star] ?? d[String(star)] ?? 0;
  }

  ratingBarMax(): number {
    const d = this.decisionInsight?.ratingDistribution;
    if (!d || !Object.keys(d).length) return 1;
    return Math.max(1, ...Object.values(d));
  }

  ratingBarWidthPct(star: number): number {
    const c = this.countRatingStar(star);
    return (c / this.ratingBarMax()) * 100;
  }

  formatAuditDate(iso?: string): string {
    if (!iso) return '—';
    return String(iso).slice(0, 16).replace('T', ' ');
  }

  auditActionLabel(action: string): string {
    const map: Record<string, string> = {
      CREATED: 'Création',
      APPROVED: 'Approbation',
      REJECTED: 'Rejet',
      SUSPENDED: 'Suspension',
      RESUBMIT: 'Re-soumission',
      RENEW: 'Renouvellement',
      AUTO_EXPIRE: 'Expiration auto',
      DELETED: 'Suppression',
      SIGN_CLIENT: 'Signature client',
      SIGN_FREELANCER: 'Signature freelancer',
      EXPIRY_REMINDER: 'Rappel fin de validité',
    };
    return map[action] ?? action;
  }

  get hasContextData(): boolean {
    return this.contextTimeline.length > 0;
  }

  private buildContextTimeline(
    projects: Project[],
    applications: VendorContextApplication[],
    contracts: Contract[],
  ): VendorContextTimelineItem[] {
    const raw: Array<{ t: number } & VendorContextTimelineItem> = [];
    const parse = (s?: string) => (s ? new Date(s).getTime() : 0);

    projects.forEach(p => {
      if (p.id == null) return;
      const t = parse(p.createdAt);
      raw.push({
        t: t || 0,
        date: p.createdAt ? String(p.createdAt).slice(0, 16).replace('T', ' ') : '—',
        label: `Projet : ${p.title}`,
        detail: [p.status, p.category].filter(Boolean).join(' · ') || '—',
        kind: 'project',
      });
    });

    applications.forEach(a => {
      const t = parse(a.appliedAt);
      raw.push({
        t: t || 0,
        date: a.appliedAt ? String(a.appliedAt).slice(0, 16).replace('T', ' ') : '—',
        label: `Candidature · ${a.projectTitle}`,
        detail: `Statut : ${a.status ?? '—'}`,
        kind: 'application',
      });
    });

    contracts.forEach(c => {
      const t = parse(c.createdAt);
      raw.push({
        t: t || 0,
        date: c.createdAt ? String(c.createdAt).slice(0, 16).replace('T', ' ') : '—',
        label: `Contrat : ${c.title}`,
        detail: [`Statut : ${c.status ?? '—'}`, c.amount != null ? `${c.amount}` : '', c.startDate ? `début ${c.startDate}` : '']
          .filter(Boolean)
          .join(' · '),
        kind: 'contract',
      });
    });

    raw.sort((a, b) => a.t - b.t);
    return raw.map(({ t: _t, ...row }) => row);
  }
}
