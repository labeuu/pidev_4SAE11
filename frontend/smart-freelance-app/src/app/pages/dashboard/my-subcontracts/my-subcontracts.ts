import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import {
  SubcontractService,
  Subcontract,
  SubcontractRequest,
  DeliverableRequest,
  FreelancerHistory,
  AuditTimelineEntry,
  SubcontractMatchCandidate,
  SubcontractMessage,
  CounterOfferRequest,
  NegotiationRoundResponse,
  SubcontractFinancialAnalysisResponse,
  SubcontractRiskCockpitResponse,
  SubcontractRiskCockpitRequest,
  RiskGauge,
  RiskRecommendation,
  RiskAlternative,
  PredictiveDashboardResponse,
  RiskTrendPoint,
  SubcontractorInsight,
} from '../../../core/services/subcontract.service';
import { User } from '../../../core/services/user.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { OfferService, OfferApplication } from '../../../core/services/offer.service';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { CoachInsightPanel } from './coach-insight-panel/coach-insight-panel';
import {
  validateSubcontractCreateForm,
  validateDeliverableModal,
  validatePromptReason,
  SC_TITLE_MAX,
  SC_SCOPE_MAX,
  SC_SCOPE_MIN_WHEN_SET,
  SC_SKILL_MAX_LEN,
  SC_MAX_SKILLS,
  HISTORY_FILTER_MAX,
  DELIV_TITLE_MAX,
  DELIV_DESC_MAX
} from '../../../core/validation/subcontract-validation';

Chart.register(...registerables);

@Component({
  selector: 'app-my-subcontracts',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective, CoachInsightPanel],
  templateUrl: './my-subcontracts.html',
  styleUrls: ['./my-subcontracts.scss']
})
export class MySubcontracts implements OnInit, OnDestroy {
  readonly Math = Math;
  readonly scTitleMaxLen = SC_TITLE_MAX;
  readonly scScopeMaxLen = SC_SCOPE_MAX;
  readonly scScopeMinWhenSet = SC_SCOPE_MIN_WHEN_SET;
  readonly historyFilterMaxLen = HISTORY_FILTER_MAX;
  readonly delTitleMaxLen = DELIV_TITLE_MAX;
  readonly delDescMaxLen = DELIV_DESC_MAX;

  subcontracts: Subcontract[] = [];
  selected: Subcontract | null = null;
  chatMessages: SubcontractMessage[] = [];
  chatInput = '';
  chatSending = false;
  chatLoading = false;
  loading = true;
  showForm = false;
  showDeliverableForm = false;

  activeTab: 'list' | 'history' | 'predictive' | 'coach' = 'list';
  history: FreelancerHistory | null = null;
  loadingHistory = false;
  historyFilter = '';
  filteredTimeline: AuditTimelineEntry[] = [];
  predictive: PredictiveDashboardResponse | null = null;
  loadingPredictive = false;
  successByCategoryChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  successByCategoryChartOptions: ChartOptions<'bar'> = {};
  riskTrendChartData: ChartData<'line'> = { labels: [], datasets: [] };
  riskTrendChartOptions: ChartOptions<'line'> = {};
  hiddenPredictiveAlerts = new Set<string>();
  riskInsightOpen = false;
  riskInsightSelectedName = '';
  riskInsightScore = 0;
  riskInsightLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' = 'LOW';
  riskInsightSummary = '';
  riskInsightCauses: string[] = [];
  riskInsightActions: string[] = [];
  negotiationForm: { proposedBudget: number | null; proposedDurationDays: number | null; note: string } = {
    proposedBudget: null,
    proposedDurationDays: null,
    note: ''
  };
  negotiationBusy = false;
  negotiationError: string | null = null;
  negotiationRound: NegotiationRoundResponse | null = null;
  actionModalOpen = false;
  actionModalType: 'cancel-subcontract' | 'reject-deliverable' | 'delete-subcontract' | 'delete-deliverable' | null = null;
  actionModalTitle = '';
  actionModalMessage = '';
  actionModalConfirmLabel = 'Confirmer';
  actionModalReason = '';
  actionModalReasonLabel = '';
  actionModalReasonPlaceholder = '';
  actionModalError: string | null = null;
  pendingSubcontractId: number | null = null;
  pendingDeliverableId: number | null = null;

  get currentUserId(): number { return this.auth.getUserId() ?? 0; }

  private buildCreateValidationInput() {
    return {
      title: this.form.title,
      projectId: this.form.projectId,
      offerId: this.form.offerId,
      subcontractorId: this.form.subcontractorId,
      category: this.form.category,
      budget: this.form.budget,
      scope: this.form.scope,
      requiredSkills: this.requiredSkills,
      startDate: this.form.startDate,
      deadline: this.form.deadline
    };
  }

  private syncScCreateErrors(): void {
    if (!this.showForm) return;
    if (!this.scCreateAttempted && !this.scFormInteracted) return;
    this.scFieldErrors = validateSubcontractCreateForm(this.buildCreateValidationInput(), {
      requireBudget: true
    }).errors;
  }

  private markScFormInteracted(): void {
    if (this.showForm) this.scFormInteracted = true;
  }

  /** Affiche erreur sous le champ après 1ʳᵉ interaction ou après tentative de création. */
  scShowScError(field: string): boolean {
    if (!this.showForm) return false;
    if (!this.scCreateAttempted && !this.scFormInteracted) return false;
    return !!this.scFieldErrors[field];
  }

  form: SubcontractRequest = { subcontractorId: 0, title: '', category: 'DEVELOPMENT' };

  /** Valeur du select mission : préfixe p: (projet) ou o: (offre) + id numérique. */
  selectedMission = '';
  delForm: DeliverableRequest = { title: '' };

  categories = ['DEVELOPMENT', 'DESIGN', 'TESTING', 'CONTENT', 'CONSULTING'];

  selectedFreelancer: User | null = null;
  profileError: string | null = null;

  /** Matching IA */
  skillInput = '';
  requiredSkills: string[] = [];
  suggestedSkills = ['Spring Boot', 'React', 'PostgreSQL', 'Figma', 'Angular', 'Node.js', 'Docker', 'TypeScript', 'Java', 'MySQL'];
  loadingAi = false;
  aiError: string | null = null;
  aiMatches: SubcontractMatchCandidate[] | null = null;

  /** Tous les projets (union, pour garde-fous). */
  myProjects: Project[] = [];
  /** Projets où votre candidature a été acceptée par le client (missions que vous exécutez). */
  myProjectsAsFreelancer: Project[] = [];
  /** Projets que vous avez publiés en tant que client. */
  myProjectsAsClient: Project[] = [];
  /** Offres que vous avez publiées — candidature client acceptée par vous. */
  acceptedOfferApps: OfferApplication[] = [];
  /** Message si aucun projet ou erreur lors du chargement (liste déroulante). */
  projectsLoadError: string | null = null;
  /** Au moins un appel API missions a échoué (réseau, serveur) — les listes peuvent être vides à tort. */
  missionsLoadHadError = false;
  loadingMissions = false;

  /** Contrôle de saisie — formulaire création sous-traitance */
  scCreateAttempted = false;
  /** Après la 1ʳᵉ saisie dans le modal : affichage des erreurs en direct. */
  scFormInteracted = false;
  scFieldErrors: Record<string, string> = {};
  /** Erreur API ou session lors de la création (le bouton ne semblait « rien faire » sans ça). */
  createSubmitError: string | null = null;
  creatingSubcontract = false;
  /** Modal livrable */
  delAttempted = false;
  delFieldErrors: Record<string, string> = {};

  mediaUploading = false;
  mediaError: string | null = null;
  presentationFileName: string | null = null;

