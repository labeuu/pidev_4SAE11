import { afterNextRender, Component, HostListener, inject, Injector, OnInit, runInInjectionContext } from '@angular/core';
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
import { AuthService } from '../../../core/services/auth.service';
import { parseVendorApiMessage } from '../../../core/utils/vendor-api-message';

export interface VendorStatsSnapshot {
  total: number;
  pending: number;
  approved: number;
  suspended: number;
  expired: number;
  reviewsDue: number;
  reviewsOverdue: number;
  expiringSoon: number;
}

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

  private readonly injector = inject(Injector);

  vendors: VendorApproval[] = [];
  reviewsDue: VendorApproval[] = [];
  /** MÉTIER 2 — révisions dont la date planifiée est déjà passée. */
  reviewsOverdue: VendorApproval[] = [];
  /** MÉTIER 5 — validUntil dans les 30 prochains jours. */
  expiringSoon: VendorApproval[] = [];
  loading = true;
  activeTab: 'all' | 'pending' | 'approved' | 'reviews' | 'expiring' = 'all';

  /** Filtre texte sur la liste courante (freelancer, organisation, domaine, statut…). */
  searchQuery = '';

  /** Compteurs alignés sur le dernier chargement — évite un getter recalculé à chaque CD. */
  statsSnapshot: VendorStatsSnapshot = {
    total: 0,
    pending: 0,
    approved: 0,
    suspended: 0,
    expired: 0,
    reviewsDue: 0,
    reviewsOverdue: 0,
    expiringSoon: 0,
  };

  showCreateModal = false;
  showActionModal = false;
  actionType: 'approve' | 'reject' | 'suspend' | 'renew' = 'approve';
  selectedVendor: VendorApproval | null = null;
  actionReason = '';
  actionNotes = '';

  newRequest: VendorApprovalRequest = { organizationId: 0, freelancerId: 0, domain: '', professionalSector: '' };

  adminId = 1;

  /** Guards stale responses when `loadAll` is triggered repeatedly (e.g. quick deletes). */
  private loadAllGeneration = 0;

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
    private auth: AuthService,
  ) {}

  @HostListener('document:keydown.escape')
  onEscapeCloseModals(): void {
    if (this.showCreateModal) {
      this.showCreateModal = false;
      return;
    }
    if (this.showActionModal) {
      this.showActionModal = false;
      return;
    }
    if (this.showContextModal) {
      this.closeContext();
      return;
    }
    if (this.showAuditModal) {
      this.closeAudit();
      return;
    }
    if (this.showDecisionModal) {
      this.closeDecision();
    }
  }

  ngOnInit() {
    const uid = this.auth.getUserId();
    if (uid != null) this.adminId = uid;
    this.loadUsers();
    this.loadAll();
  }

  refreshList(): void {
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

  /**
   * @param afterLoad Optional callback after data and `loading` are settled (e.g. success toast).
   *
   * HTTP `subscribe` runs as a microtask; in dev mode Angular can verify bindings twice in one
   * turn so `stats.*` would change between checks (NG0100). Applying the forkJoin result inside
   * `setTimeout(0)` defers to a macrotask so change detection completes first.
   */
  loadAll(afterLoad?: () => void) {
    this.loading = true;
    const generation = ++this.loadAllGeneration;
    forkJoin({
      all: this.vendorService.getAll(),
      due: this.vendorService.getReviewsDue(),
      overdue: this.vendorService.getReviewsOverdue(),
      expiring: this.vendorService.getExpiringSoon(30),
    }).subscribe({
      next: ({ all, due, overdue, expiring }) => {
        setTimeout(() => {
          if (generation !== this.loadAllGeneration) return;
          this.vendors = all;
          this.reviewsDue = due;
          this.reviewsOverdue = overdue;
          this.expiringSoon = expiring;
          this.statsSnapshot = {
            total: all.length,
            pending: all.filter(v => v.status === 'PENDING').length,
            approved: all.filter(v => v.status === 'APPROVED').length,
            suspended: all.filter(v => v.status === 'SUSPENDED').length,
            expired: all.filter(v => v.status === 'EXPIRED').length,
            reviewsDue: due.length,
            reviewsOverdue: overdue.length,
            expiringSoon: expiring.length,
          };
          this.loading = false;
          if (afterLoad) {
            const run = afterLoad;
            runInInjectionContext(this.injector, () => {
              afterNextRender(() => run());
            });
          }
        });
      },
      error: err => {
        setTimeout(() => {
          if (generation !== this.loadAllGeneration) return;
          this.loading = false;
          runInInjectionContext(this.injector, () => {
            afterNextRender(() =>
              this.toast.error(parseVendorApiMessage(err, 'Impossible de charger les agréments.')),
            );
          });
        });
      },
    });
  }

  get filteredVendors(): VendorApproval[] {
    let list: VendorApproval[];
    if (this.activeTab === 'pending') list = this.vendors.filter(v => v.status === 'PENDING');
    else if (this.activeTab === 'approved') list = this.vendors.filter(v => v.status === 'APPROVED');
    else if (this.activeTab === 'reviews') list = this.reviewsDue;
    else if (this.activeTab === 'expiring') list = this.expiringSoon;
    else list = this.vendors;

    const q = this.searchQuery.trim().toLowerCase();
    if (!q) return list;

    return list.filter(v => {
      const hay = [
        this.userName(v.freelancerId),
        this.userName(v.organizationId),
        v.domain,
        v.professionalSector,
        this.statusLabelFr(v.status),
        v.status,
        String(v.id),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return hay.includes(q);
    });
  }

  tabEmptyMessage(): string {
    const q = this.searchQuery.trim();
    if (q) {
      return `Aucun agrément ne correspond à « ${q} » pour cet onglet. Effacez la recherche ou changez d'historique.`;
    }
    switch (this.activeTab) {
      case 'all':
        return "Aucun agrément enregistré. Utilisez « Nouvel agrément » pour en créer un.";
      case 'pending':
        return 'Aucune demande en attente. Les nouvelles demandes apparaîtront ici.';
      case 'approved':
        return "Aucun agrément approuvé. Passez à l'onglet « En attente » pour traiter des dossiers.";
      case 'reviews':
        return 'Aucune révision à planifier dans la fenêtre des 30 prochains jours.';
      case 'expiring':
        return "Aucun agrément n'approche de sa date de fin de validité (30 j.).";
      default:
        return 'Aucun agrément dans cette catégorie.';
    }
  }

  formatShortDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return String(iso).slice(0, 10);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  statusLabelFr(status: VendorApprovalStatus): string {
    const map: Record<VendorApprovalStatus, string> = {
      PENDING: 'En attente',
      APPROVED: 'Approuvé',
      REJECTED: 'Rejeté',
      SUSPENDED: 'Suspendu',
      EXPIRED: 'Expiré',
    };
    return map[status] ?? status;
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
    const { organizationId, freelancerId } = this.newRequest;
    if (!organizationId || !freelancerId) {
      this.toast.error('Veuillez sélectionner un client et un freelancer.');
      return;
    }
    const payload: VendorApprovalRequest = { ...this.newRequest };
    this.showCreateModal = false;
    this.activeTab = 'pending';
    this.searchQuery = '';

    this.vendorService.create(payload).subscribe({
      next: () => {
        this.loadAll(() =>
          this.toast.success('Agrément créé (en attente). Icône œil = contexte projets / contrats.'),
        );
      },
      error: err => {
        this.newRequest = { ...payload };
        this.showCreateModal = true;
        this.toast.error(parseVendorApiMessage(err, 'Erreur lors de la création'));
      },
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
      this.loadAll(() => this.toast.success('Action enregistrée.'));
    };
    const fail = (err: unknown) => this.toast.error(parseVendorApiMessage(err, 'Action échouée'));

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
        this.loadAll(() => this.toast.success('Demande re-soumise.'));
      },
      error: err => this.toast.error(parseVendorApiMessage(err, 'Re-soumission impossible')),
    });
  }

  deleteVendor(vendor: VendorApproval) {
    if (!confirm('Supprimer cet agrément ?')) return;
    this.vendorService.delete(vendor.id).subscribe({
      next: () => {
        this.loadAll(() => this.toast.success('Agrément supprimé.'));
      },
      error: err => this.toast.error(parseVendorApiMessage(err, 'Suppression impossible')),
    });
  }

  sendExpiryReminders() {
    this.vendorService.sendExpiryReminders().subscribe({
      next: res => {
        const n = res.remindersSent;
        const msg =
          n === 0
            ? 'Aucun rappel à envoyer aujourd’hui (J-30 déjà traité ou aucune échéance).'
            : `${n} rappel(s) d’expiration envoyé(s).`;
        this.loadAll(() => this.toast.success(msg));
      },
      error: err => this.toast.error(parseVendorApiMessage(err, 'Impossible d’envoyer les rappels')),
    });
  }

  expireOutdated() {
    this.vendorService.expireOutdated().subscribe({
      next: res => {
        const n = res.expiredCount;
        const msg =
          n === 0
            ? 'Aucun agrément à expirer pour le moment.'
            : `${n} agrément(s) passé(s) en expiré.`;
        this.loadAll(() => this.toast.success(msg));
      },
      error: err => this.toast.error(parseVendorApiMessage(err, 'Impossible de lancer l’expiration')),
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
      error: err => {
        this.auditLoading = false;
        this.toast.error(parseVendorApiMessage(err, 'Impossible de charger le journal d’audit.'));
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
        this.toast.error(parseVendorApiMessage(err, 'Impossible de charger le rapport décision.'));
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
      error: err =>
        this.toast.error(parseVendorApiMessage(err, 'Téléchargement PDF impossible')),
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
    const max = this.ratingBarMax();
    if (max <= 0) return 0;
    return Math.max(0, Math.min(100, (c / max) * 100));
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