  /** Cockpit risque (création avant soumission) */
  riskCockpit: SubcontractRiskCockpitResponse | null = null;
  riskLoading = false;
  riskError: string | null = null;
  riskNarrative = '';
  /** @deprecated — remplacé par radar + jauge principale */
  riskGaugeChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{ data: [], label: 'Score risque', backgroundColor: '#f59e0b', borderRadius: 8 }]
  };
  riskGaugeChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, max: 100 } }
  };

  /** Jauge circulaire principale (score global) */
  mainRiskGaugeData: ChartData<'doughnut'> = {
    labels: ['Score', 'Restant'],
    datasets: [{ data: [0, 100], backgroundColor: ['#22c55e', '#e2e8f0'], borderWidth: 0 }]
  };
  mainRiskGaugeOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: true,
    cutout: '78%',
    rotation: -90,
    circumference: 360,
    animation: { duration: 400, easing: 'easeOutQuart' },
    plugins: {
      legend: { display: false },
      tooltip: { enabled: false }
    }
  };

  /** Radar 5 axes : actuel vs cible IA */
  riskRadarChartData: ChartData<'radar'> = {
    labels: [],
    datasets: [
      {
        label: 'Actuel',
        data: [],
        borderColor: '#ef4444',
        backgroundColor: 'rgba(239, 68, 68, 0.12)',
        borderWidth: 2,
        pointBackgroundColor: '#ef4444'
      },
      {
        label: 'Configuration cible IA',
        data: [],
        borderColor: '#22c55e',
        backgroundColor: 'rgba(34, 197, 94, 0.06)',
        borderWidth: 2,
        borderDash: [6, 4],
        pointBackgroundColor: '#22c55e'
      }
    ]
  };
  riskRadarChartOptions: ChartOptions<'radar'> = {
    responsive: true,
    maintainAspectRatio: true,
    animation: { duration: 450, easing: 'easeOutQuart' },
    plugins: {
      legend: {
        position: 'bottom',
        labels: { usePointStyle: true, padding: 16, font: { size: 11 } }
      }
    },
    scales: {
      r: {
        min: 0,
        max: 100,
        ticks: { stepSize: 20, font: { size: 10 } },
        grid: { color: 'rgba(148, 163, 184, 0.25)' },
        pointLabels: { font: { size: 10 } }
      }
    }
  };

  simulationBudget: number | null = null;
  simulationDurationDays = 14;
  private riskDebounceHandle: ReturnType<typeof setTimeout> | null = null;
  /** Snapshot avant dernier recalcul (tendances ↑↓) */
  private prevRiskSnapshot: { total: number; byKey: Record<string, number> } | null = null;
  /** État avant le dernier recalcul (pour flèches ↑↓ et impacts sliders) */
  riskCompareSnapshot: { total: number; byKey: Record<string, number> } | null = null;
  riskRefreshAt = 0;
  private riskAgoInterval: ReturnType<typeof setInterval> | null = null;
  riskUiTick = 0;
  expandedGaugeKey: string | null = null;
  appliedRiskRecommendationIndexes = new Set<number>();

  /** BI financier (Claude) */
  financialBi: SubcontractFinancialAnalysisResponse | null = null;
  loadingFinancial = false;
  financialError: string | null = null;

  budgetChartData: ChartData<'doughnut'> = {
    labels: [],
    datasets: [{ data: [], backgroundColor: ['#2563eb', '#94a3b8', '#22c55e'], borderWidth: 0 }]
  };
  budgetChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: { legend: { position: 'bottom' } }
  };

  gaugeChartData: ChartData<'doughnut'> = {
    labels: ['Rentabilité', ''],
    datasets: [{ data: [0, 100], backgroundColor: ['#0d9488', '#e5e7eb'], borderWidth: 0 }]
  };
  gaugeChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    circumference: 180,
    rotation: 270,
    cutout: '72%',
    plugins: { legend: { display: false } }
  };

  timelineChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        label: 'Budget mission (passées)',
        data: [],
        backgroundColor: 'rgba(37, 99, 235, 0.75)',
        borderRadius: 6
      }
    ]
  };
  timelineChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: { x: { ticks: { maxRotation: 45, minRotation: 0 } }, y: { beginAtZero: true } }
  };

  constructor(
    private svc: SubcontractService,
    private projectSvc: ProjectService,
    private offerSvc: OfferService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnDestroy(): void {
    this.stopRiskAgoClock();
    if (this.riskDebounceHandle) clearTimeout(this.riskDebounceHandle);
  }

  ngOnInit() {
    // Defer async state updates to avoid NG0100 in initial CD pass.
    setTimeout(() => this.bootstrapData(), 0);
  }

  private bootstrapData() {
    const uid = this.auth.getUserId();
    if (uid && uid > 0) {
      this.load();
      return;
    }

    // Try to resolve user id from profile endpoint; if missing, stop gracefully.
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
    this.svc.getByFreelancer(this.currentUserId).subscribe({
      next: data => { this.subcontracts = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  openForm() {
    if (!this.currentUserId || this.currentUserId <= 0) return;
    this.form = { subcontractorId: 0, title: '', category: 'DEVELOPMENT', budget: 500 };
    this.simulationBudget = 500;
    this.selectedMission = '';
    this.mediaError = null;
    this.presentationFileName = null;
    this.mediaUploading = false;
    this.selectedFreelancer = null;
    this.skillInput = '';
    this.requiredSkills = [];
    this.aiMatches = null;
    this.aiError = null;
    this.riskCockpit = null;
    this.riskError = null;
    this.riskNarrative = '';
    this.simulationBudget = null;
    this.simulationDurationDays = 14;
    this.scCreateAttempted = false;
    this.scFormInteracted = false;
    this.scFieldErrors = {};
    this.createSubmitError = null;
    this.showForm = true;
    this.prevRiskSnapshot = null;
    this.expandedGaugeKey = null;
    this.appliedRiskRecommendationIndexes.clear();
    this.riskRefreshAt = 0;
    this.startRiskAgoClock();
    this.scheduleRiskRefresh(false);
    this.loadEligibleMissionsForForm();
  }

  /** Recharge la liste des missions (bouton Réessayer ou réouverture du formulaire). */
  loadEligibleMissionsForForm(): void {
    if (!this.currentUserId || this.currentUserId <= 0) return;
    this.missionsLoadHadError = false;
    this.projectsLoadError = null;
    this.loadingMissions = true;

    const markFail = (label: string, err: unknown) => {
      console.warn(`[Mes sous-traitances] Échec chargement missions (${label})`, err);
      this.missionsLoadHadError = true;
    };

    forkJoin({
      asClientWithAccepted: this.projectSvc
        .getProjectsAsClientWithAcceptedFreelancer(this.currentUserId)
        .pipe(catchError((err) => {
          markFail('projets client', err);
          return of([] as Project[]);
        })),
      asFreelancerAccepted: this.projectSvc
        .getProjectsForFreelancer(this.currentUserId)
        .pipe(catchError((err) => {
          markFail('projets freelancer', err);
          return of([] as Project[]);
        })),
      acceptedOffers: this.offerSvc
        .getAcceptedApplicationsForFreelancerOffers(this.currentUserId)
        .pipe(catchError((err) => {
          markFail('offres', err);
          return of([] as OfferApplication[]);
        }))
    }).subscribe({
      next: ({ asClientWithAccepted, asFreelancerAccepted, acceptedOffers }) => {
        this.loadingMissions = false;
        this.myProjectsAsFreelancer = asFreelancerAccepted.filter(p => p?.id != null);
        this.myProjectsAsClient = asClientWithAccepted.filter(p => p?.id != null);
        this.acceptedOfferApps = (acceptedOffers ?? []).filter(a => a?.offerId != null);
        const byId = new Map<number, Project>();
        for (const p of [...this.myProjectsAsFreelancer, ...this.myProjectsAsClient]) {
          if (p?.id != null) byId.set(p.id, p);
        }
        this.myProjects = Array.from(byId.values());

        if (this.myProjects.length === 0 && this.acceptedOfferApps.length === 0) {
          if (this.missionsLoadHadError) {
            this.projectsLoadError =
              'Impossible de récupérer vos missions pour le moment. Vérifiez votre connexion, puis appuyez sur « Réessayer ». ' +
              'Si le message revient, contactez le support de la plateforme.';
    } else {
            this.projectsLoadError =
              'Aucune mission ne peut encore être liée à une sous-traitance. ' +
              'Cela concerne les projets ou offres pour lesquels une candidature a déjà été acceptée (vous côté freelancer, client, ou auteur d’offre). ' +
              'Si une mission devrait figurer ici, assurez-vous d’être connecté avec le compte concerné.';
          }
        } else {
          this.projectsLoadError = null;
        }
      },
      error: () => {
        this.loadingMissions = false;
        this.missionsLoadHadError = true;
        this.projectsLoadError =
          'Impossible de récupérer vos missions pour le moment. Vérifiez votre connexion, puis appuyez sur « Réessayer ».';
      }
    });
  }

  onMissionSelect(value: string) {
    this.selectedMission = value;
    this.form.projectId = undefined;
    this.form.offerId = undefined;
    if (value.startsWith('p:')) {
      this.form.projectId = +value.slice(2);
    } else if (value.startsWith('o:')) {
      this.form.offerId = +value.slice(2);
    }
    this.onFormRiskInputChanged();
  }
  closeForm() {
    this.showForm = false;
    this.stopRiskAgoClock();
    this.scCreateAttempted = false;
    this.scFormInteracted = false;
    this.scFieldErrors = {};
    this.createSubmitError = null;
    this.creatingSubcontract = false;
  }

  onFormRiskInputChanged() {
    this.markScFormInteracted();
    this.syncScCreateErrors();
    this.scheduleRiskRefresh(false);
  }

  onSimulationChanged() {
    if (this.simulationBudget != null) this.form.budget = this.simulationBudget;
    this.applyDurationToForm(this.simulationDurationDays);
    this.scheduleRiskRefresh(true);
  }

  private scheduleRiskRefresh(simulation: boolean) {
    if (!this.showForm || !this.currentUserId) return;
    if (this.riskDebounceHandle) clearTimeout(this.riskDebounceHandle);
    this.riskDebounceHandle = setTimeout(() => this.refreshRiskCockpit(simulation), 500);
  }

  private refreshRiskCockpit(simulation: boolean) {
    const body: SubcontractRiskCockpitRequest = {
      mainFreelancerId: this.currentUserId,
      subcontractorId: this.form.subcontractorId || undefined,
      projectId: this.form.projectId || undefined,
      offerId: this.form.offerId || undefined,
      budget: this.form.budget,
      startDate: this.form.startDate,
      deadline: this.form.deadline,
      scope: this.form.scope,
      requiredSkills: this.requiredSkills.length ? this.requiredSkills : undefined
    };
    this.riskLoading = true;
    this.riskError = null;
    this.svc.getRiskCockpit(body).subscribe({
      next: (resp) => {
        this.riskCompareSnapshot = this.prevRiskSnapshot;
        const gauges = resp.gauges ?? [];
        const byKey: Record<string, number> = {};
        for (const g of gauges) {
          if (g?.key != null) byKey[g.key] = g.score ?? 0;
        }
        this.riskCockpit = resp;
        this.riskLoading = false;
        this.riskRefreshAt = Date.now();
        this.streamNarrative(resp.streamedNarrative || '');
        this.applyRiskChartsFromResponse(resp);
        this.prevRiskSnapshot = { total: resp.totalRiskScore, byKey };
        if (simulation) {
          this.svc.auditRiskSimulation(this.currentUserId, resp).subscribe({ next: () => {}, error: () => {} });
        }
        this.cdr.markForCheck();
      },
      error: (err: { error?: unknown; message?: string }) => {
        this.riskLoading = false;
        let msg = 'Analyse risque indisponible.';
        const e = err?.error;
        if (typeof e === 'string') msg = e;
        else if (e && typeof e === 'object') {
          const o = e as { message?: string; detail?: string };
          if (o.detail) msg = o.detail;
          else if (o.message) msg = o.message;
        } else if (err?.message) msg = err.message;
        this.riskError = msg;
      }
    });
  }

  private streamNarrative(text: string) {
    this.riskNarrative = '';
    const chunks = text.split(' ');
    let i = 0;
    const ticker = setInterval(() => {
      if (i >= chunks.length) {
        clearInterval(ticker);
        return;
      }
      this.riskNarrative += (this.riskNarrative ? ' ' : '') + chunks[i++];
    }, 28);
  }

  private applyRiskChartsFromResponse(resp: SubcontractRiskCockpitResponse) {
    const gauges = resp.gauges ?? [];
    const total = Math.min(100, Math.max(0, resp.totalRiskScore ?? 0));
    const rest = Math.max(0, 100 - total);
    const mainColor = this.mainRiskArcColor(total);
    this.mainRiskGaugeData = {
      labels: ['Score', 'Restant'],
      datasets: [{ data: [total, rest], backgroundColor: [mainColor, '#e2e8f0'], borderWidth: 0 }]
    };
    const labels = gauges.map(g => g.label);
    const scores = gauges.map(g => g.score ?? 0);
    const optimal = scores.map(s => Math.min(100, Math.max(0, Math.round(s * 0.68 + 12))));
    this.riskRadarChartData = {
      labels,
      datasets: [
        {
          label: 'Actuel',
          data: scores,
          borderColor: '#ef4444',
          backgroundColor: 'rgba(239, 68, 68, 0.12)',
          borderWidth: 2,
          pointBackgroundColor: '#ef4444'
        },
        {
          label: 'Configuration cible IA',
          data: optimal,
          borderColor: '#22c55e',
          backgroundColor: 'rgba(34, 197, 94, 0.06)',
          borderWidth: 2,
          borderDash: [6, 4],
          pointBackgroundColor: '#22c55e'
        }
      ]
    };
    this.riskGaugeChartData = {
      labels: gauges.map(g => g.label),
      datasets: [{
        data: scores,
        label: 'Score risque',
        backgroundColor: gauges.map(g => this.enterpriseGaugeBarColor(g.score ?? 0)),
        borderRadius: 8
      }]
    };
  }

  /** Couleur arc principal : 0–40 vert, 41–70 orange, 71–100 rouge */
  mainRiskArcColor(score: number): string {
    if (score <= 40) return '#22c55e';
    if (score <= 70) return '#f97316';
    return '#ef4444';
  }

  private enterpriseGaugeBarColor(score: number): string {
    if (score >= 71) return '#ef4444';
    if (score >= 41) return '#f97316';
    return '#22c55e';
  }

  private gaugeColor(score: number): string {
    if (score > 80) return '#dc2626';
    if (score > 60) return '#f59e0b';
    if (score > 35) return '#2563eb';
    return '#16a34a';
  }

  gradientForGauge(score: number): string {
    const c = this.enterpriseGaugeBarColor(score);
    return `linear-gradient(90deg, ${c}88 0%, ${c} 100%)`;
  }

  gaugeFrLabel(score: number): string {
    if (score >= 71) return 'ÉLEVÉ';
    if (score >= 41) return 'MOYEN';
    return 'FAIBLE';
  }

  gaugeIcon(key: string): string {
    const k = (key || '').toLowerCase();
    if (k.includes('budget') || k.includes('finance')) return '💰';
    if (k.includes('delay') || k.includes('délai') || k.includes('time') || k.includes('temp')) return '⏱';
    if (k.includes('quality') || k.includes('qual')) return '⭐';
    if (k.includes('relation') || k.includes('people') || k.includes('hum')) return '🤝';
    if (k.includes('market') || k.includes('march')) return '📈';
    return '⚡';
  }

  gaugeTrendText(g: RiskGauge): string {
    const prev = this.riskCompareSnapshot?.byKey?.[g.key];
    const cur = g.score ?? 0;
    if (prev === undefined) return '';
    const d = cur - prev;
    if (d === 0) return '→ stable vs simulation précédente';
    if (d > 0) return `↑ +${d} vs simulation précédente`;
    return `↓ ${d} vs simulation précédente`;
  }

  /** Lignes d’impact sous les sliders (heuristique basée sur le delta global) */
  budgetSliderImpactLine(): string {
    return this.sliderImpactHint('budget');
  }

  durationSliderImpactLine(): string {
    return this.sliderImpactHint('duration');
  }

  private sliderImpactHint(kind: 'budget' | 'duration'): string {
    const prev = this.riskCompareSnapshot?.total;
    const cur = this.riskCockpit?.totalRiskScore;
    if (prev === undefined || cur === undefined) return '';
    const d = cur - prev;
    if (d === 0) return '→ Impact neutre sur le score global';
    if (kind === 'budget') {
      return d > 0
        ? `↑ Impact sur le risque global : +${d} pts`
        : `↓ Améliore le risque global : ${d} pts`;
    }
    return d > 0
      ? `↑ Impact sur le risque délai / global : +${d} pts`
      : `↓ Améliore le risque délai : ${d} pts`;
  }

  get budgetSliderPct(): number {
    const min = 100;
    const max = 6000;
    const v = this.simulationBudget ?? this.form.budget ?? min;
    return Math.min(100, Math.max(0, ((v - min) / (max - min)) * 100));
  }

  get durationSliderPct(): number {
    const min = 1;
    const max = 120;
    const v = this.simulationDurationDays;
    return Math.min(100, Math.max(0, ((v - min) / (max - min)) * 100));
  }

  riskAgoSeconds(): number {
    if (!this.riskRefreshAt) return 0;
    return Math.max(0, Math.floor((Date.now() - this.riskRefreshAt) / 1000));
  }

  private startRiskAgoClock(): void {
    this.stopRiskAgoClock();
    this.riskAgoInterval = setInterval(() => {
      this.riskUiTick++;
      this.cdr.markForCheck();
    }, 1000);
  }

  private stopRiskAgoClock(): void {
    if (this.riskAgoInterval) {
      clearInterval(this.riskAgoInterval);
      this.riskAgoInterval = null;
    }
  }

  toggleGaugeExpand(g: RiskGauge): void {
    this.expandedGaugeKey = this.expandedGaugeKey === g.key ? null : g.key;
  }

  recommendationImpactEstimate(i: number): string {
    const base = 18 - i * 4;
    return `Réduit le risque d’environ ${Math.max(6, base)} pts`;
  }

  scrollFormToEdit(): void {
    document.querySelector('.modal-ai-match .form-col-main')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  /** Résumé pied de formulaire création */
  riskFooterSummary(): string {
    const r = this.riskCockpit;
    if (!r) return 'Analyse risque : en attente…';
    const rent = this.estimatedRentabilityPct();
    const lvl = (r.level || '').toUpperCase();
    let rec = 'Valider les paramètres avant création.';
    if (lvl === 'HIGH' || lvl === 'CRITICAL') {
      rec = '⚠️ Revoir les conditions avant de créer';
    } else if (lvl === 'LOW') {
      rec = '✓ Paramètres sous contrôle';
    }
    return `Risque global : ${lvl} ${r.totalRiskScore}/100 • Rentabilité estimée : ${rent}% • Recommandation IA : ${rec}`;
  }

  estimatedRentabilityPct(): number {
    const t = this.riskCockpit?.totalRiskScore ?? 50;
    return Math.max(5, Math.min(95, Math.round(72 - t * 0.45)));
  }

  showModifyFormButton(): boolean {
    const l = (this.riskCockpit?.level || '').toUpperCase();
    return l === 'HIGH' || l === 'CRITICAL';
  }

  isCreateBlockedByCriticalRisk(): boolean {
    const r = this.riskCockpit;
    if (!r) return false;
    return (r.level || '').toUpperCase() === 'CRITICAL' && (r.totalRiskScore ?? 0) > 80;
  }

  createButtonLabel(): string {
    const l = (this.riskCockpit?.level || '').toUpperCase();
    if (l === 'HIGH' || l === 'CRITICAL') return 'Créer malgré le risque';
    return 'Créer la sous-traitance';
  }

  splitExplanationFactors(text: string): string[] {
    const t = (text || '').trim();
    if (!t) return [];
    const parts = t.split(/\n+|(?<=[.!?;])\s+/).map(s => s.trim()).filter(Boolean);
    return parts.length ? parts.slice(0, 4) : [t];
  }

  riskEnterpriseBadgeClass(level?: string): string {
    const v = (level || '').toUpperCase();
    if (v === 'CRITICAL') return 'rbd-critical';
    if (v === 'HIGH') return 'rbd-high';
    if (v === 'MEDIUM') return 'rbd-medium';
    return 'rbd-low';
  }

  riskLevelLabelFr(level?: string): string {
    const v = (level || '').toUpperCase();
    if (v === 'CRITICAL') return 'CRITIQUE';
    if (v === 'HIGH') return 'ÉLEVÉ';
    if (v === 'MEDIUM') return 'MOYEN';
    return 'FAIBLE';
  }

  riskLevelClass(level?: string): string {
    const v = (level || '').toUpperCase();
    if (v === 'CRITICAL') return 'risk-critical';
    if (v === 'HIGH') return 'risk-high';
    if (v === 'MEDIUM') return 'risk-medium';
    return 'risk-low';
  }

  applyRecommendation(r: RiskRecommendation, index: number) {
    const a = r.action;
    if (!a) return;
    if (a.type === 'ADJUST_BUDGET' && a.budgetMultiplier && this.form.budget != null) {
      this.form.budget = Math.max(0, Math.round(this.form.budget * a.budgetMultiplier));
      this.simulationBudget = this.form.budget;
    } else if (a.type === 'ADJUST_DURATION' && a.durationDeltaDays) {
      this.simulationDurationDays = Math.max(1, this.simulationDurationDays + a.durationDeltaDays);
      this.applyDurationToForm(this.simulationDurationDays);
    } else if (a.type === 'REFINE_SCOPE' && a.scopeHint) {
      const current = this.form.scope?.trim() || '';
      this.form.scope = current ? `${current}\n- ${a.scopeHint}` : a.scopeHint;
    }
    this.appliedRiskRecommendationIndexes.add(index);
    this.scheduleRiskRefresh(true);
  }

  applyAlternative(a: RiskAlternative) {
    const t = (a.changes || '').toLowerCase();
    if (t.includes('budget -20') && this.form.budget != null) {
      this.form.budget = Math.round(this.form.budget * 0.8);
      this.simulationBudget = this.form.budget;
    }
    if (t.includes('durée +7')) {
      this.simulationDurationDays += 7;
      this.applyDurationToForm(this.simulationDurationDays);
    }
    this.scheduleRiskRefresh(true);
  }

  private applyDurationToForm(days: number) {
    if (!this.form.startDate) {
      const now = new Date();
      this.form.startDate = now.toISOString().slice(0, 10);
    }
    const start = new Date(this.form.startDate as string);
    if (Number.isNaN(start.getTime())) return;
    const end = new Date(start.getTime());
    end.setDate(end.getDate() + Math.max(1, days));
    this.form.deadline = end.toISOString().slice(0, 10);
  }

  addSkillFromInput() {
    if (this.requiredSkills.length >= SC_MAX_SKILLS) return;
    const raw = this.skillInput.trim();
    if (!raw) return;
    const parts = raw.split(/[,;]+/).map(s => s.trim()).filter(Boolean);
    for (const p of parts) {
      if (this.requiredSkills.length >= SC_MAX_SKILLS) break;
      const t = p.slice(0, SC_SKILL_MAX_LEN);
      if (!this.requiredSkills.some(s => s.toLowerCase() === t.toLowerCase())) {
        this.requiredSkills = [...this.requiredSkills, t];
      }
    }
    this.skillInput = '';
    this.markScFormInteracted();
    this.syncScCreateErrors();
  }

  onSkillKeydown(ev: KeyboardEvent) {
    if (ev.key === 'Enter' || ev.key === ',') {
      ev.preventDefault();
      this.addSkillFromInput();
    }
  }

  addSuggestedSkill(s: string) {
    if (this.requiredSkills.length >= SC_MAX_SKILLS) return;
    const t = s.slice(0, SC_SKILL_MAX_LEN);
    if (!this.requiredSkills.some(x => x.toLowerCase() === t.toLowerCase())) {
      this.requiredSkills = [...this.requiredSkills, t];
      this.markScFormInteracted();
      this.syncScCreateErrors();
      this.scheduleRiskRefresh(false);
    }
  }

  removeSkill(s: string) {
    this.requiredSkills = this.requiredSkills.filter(x => x !== s);
    this.markScFormInteracted();
    this.syncScCreateErrors();
    this.scheduleRiskRefresh(false);
  }

  /** Masque les détails techniques (clés API, noms de propriétés) pour l’affichage utilisateur. */
  private userFriendlyBackendError(raw: string): string {
    const t = (raw || '').toLowerCase();
    if (
      t.includes('anthropic') ||
      t.includes('anthropic.api') ||
      t.includes('api key') ||
      t.includes('anthropic_api')
    ) {
      return 'Le service d’intelligence artificielle n’est pas disponible. Réessayez plus tard ou contactez le support.';
    }
    if (t.includes('project_id') && (t.includes('cannot be null') || t.includes('ne peut être null'))) {
      return 'Schéma base encore bloquant (project_id obligatoire). Redémarrez le microservice Sous-traitance (correctif automatique au démarrage), ou exécutez le script fix-subcontracts-mission-columns-nullable.sql sur la base.';
    }
    return raw;
  }

  runAiMatch() {
    if (!this.currentUserId || this.currentUserId <= 0) return;
    if (this.requiredSkills.length === 0) {
      this.aiError = 'Ajoutez au moins une compétence.';
      return;
    }
    this.aiError = null;
    this.loadingAi = true;
    this.aiMatches = null;
    this.svc.matchSubcontractor(this.currentUserId, this.requiredSkills).subscribe({
      next: res => {
        this.aiMatches = res.candidates ?? [];
        this.loadingAi = false;
        if (this.aiMatches.length === 0) {
          this.aiError = 'Aucun autre freelancer actif n’a été trouvé pour le matching. Vérifiez les services utilisateurs / portfolio.';
        }
      },
      error: (err: { error?: unknown; message?: string }) => {
        this.loadingAi = false;
        const e = err?.error;
        let msg = 'Erreur lors du matching. Réessayez dans un instant.';
        if (typeof e === 'string') msg = e;
        else if (e && typeof e === 'object') {
          const o = e as { message?: string; error?: string; detail?: string };
          if (o.detail) msg = o.detail;
          else if (o.message) msg = o.message;
          else if (o.error) msg = o.error;
        } else if (err?.message) msg = err.message;
        this.aiError = this.userFriendlyBackendError(msg);
      }
    });
  }

  selectMatchCandidate(c: SubcontractMatchCandidate) {
    const parts = c.fullName.trim().split(/\s+/);
    const firstName = parts[0] || 'Freelancer';
    const lastName = parts.length > 1 ? parts.slice(1).join(' ') : '';
    this.selectedFreelancer = {
      id: c.freelancerId,
      email: c.email || '',
      firstName,
      lastName,
      role: 'FREELANCER',
      isActive: true,
      createdAt: '',
      updatedAt: ''
    };
    this.form.subcontractorId = c.freelancerId;
    this.syncScCreateErrors();
    this.scheduleRiskRefresh(false);
  }

  clearFreelancer() {
    this.selectedFreelancer = null;
    this.form.subcontractorId = 0;
    this.syncScCreateErrors();
    this.scheduleRiskRefresh(false);
  }

  recommendationClass(r: string): string {
    const u = (r || '').toUpperCase();
    if (u.includes('HIGHLY')) return 'rec-high';
    if (u.includes('RECOMMENDED') && !u.includes('HIGHLY')) return 'rec-mid';
    return 'rec-low';
  }

  recommendationLabel(r: string): string {
    const u = (r || '').toUpperCase();
    if (u.includes('HIGHLY')) return 'Très recommandé';
    if (u.includes('RECOMMENDED') && !u.includes('HIGHLY')) return 'Recommandé';
    return 'Possible';
  }

  onPresentationSelected(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.currentUserId) return;
    const lower = file.name.toLowerCase();
    if (!lower.endsWith('.mp4') && !lower.endsWith('.mp3')) {
      this.mediaError = 'Formats acceptés : MP4 (vidéo, max ~3 min conseillé) ou MP3 (audio, max ~5 min conseillé).';
      return;
    }
    this.mediaError = null;
    this.mediaUploading = true;
    this.svc.uploadPresentationMedia(this.currentUserId, file).subscribe({
      next: res => {
        this.form.mediaUrl = res.mediaUrl;
        this.form.mediaType = res.mediaType;
        this.presentationFileName = file.name;
        this.mediaUploading = false;
      },
      error: () => {
        this.mediaUploading = false;
        this.mediaError = 'Échec de l’upload. Réessayez ou choisissez un fichier plus léger.';
      }
    });
  }

  clearPresentation() {
    this.form.mediaUrl = undefined;
    this.form.mediaType = undefined;
    this.presentationFileName = null;
    this.mediaError = null;
  }

  createSubcontract() {
    this.createSubmitError = null;
    if (!this.currentUserId || this.currentUserId <= 0) {
      this.createSubmitError = 'Session invalide ou expirée. Déconnectez-vous et reconnectez-vous.';
      return;
    }
    if (this.loadingMissions) {
      this.createSubmitError = 'Vos missions sont encore en cours de chargement. Patientez quelques secondes.';
      return;
    }
    this.scCreateAttempted = true;
    this.scFormInteracted = true;
    const v = validateSubcontractCreateForm(this.buildCreateValidationInput(), { requireBudget: true });
    this.scFieldErrors = v.errors;
    if (!v.valid) {
      this.createSubmitError =
        'Vérifiez les champs obligatoires (titre, mission, compétences, sous-traitant). Faites défiler vers le haut si les messages sont au-dessus.';
      setTimeout(() => {
        document
          .querySelector('.modal-ai-match .field-invalid')
          ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 0);
      return;
    }
    const payload: SubcontractRequest = {
      subcontractorId: this.form.subcontractorId,
      title: this.form.title,
      scope: this.form.scope,
      category: this.form.category,
      budget: this.form.budget,
      currency: this.form.currency,
      startDate: this.form.startDate,
      deadline: this.form.deadline,
      contractId: this.form.contractId,
      mediaUrl: this.form.mediaUrl,
      mediaType: this.form.mediaType,
      requiredSkills: this.requiredSkills.length > 0 ? [...this.requiredSkills] : undefined
    };
    if (this.form.projectId != null && this.form.projectId > 0) {
      payload.projectId = this.form.projectId;
    }
    if (this.form.offerId != null && this.form.offerId > 0) {
      payload.offerId = this.form.offerId;
    }
    this.creatingSubcontract = true;
    this.svc
      .create(this.currentUserId, payload)
      .pipe(finalize(() => (this.creatingSubcontract = false)))
      .subscribe({
        next: (created) => {
          this.createSubmitError = null;
          if (this.riskCockpit) {
            this.svc
              .confirmRisk(created.id, this.currentUserId, {
                totalRiskScore: this.riskCockpit.totalRiskScore,
                selectedAlternativeLabel: this.riskCockpit.alternatives?.[0]?.label,
                summary: this.riskNarrative
              })
              .subscribe({ next: () => {}, error: () => {} });
          }
          this.showForm = false;
          this.load();
        },
        error: (err: { error?: unknown; message?: string }) => {
          const e = err?.error;
          let msg = 'Impossible de créer la sous-traitance. Réessayez ou vérifiez votre connexion.';
          if (typeof e === 'string') msg = e;
          else if (e && typeof e === 'object') {
            const o = e as { message?: string; error?: string; detail?: string };
            if (o.detail) msg = o.detail;
            else if (o.message) msg = o.message;
            else if (o.error) msg = typeof o.error === 'string' ? o.error : JSON.stringify(o.error);
          } else if (err?.message) msg = err.message;
          this.createSubmitError = this.userFriendlyBackendError(msg);
        }
      });
  }

  select(s: Subcontract) {
    this.selected = s;
    this.financialBi = null;
    this.financialError = null;
    this.chatInput = '';
    this.negotiationError = null;
    this.negotiationRound = null;
    this.prefillNegotiationForm(s);
    this.loadChatMessages();
  }

  closeDetail() {
    this.selected = null;
    this.financialBi = null;
    this.financialError = null;
    this.chatMessages = [];
    this.chatInput = '';
    this.negotiationError = null;
    this.negotiationRound = null;
    this.negotiationBusy = false;
  }

  canShowNegotiationPanel(): boolean {
    return this.isNegotiationEligible(this.selected);
  }

  isNegotiationEligible(sc: Subcontract | null | undefined): boolean {
    const s = sc?.status;
    return s === 'PROPOSED' || s === 'COUNTER_OFFERED' || s === 'AI_MEDIATION' || s === 'NEGOTIATED' || s === 'NEGOTIATION_IMPASSE';
  }

  negotiationAvailabilityMessage(sc: Subcontract | null | undefined): string {
    if (!sc) return 'Sélectionnez une sous-traitance pour accéder à la négociation IA.';
    if (this.isNegotiationEligible(sc)) return 'Négociation IA disponible pour cette mission.';
    return `Négociation IA indisponible pour le statut actuel (${sc.status}).`;
  }

  openNegotiationFromCard(s: Subcontract, ev: Event): void {
    ev.stopPropagation();
    this.select(s);
    setTimeout(() => {
      document.querySelector('.negotiation-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 80);
  }

  private prefillNegotiationForm(sc: Subcontract): void {
    this.negotiationForm = {
      proposedBudget: sc.budget ?? null,
      proposedDurationDays: this.durationDaysFromSubcontract(sc),
      note: ''
    };
  }

  private durationDaysFromSubcontract(sc: Subcontract): number | null {
    if (!sc.startDate || !sc.deadline) return null;
    const a = new Date(sc.startDate).getTime();
    const b = new Date(sc.deadline).getTime();
    if (Number.isNaN(a) || Number.isNaN(b) || b < a) return null;
    return Math.max(1, Math.ceil((b - a) / 86400000));
  }

  sendCounterOffer(): void {
    if (!this.selected) return;
    if (!this.selected.subcontractorId) {
      this.negotiationError = 'Sous-traitant introuvable pour cette mission.';
      return;
    }
    if (!this.negotiationForm.proposedBudget || this.negotiationForm.proposedBudget <= 0) {
      this.negotiationError = 'Le budget proposé doit être supérieur à 0.';
      return;
    }
    if (!this.negotiationForm.proposedDurationDays || this.negotiationForm.proposedDurationDays <= 0) {
      this.negotiationError = 'La durée proposée doit être au moins 1 jour.';
      return;
    }
    const body: CounterOfferRequest = {
      proposedBudget: this.negotiationForm.proposedBudget,
      proposedDurationDays: this.negotiationForm.proposedDurationDays,
      note: this.negotiationForm.note?.trim() || undefined
    };
    this.negotiationBusy = true;
    this.negotiationError = null;
    this.svc.counterOffer(this.selected.id, this.selected.subcontractorId, body).subscribe({
      next: (round) => {
        this.negotiationRound = round;
        this.negotiationBusy = false;
        this.reloadSelected();
      },
      error: (err: { error?: unknown; message?: string }) => {
        this.negotiationBusy = false;
        const e = err?.error as { message?: string; detail?: string } | string | undefined;
        if (typeof e === 'string' && e.trim()) this.negotiationError = e;
        else if (e && typeof e === 'object') this.negotiationError = e.detail || e.message || 'Impossible d’envoyer la contre-offre.';
        else this.negotiationError = err?.message || 'Impossible d’envoyer la contre-offre.';
      }
    });
  }

  mediateWithAi(): void {
    if (!this.selected) return;
    this.negotiationBusy = true;
    this.negotiationError = null;
    this.svc.aiMediate(this.selected.id, this.currentUserId, { note: this.negotiationForm.note?.trim() || undefined }).subscribe({
      next: (round) => {
        this.negotiationRound = round;
        this.negotiationBusy = false;
        this.reloadSelected();
      },
      error: (err: { error?: unknown; message?: string }) => {
        this.negotiationBusy = false;
        const e = err?.error as { message?: string; detail?: string } | string | undefined;
        if (typeof e === 'string' && e.trim()) this.negotiationError = e;
        else if (e && typeof e === 'object') this.negotiationError = e.detail || e.message || 'Médiation IA indisponible.';
        else this.negotiationError = err?.message || 'Médiation IA indisponible.';
      }
    });
  }

  acceptNegotiated(): void {
    if (!this.selected) return;
    this.accept(this.selected.id);
  }

  private reloadSelected(): void {
    if (!this.selected) return;
    this.svc.getById(this.selected.id).subscribe({
      next: (s) => {
        this.selected = s;
        this.load();
      },
      error: () => {}
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

  /** Initiales affichées dans l’avatar du panneau détail. */
  get detailSubInitials(): string {
    const n = this.selected?.subcontractorName?.trim();
    if (!n) return '?';
    const parts = n.split(/\s+/).filter(Boolean);
    if (parts.length === 0) return '?';
    if (parts.length === 1) {
      const p = parts[0];
      return p.length >= 2 ? p.substring(0, 2).toUpperCase() : (p[0] + p[0]).toUpperCase();
    }
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  loadFinancialAnalysis() {
    if (!this.selected || !this.currentUserId) return;
    this.loadingFinancial = true;
    this.financialError = null;
    this.financialBi = null;
    this.svc.getFinancialAnalysis(this.selected.id, this.currentUserId).subscribe({
      next: f => {
        this.financialBi = f;
        this.applyFinancialCharts(f);
        this.loadingFinancial = false;
      },
      error: (err: { error?: unknown; message?: string }) => {
        this.loadingFinancial = false;
        const e = err?.error;
        let msg = 'Analyse indisponible.';
        if (typeof e === 'string') msg = e;
        else if (e && typeof e === 'object') {
          const o = e as { message?: string; detail?: string };
          if (o.detail) msg = o.detail;
          else if (o.message) msg = o.message;
        }
        this.financialError = this.userFriendlyBackendError(msg);
      }
    });
  }

  private applyFinancialCharts(f: SubcontractFinancialAnalysisResponse) {
    const score = Math.max(0, Math.min(100, f.rentabilityScore ?? 0));
    this.gaugeChartData = {
      ...this.gaugeChartData,
      datasets: [{ ...this.gaugeChartData.datasets[0], data: [score, Math.max(0, 100 - score)] }]
    };

    const sub = f.subcontractBudget ?? 0;
    const others = f.otherSubcontractsOnContractTotal ?? 0;
    let rem = f.remainingMarginForPrincipal ?? 0;
    if (rem < 0) rem = 0;
    const principal = f.principalContractBudget;
    if (principal != null && principal > 0) {
      this.budgetChartData = {
        labels: ['Cette ST', 'Autres ST (contrat)', 'Marge résiduelle'],
        datasets: [
          {
            data: [sub, others, rem],
            backgroundColor: ['#2563eb', '#94a3b8', '#22c55e'],
            borderWidth: 0
          }
        ]
      };
    } else {
      this.budgetChartData = {
        labels: ['Budget sous-traitance'],
        datasets: [{ data: [sub || 0.0001], backgroundColor: ['#2563eb'], borderWidth: 0 }]
      };
    }

    const tl = f.financialTimeline ?? [];
    this.timelineChartData = {
      ...this.timelineChartData,
      labels: tl.map(t => t.date),
      datasets: [
        {
          ...this.timelineChartData.datasets[0],
          data: tl.map(t => Number(t.amount) || 0)
        }
      ]
    };
  }

  verdictLabel(v: string): string {
    const u = (v || '').toUpperCase();
    if (u.includes('EXCELLENT')) return 'Excellent choix';
    if (u.includes('GOOD')) return 'Bon choix';
    if (u.includes('NOT') || u.includes('NO_RECOMMENDED')) return 'Non recommandé';
    if (u.includes('RISKY')) return 'Risqué';
    return v || '—';
  }

  verdictClass(v: string): string {
    const u = (v || '').toUpperCase();
    if (u.includes('EXCELLENT')) return 'verdict-excellent';
    if (u.includes('GOOD')) return 'verdict-good';
    if (u.includes('NOT') || u.includes('NO_RECOMMENDED')) return 'verdict-bad';
    return 'verdict-risk';
  }

  propose(id: number) { this.svc.propose(id).subscribe(() => this.reload()); }
  accept(id: number) { this.svc.accept(id).subscribe(() => this.reload()); }
  startWork(id: number) { this.svc.startWork(id).subscribe(() => this.reload()); }
  complete(id: number) { this.svc.complete(id).subscribe(() => this.reload()); }
  close(id: number) { this.svc.close(id).subscribe(() => this.reload()); }
  cancel(id: number) {
    this.pendingSubcontractId = id;
    this.pendingDeliverableId = null;
    this.openActionModal({
      type: 'cancel-subcontract',
      title: 'Annuler la sous-traitance',
      message: 'Indiquez la raison de l’annulation.',
      confirmLabel: 'Annuler la mission',
      reasonLabel: 'Raison de l’annulation',
      reasonPlaceholder: 'Ex: délai non réaliste, changement de priorité...'
    });
  }
  deleteSubcontract(id: number) {
    this.pendingSubcontractId = id;
    this.pendingDeliverableId = null;
    this.openActionModal({
      type: 'delete-subcontract',
      title: 'Supprimer la sous-traitance',
      message: 'Cette action est définitive. Voulez-vous continuer ?',
      confirmLabel: 'Supprimer'
    });
  }

  openDeliverableForm() {
    this.delAttempted = false;
    this.delFieldErrors = {};
    this.delForm = { title: '' };
    this.showDeliverableForm = true;
  }
  closeDeliverableForm() {
    this.showDeliverableForm = false;
    this.delAttempted = false;
    this.delFieldErrors = {};
  }

  onDeliverableFieldChange(): void {
    if (!this.delAttempted) return;
    this.delFieldErrors = validateDeliverableModal({
      title: this.delForm.title,
      description: this.delForm.description,
      deadline: this.delForm.deadline
    }).errors;
  }

  addDeliverable() {
    if (!this.selected) return;
    this.delAttempted = true;
    const dv = validateDeliverableModal({
      title: this.delForm.title,
      description: this.delForm.description,
      deadline: this.delForm.deadline
    });
    this.delFieldErrors = dv.errors;
    if (!dv.valid) return;
    this.svc.addDeliverable(this.selected.id, this.delForm).subscribe(() => {
      this.showDeliverableForm = false;
      this.svc.getById(this.selected!.id).subscribe(s => this.selected = s);
      this.load();
    });
  }

  reviewDeliverable(deliverableId: number, approved: boolean) {
    if (!this.selected) return;
    if (!approved) {
      this.pendingDeliverableId = deliverableId;
      this.pendingSubcontractId = this.selected.id;
      this.openActionModal({
        type: 'reject-deliverable',
        title: 'Rejeter le livrable',
        message: 'Expliquez clairement au sous-traitant ce qui doit être corrigé.',
        confirmLabel: 'Rejeter',
        reasonLabel: 'Raison du rejet',
        reasonPlaceholder: 'Ex: non conformité des critères d’acceptation...'
      });
      return;
    }
    this.svc.reviewDeliverable(this.selected.id, deliverableId, { approved, reviewNote: '' }).subscribe(() => {
      this.svc.getById(this.selected!.id).subscribe(s => this.selected = s);
      this.load();
    });
  }

  deleteDeliverable(deliverableId: number) {
    if (!this.selected) return;
    this.pendingDeliverableId = deliverableId;
    this.pendingSubcontractId = this.selected.id;
    this.openActionModal({
      type: 'delete-deliverable',
      title: 'Supprimer le livrable',
      message: 'Ce livrable sera supprimé définitivement.',
      confirmLabel: 'Supprimer'
    });
  }

  private openActionModal(cfg: {
    type: 'cancel-subcontract' | 'reject-deliverable' | 'delete-subcontract' | 'delete-deliverable';
    title: string;
    message: string;
    confirmLabel: string;
    reasonLabel?: string;
    reasonPlaceholder?: string;
  }): void {
    this.actionModalType = cfg.type;
    this.actionModalTitle = cfg.title;
    this.actionModalMessage = cfg.message;
    this.actionModalConfirmLabel = cfg.confirmLabel;
    this.actionModalReasonLabel = cfg.reasonLabel || '';
    this.actionModalReasonPlaceholder = cfg.reasonPlaceholder || '';
    this.actionModalReason = '';
    this.actionModalError = null;
    this.actionModalOpen = true;
  }

  closeActionModal(): void {
    this.actionModalOpen = false;
    this.actionModalType = null;
    this.actionModalError = null;
    this.actionModalReason = '';
  }

  confirmActionModal(): void {
    if (!this.actionModalType) return;
    if (this.actionModalType === 'cancel-subcontract') {
      if (!this.pendingSubcontractId) return;
      const err = validatePromptReason(this.actionModalReason);
      if (err) {
        this.actionModalError = err;
        return;
      }
      this.svc.cancel(this.pendingSubcontractId, this.actionModalReason.trim()).subscribe(() => {
        this.closeActionModal();
        this.reload();
      });
      return;
    }

    if (this.actionModalType === 'reject-deliverable') {
      if (!this.pendingSubcontractId || !this.pendingDeliverableId) return;
      const err = validatePromptReason(this.actionModalReason);
      if (err) {
        this.actionModalError = err;
        return;
      }
      this.svc.reviewDeliverable(this.pendingSubcontractId, this.pendingDeliverableId, {
        approved: false,
        reviewNote: this.actionModalReason.trim()
      }).subscribe(() => {
        this.closeActionModal();
        if (this.selected) {
          this.svc.getById(this.selected.id).subscribe(s => this.selected = s);
        }
        this.load();
      });
      return;
    }

    if (this.actionModalType === 'delete-subcontract') {
      if (!this.pendingSubcontractId) return;
      this.svc.delete(this.pendingSubcontractId).subscribe(() => {
        this.closeActionModal();
        this.selected = null;
        this.load();
      });
      return;
    }

    if (this.actionModalType === 'delete-deliverable') {
      if (!this.pendingSubcontractId || !this.pendingDeliverableId) return;
      this.svc.deleteDeliverable(this.pendingSubcontractId, this.pendingDeliverableId).subscribe(() => {
        this.closeActionModal();
        if (this.selected) {
          this.svc.getById(this.selected.id).subscribe(s => this.selected = s);
        }
        this.load();
      });
    }
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'badge-secondary', PROPOSED: 'badge-info', ACCEPTED: 'badge-primary',
      COUNTER_OFFERED: 'badge-warning', AI_MEDIATION: 'badge-primary', NEGOTIATED: 'badge-success',
      NEGOTIATION_IMPASSE: 'badge-danger',
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

  switchTab(tab: 'list' | 'history' | 'predictive' | 'coach') {
    this.activeTab = tab;
    if (tab === 'history' && !this.history) {
      this.loadHistory();
    } else if (tab === 'predictive' && !this.predictive) {
      this.loadPredictiveDashboard();
    } else if (tab === 'coach' && !this.selected && this.subcontracts.length > 0) {
      // Préselection d'une mission pour éviter un contexte vide au premier accès.
      this.select(this.subcontracts[0]);
    }
  }

  loadPredictiveDashboard() {
    if (!this.currentUserId || this.currentUserId <= 0) {
      this.loadingPredictive = false;
      return;
    }
    this.loadingPredictive = true;
    this.svc.getPredictiveDashboard(this.currentUserId).subscribe({
      next: p => {
        this.predictive = p;
        this.hiddenPredictiveAlerts.clear();
        this.loadingPredictive = false;
        this.applyPredictiveCharts(p);
      },
      error: () => {
        this.loadingPredictive = false;
      }
    });
  }

  private applyPredictiveCharts(p: PredictiveDashboardResponse) {
    const rows = this.successRows;
    const entries = rows.map(r => [r.category, r.successRate] as const);
    this.successByCategoryChartData = {
      labels: entries.map(([k]) => k),
      datasets: [{
        data: entries.map(([, v]) => Number(v) || 0),
        label: 'Succès (%)',
        borderRadius: 8,
        backgroundColor: entries.map(([, v]) => this.successColor(Number(v) || 0))
      }]
    };
    this.successByCategoryChartOptions = {
      responsive: true,
      indexAxis: 'y',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const row = rows[ctx.dataIndex];
              if (!row) return '';
              return `${row.category} : ${row.missions} missions, ${row.successMissions} réussies, ${row.incidents} incident(s)`;
            }
          }
        }
      },
      scales: {
        x: { beginAtZero: true, max: 100, grid: { color: 'rgba(148,163,184,0.2)' } },
        y: { grid: { display: false } }
      }
    };

    const trend = p.riskTrend || [];
    this.riskTrendChartData = {
      labels: trend.map(t => t.month),
      datasets: [{
        data: trend.map(t => Number(t.avgRiskScore) || 0),
        label: 'Risque moyen',
        borderColor: '#dc2626',
        backgroundColor: 'rgba(220,38,38,.14)',
        pointBackgroundColor: '#dc2626',
        pointRadius: 4,
        pointHoverRadius: 6,
        fill: true,
        tension: 0.25
      }]
    };
    this.riskTrendChartOptions = {
      responsive: true,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => `Score moyen : ${ctx.parsed.y}/100`
          }
        }
      },
      scales: {
        y: { beginAtZero: true, max: 100 },
        x: { grid: { display: false } }
      }
    };
  }

  get hasPredictiveInsight(): boolean {
    if (!this.predictive) return false;
    return this.successRows.length > 0 || this.riskTrendPoints.length > 0 || this.predictiveAlerts.length > 0;
  }

  get dashboardNarrative(): string {
    const raw = this.predictive?.narrativeSummary || '';
    return raw.replace('Analyse générée sans LLM', 'Analyse IA • Mise à jour automatique');
  }

  get monthlyHintText(): string {
    return this.predictive?.monthlyReportHint || 'Analyse IA • Mise à jour automatique';
  }

  get riskTrendPoints(): RiskTrendPoint[] {
    return this.predictive?.riskTrend ?? [];
  }

  get globalPredictiveScore(): number {
    const latestRisk = this.riskTrendPoints[this.riskTrendPoints.length - 1]?.avgRiskScore ?? 35;
    return Math.max(0, Math.min(100, Math.round(100 - latestRisk)));
  }

  get globalPredictiveScoreClass(): string {
    const s = this.globalPredictiveScore;
    if (s >= 80) return 'ok';
    if (s >= 60) return 'warn';
    return 'danger';
  }

  get monthTrendDelta(): number {
    const trend = this.riskTrendPoints;
    if (trend.length < 2) return 0;
    const prevHealth = 100 - trend[trend.length - 2].avgRiskScore;
    return Math.round(this.globalPredictiveScore - prevHealth);
  }

  get monthTrendText(): string {
    const d = this.monthTrendDelta;
    if (d === 0) return 'Stable ce mois';
    return d > 0 ? `+${d} pts ce mois` : `${d} pts ce mois`;
  }

  get monthTrendClass(): string {
    const d = this.monthTrendDelta;
    if (d > 0) return 'up';
    if (d < 0) return 'down';
    return 'flat';
  }

  get predictiveAlerts(): Array<{ id: string; level: 'URGENT' | 'ATTENTION' | 'INFO'; title: string; detail: string; primary: string; secondary: string }> {
    if (!this.predictive) return [];
    const alerts: Array<{ id: string; level: 'URGENT' | 'ATTENTION' | 'INFO'; title: string; detail: string; primary: string; secondary: string }> = [];
    for (const r of this.predictive.topRiskySubcontractors ?? []) {
      if ((r.riskScore ?? 0) >= 70) {
        alerts.push({
          id: `risk-${r.name}`,
          level: (r.riskScore ?? 0) >= 85 ? 'URGENT' : 'ATTENTION',
          title: `Sous-traitance à risque — ${r.name}`,
          detail: r.note || `Score de risque ${r.riskScore}/100`,
          primary: 'Analyser',
          secondary: 'Voir détails'
        });
      }
    }
    if ((this.predictive.nextIncidentPrediction || '').trim().length > 6 && !this.isIncidentSafe) {
      alerts.unshift({
        id: 'incident-next',
        level: 'URGENT',
        title: 'Incident probable détecté',
        detail: this.predictive.nextIncidentPrediction,
        primary: 'Contacter',
        secondary: 'Voir livrable'
      });
    }
    return alerts.filter(a => !this.hiddenPredictiveAlerts.has(a.id)).slice(0, 5);
  }

  dismissAlert(alertId: string): void {
    this.hiddenPredictiveAlerts.add(alertId);
  }

  scrollToAlerts(): void {
    document.querySelector('.ai-alerts-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  get successRows(): Array<{ category: string; successRate: number; missions: number; successMissions: number; incidents: number }> {
    const map = this.predictive?.successRateByCategory || {};
    return Object.entries(map).map(([category, value]) => {
      const missions = this.subcontracts.filter(s => (s.category || '').toUpperCase() === category.toUpperCase()).length;
      const rate = Math.max(0, Math.min(100, Number(value) || 0));
      const successMissions = Math.round((missions * rate) / 100);
      const incidents = Math.max(0, missions - successMissions);
      return { category, successRate: rate, missions, successMissions, incidents };
    }).sort((a, b) => b.successRate - a.successRate);
  }

  successColor(rate: number): string {
    if (rate >= 80) return '#16a34a';
    if (rate >= 60) return '#ea580c';
    return '#dc2626';
  }

  get profitableCards(): Array<SubcontractorInsight & { reliabilityScore: number; activeMissions: number; successCount: number; incidentCount: number; trend: 'up' | 'down' | 'flat'; rank: number }> {
    const source = [...(this.predictive?.topProfitableSubcontractors ?? [])];
    const allEqual = source.length > 1 && source.every(s => s.profitabilityScore === source[0].profitabilityScore);
    if (allEqual) source.sort((a, b) => a.name.localeCompare(b.name));
    return source.map((s, i) => {
      const missions = this.subcontracts.filter(x => (x.subcontractorName || '').toLowerCase() === s.name.toLowerCase());
      const successCount = missions.filter(m => ['COMPLETED', 'CLOSED'].includes(m.status)).length;
      const incidentCount = missions.filter(m => ['CANCELLED', 'REJECTED'].includes(m.status)).length;
      const reliability = Math.max(30, Math.min(100, Math.round(100 - s.riskScore)));
      return {
        ...s,
        reliabilityScore: reliability,
        activeMissions: missions.filter(m => ['PROPOSED', 'ACCEPTED', 'IN_PROGRESS'].includes(m.status)).length,
        successCount,
        incidentCount,
        trend: i % 2 === 0 ? 'up' : 'flat',
        rank: i + 1
      };
    });
  }

  get riskyCards(): Array<SubcontractorInsight & { reliabilityScore: number; activeMissions: number; successCount: number; incidentCount: number; trend: 'up' | 'down' | 'flat'; rank: number; calculating: boolean }> {
    const source = [...(this.predictive?.topRiskySubcontractors ?? [])];
    const allEqual = source.length > 1 && source.every(s => s.riskScore === source[0].riskScore);
    if (allEqual) source.sort((a, b) => a.name.localeCompare(b.name));
    return source.map((s, i) => {
      const missions = this.subcontracts.filter(x => (x.subcontractorName || '').toLowerCase() === s.name.toLowerCase());
      const successCount = missions.filter(m => ['COMPLETED', 'CLOSED'].includes(m.status)).length;
      const incidentCount = missions.filter(m => ['CANCELLED', 'REJECTED'].includes(m.status)).length;
      return {
        ...s,
        reliabilityScore: Math.max(10, Math.min(100, Math.round(100 - s.riskScore))),
        activeMissions: missions.filter(m => ['PROPOSED', 'ACCEPTED', 'IN_PROGRESS'].includes(m.status)).length,
        successCount,
        incidentCount,
        trend: i % 2 === 0 ? 'down' : 'flat',
        rank: i + 1,
        calculating: allEqual
      };
    });
  }

  progressClass(rank: number): string {
    if (rank === 1) return 'rank-gold';
    if (rank === 2) return 'rank-silver';
    if (rank === 3) return 'rank-bronze';
    return 'rank-default';
  }

  openRiskSidebar(name: string): void {
    this.riskInsightOpen = true;
    this.riskInsightSelectedName = name;
    const target = this.riskyCards.find(c => c.name === name);
    const score = Math.max(0, Math.min(100, target?.riskScore ?? 0));
    this.riskInsightScore = score;
    this.riskInsightLevel = this.toRiskLevel(score);
    this.riskInsightSummary = target?.note || `Risque évalué à ${score}/100 pour ce sous-traitant.`;
    this.riskInsightCauses = this.buildRiskCauses(target?.name || name);
    this.riskInsightActions = this.buildRiskActions(target?.name || name, score);
  }

  closeRiskSidebar(): void {
    this.riskInsightOpen = false;
    this.riskInsightSelectedName = '';
    this.riskInsightScore = 0;
    this.riskInsightLevel = 'LOW';
    this.riskInsightSummary = '';
    this.riskInsightCauses = [];
    this.riskInsightActions = [];
  }

  riskInsightLevelClass(): string {
    if (this.riskInsightLevel === 'CRITICAL') return 'danger';
    if (this.riskInsightLevel === 'HIGH') return 'warn';
    if (this.riskInsightLevel === 'MEDIUM') return 'mid';
    return 'ok';
  }

  private toRiskLevel(score: number): 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' {
    if (score >= 85) return 'CRITICAL';
    if (score >= 70) return 'HIGH';
    if (score >= 45) return 'MEDIUM';
    return 'LOW';
  }

  private buildRiskCauses(name: string): string[] {
    const byName = this.subcontracts.filter(s => (s.subcontractorName || '').toLowerCase() === name.toLowerCase());
    const delayed = byName.filter(s => ['IN_PROGRESS'].includes(s.status) && !!s.deadline).length;
    const budgetPressure = byName.filter(s => (s.budget ?? 0) > 0).length > 0;
    const causes: string[] = [];
    if (delayed > 0) causes.push(`Plusieurs missions en cours nécessitent un suivi rapproché (${delayed} en exécution).`);
    if (budgetPressure) causes.push(`Pression budget/planning détectée sur des missions actives.`);
    const incidentAlert = this.predictiveAlerts.find(a => a.id.startsWith('incident-'));
    if (incidentAlert) causes.push(incidentAlert.detail);
    if (!causes.length) causes.push(`Signaux de risque opérationnel détectés dans l'historique de la mission.`);
    return causes.slice(0, 3);
  }

  private buildRiskActions(name: string, score: number): string[] {
    const actions: string[] = [];
    actions.push(`Planifier un point de pilotage avec ${name} sous 24h pour sécuriser les livrables prioritaires.`);
    actions.push(`Ajouter un jalon intermédiaire validé avec critères d'acceptation écrits.`);
    if (score >= 70) actions.push(`Prévoir un plan B (ressource de backup) si aucun progrès concret sous 72h.`);
    else actions.push(`Maintenir un suivi hebdomadaire avec indicateurs délai / qualité / budget.`);
    return actions;
  }

  navigateToSubcontractorProfile(name: string): void {
    const target = this.subcontracts.find(s => (s.subcontractorName || '').toLowerCase() === name.toLowerCase());
    if (!target?.subcontractorId) return;
    this.router.navigate(['/dashboard/users', target.subcontractorId]);
  }

  get seasonalityLegend(): string {
    if (!this.predictive?.bestMonthsForSubcontracting?.length) {
      return 'Donnees insuffisantes — base sur les moyennes marche';
    }
    return 'IA : activite moyenne, delais tendus en fin de mois. Preferez les periodes vertes.';
  }

  seasonalityForMonth(monthIndex: number): { state: 'good' | 'mid' | 'bad' | 'none'; score: number } {
    const monthNames = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aou', 'Sep', 'Oct', 'Nov', 'Dec'];
    const m = monthNames[monthIndex];
    const entry = (this.predictive?.bestMonthsForSubcontracting ?? []).find(x => (x.month || '').toLowerCase().startsWith(m.toLowerCase()));
    if (!entry) return { state: 'none', score: 0 };
    if (entry.score >= 70) return { state: 'good', score: entry.score };
    if (entry.score >= 45) return { state: 'mid', score: entry.score };
    return { state: 'bad', score: entry.score };
  }

  get isIncidentSafe(): boolean {
    const txt = (this.predictive?.nextIncidentPrediction || '').toLowerCase();
    return !txt || txt.includes('aucun incident') || txt.includes('sous contrôle') || txt.includes('sous controle');
  }

  get lastAnalysisLabel(): string {
    const iso = this.predictive?.generatedAt;
    if (!iso) return 'Analyse en attente';
    return new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  downloadMonthlyReport(): void {
    if (!this.predictive) return;
    const body = [
      `Rapport IA mensuel`,
      `Date: ${new Date().toLocaleDateString('fr-FR')}`,
      '',
      this.dashboardNarrative,
      '',
      this.monthlyHintText
    ].join('\n');
    const blob = new Blob([body], { type: 'text/plain;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `rapport-ia-${new Date().toISOString().slice(0, 10)}.txt`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  sendMonthlyReportByEmail(): void {
    if (!this.predictive) return;
    const monthLabel = new Date(this.predictive.generatedAt || Date.now()).toLocaleDateString('fr-FR', {
      month: 'long',
      year: 'numeric'
    });
    const subject = `Rapport IA mensuel - ${monthLabel}`;
    const body = [
      'Bonjour,',
      '',
      `Voici le rapport IA mensuel (${monthLabel}) :`,
      '',
      this.dashboardNarrative || 'Analyse IA • Mise à jour automatique',
      '',
      this.monthlyHintText,
      '',
      '— Dashboard Prédictif IA'
    ].join('\n');

    const mailto = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    window.location.href = mailto;
  }

  loadHistory() {
    if (!this.currentUserId || this.currentUserId <= 0) {
      this.loadingHistory = false;
      return;
    }
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
