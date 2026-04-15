import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CoachInsightRequest,
  CoachInsightResponse,
  CoachWalletService,
  InsufficientPointsBody,
  WalletMeResponse,
} from '../../../../core/services/coach-wallet.service';
import { Subcontract } from '../../../../core/services/subcontract.service';

type UiState = 'loading' | 'blocked' | 'free' | 'insufficient' | 'advanced' | 'error';
type ActionPlanItem = { title: string; detail: string; deadline: string; priority: string };

@Component({
  selector: 'app-coach-insight-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './coach-insight-panel.html',
  styleUrl: './coach-insight-panel.scss',
})
export class CoachInsightPanel implements OnChanges {
  @Input({ required: true }) userId!: number;
  @Input() selected: Subcontract | null = null;

  readonly Math = Math;

  state: UiState = 'loading';
  wallet: WalletMeResponse | null = null;
  /** Erreur chargement wallet (pleine page) */
  errorMsg = '';
  /** Erreur dernière action (analyse, etc.) — visible dans la carte */
  actionError = '';
  /** Succès dernière action (demande recharge, etc.) */
  actionSuccess = '';
  copiedPlanMessage = false;
  insight: CoachInsightResponse | null = null;
  busy = false;
  insufficient: InsufficientPointsBody | null = null;
  rechargeMsg = '';
  /** Coût analyse avancée (RISK_DEEP_ANALYSIS) */
  advancedCost = 1000;

  constructor(private coachApi: CoachWalletService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['userId']) {
      this.refreshWallet();
    } else if (changes['selected'] && !changes['selected'].firstChange) {
      this.insight = null;
      this.actionError = '';
    }
  }

  /** Rechargement complet (onglet, contexte) : efface le résultat précédent. */
  refreshWallet(): void {
    if (!this.userId) {
      this.state = 'error';
      this.errorMsg = 'Utilisateur non connecté.';
      return;
    }
    this.state = 'loading';
    this.errorMsg = '';
    this.actionError = '';
    this.actionSuccess = '';
    this.insight = null;
    this.insufficient = null;
    this.coachApi.getMyWallet(this.userId).subscribe({
      next: (w) => this.applyWalletState(w),
      error: (e) => {
        this.state = 'error';
        this.errorMsg = this.extractHttpError(e) || 'Impossible de charger le wallet coaching.';
      },
    });
  }

  /**
   * Met à jour solde / état sans effacer l’analyse affichée (après succès API insight).
   */
  private syncWalletAfterAction(): void {
    if (!this.userId) return;
    this.coachApi.getMyWallet(this.userId).subscribe({
      next: (w) => this.applyWalletState(w),
      error: () => {},
    });
  }

  private applyWalletState(w: WalletMeResponse): void {
    this.wallet = w;
    const ra = w.remainingAnalysesByFeature?.['RISK_DEEP_ANALYSIS'];
    this.advancedCost = ra?.costPoints ?? 1000;
    if (w.blocked) {
      this.state = 'blocked';
    } else if (!w.firstFreeUsed) {
      this.state = 'free';
    } else if (w.balance < this.advancedCost) {
      this.state = 'insufficient';
    } else {
      this.state = 'advanced';
    }
  }

  /** Extrait un message lisible (évite le libellé Spring générique « No message available »). */
  private extractHttpError(e: unknown): string {
    const NO_MSG = 'no message available';
    const x = e as {
      error?: unknown;
      message?: string;
      status?: number;
      statusText?: string;
    };

    const isGarbage = (s: string) => {
      const t = s.trim().toLowerCase();
      return !t || t === NO_MSG || t.includes('http failure response for') || t.includes('unknown error');
    };

    const st = x?.status;
    // Status 0 = API not reachable / CORS / gateway down. Prioritize this over generic Angular message.
    if (st === 0) {
      return "Service coaching indisponible (gateway/microservice arrêté ou CORS). Démarrez API Gateway (8078) et Subcontracting, puis réessayez.";
    }

    if (typeof x?.error === 'string') {
      const s = x.error.trim();
      if (!isGarbage(s)) return s;
    }

    if (x?.error && typeof x.error === 'object') {
      const o = x.error as Record<string, unknown>;
      const parts: string[] = [];
      const push = (v: unknown) => {
        if (v == null) return;
        const s = String(v).trim();
        if (s && !isGarbage(s)) parts.push(s);
      };
      push(o['message']);
      push(o['detail']);
      push(o['title']);
      if (typeof o['error'] === 'string') push(o['error']);
      if (parts.length) return [...new Set(parts)].join(' — ');
    }

    if (x?.message && !isGarbage(x.message)) return x.message;

    if (st === 401) return 'Non authentifié : reconnectez-vous.';
    if (st === 403) return 'Accès refusé (session ou droits).';
    if (st === 404) return 'Ressource introuvable (URL gateway ou service).';
    if (st === 409) return 'Conflit : cette opération ne peut pas être appliquée (données déjà utilisées).';
    if (st === 400) return 'Requête refusée : vérifiez les paramètres.';
    if (st === 502 || st === 503) return 'Service temporairement indisponible (gateway ou microservice arrêté).';
    if (st === 500) {
      return 'Erreur serveur (500). Consultez les logs du microservice Subcontracting — souvent une contrainte SQL ou le JSON insight.';
    }
    return '';
  }

  private buildRequest(): CoachInsightRequest {
    const s = this.selected;
    let durationDays: number | undefined;
    if (s?.startDate && s?.deadline) {
      const a = new Date(s.startDate).getTime();
      const b = new Date(s.deadline).getTime();
      if (!isNaN(a) && !isNaN(b) && b >= a) {
        durationDays = Math.ceil((b - a) / (86400000));
      }
    }
    return {
      subcontractId: s?.id ?? null,
      scope: s?.scope ?? null,
      category: s?.category ?? null,
      budget: s?.budget ?? null,
      durationDays,
      subcontractorId: s?.subcontractorId ?? null,
      featureCode: 'RISK_DEEP_ANALYSIS',
    };
  }

  runInitial(): void {
    if (!this.hasSelectedContext()) {
      this.actionError = "Sélectionnez d'abord une sous-traitance pour lancer l'analyse.";
      this.actionSuccess = '';
      return;
    }
    this.actionError = '';
    this.actionSuccess = '';
    this.busy = true;
    this.coachApi.postInitialInsight(this.userId, this.buildRequest()).subscribe({
      next: (r) => {
        this.insight = r;
        this.syncWalletAfterAction();
        this.busy = false;
      },
      error: (e) => {
        this.busy = false;
        if (e?.status === 409) {
          this.actionError = "L'analyse gratuite a déjà été utilisée.";
          this.syncWalletAfterAction();
        } else {
          this.actionError =
            this.extractHttpError(e) || `Échec de l'analyse (${e?.status ?? '?'}) — vérifiez la console réseau.`;
        }
      },
    });
  }

  runAdvanced(): void {
    if (!this.hasSelectedContext()) {
      this.actionError = "Sélectionnez d'abord une sous-traitance pour lancer l'analyse avancée.";
      this.actionSuccess = '';
      return;
    }
    this.actionError = '';
    this.actionSuccess = '';
    this.busy = true;
    this.insufficient = null;
    this.coachApi.postAdvancedInsight(this.userId, this.buildRequest()).subscribe({
      next: (r) => {
        this.insight = r;
        this.syncWalletAfterAction();
        this.busy = false;
      },
      error: (e) => {
        this.busy = false;
        if (e?.status === 400 && e?.error?.shortage != null) {
          this.insufficient = e.error as InsufficientPointsBody;
          this.state = 'insufficient';
          this.actionError = this.extractHttpError(e) || 'Solde insuffisant.';
        } else if (e?.status === 403) {
          this.actionError = 'Accès coaching suspendu.';
          this.syncWalletAfterAction();
        } else {
          this.actionError = this.extractHttpError(e) || 'Échec analyse avancée.';
        }
      },
    });
  }

  sendRechargeRequest(): void {
    const suggested = this.insufficient?.requiredPoints ?? this.advancedCost;
    this.busy = true;
    this.actionError = '';
    this.actionSuccess = '';
    this.coachApi.postRechargeRequest(this.userId, { suggestedPoints: suggested, message: this.rechargeMsg }).subscribe({
      next: () => {
        this.busy = false;
        this.rechargeMsg = '';
        this.actionError = '';
        this.actionSuccess = 'Demande envoyée aux administrateurs.';
      },
      error: (e) => {
        this.busy = false;
        this.actionError = this.extractHttpError(e) || "Impossible d'envoyer la demande.";
        this.actionSuccess = '';
      },
    });
  }

  riskLevelClass(level: string | undefined): string {
    const l = (level || '').toUpperCase();
    if (l === 'CRITICAL') return 'risk-crit';
    if (l === 'HIGH') return 'risk-high';
    if (l === 'MEDIUM') return 'risk-med';
    return 'risk-low';
  }

  priorityClass(p: string | undefined): string {
    const x = (p || '').toUpperCase();
    if (x === 'URGENT') return 'pri-urgent';
    if (x === 'THIS_WEEK') return 'pri-week';
    return 'pri-opt';
  }

  approxAdvancedCount(): number {
    const b = this.wallet?.balance ?? 0;
    if (this.advancedCost <= 0) return 0;
    return Math.floor(b / this.advancedCost);
  }

  dismissActionError(): void {
    this.actionError = '';
  }

  dismissActionSuccess(): void {
    this.actionSuccess = '';
  }

  hasSelectedContext(): boolean {
    return !!this.selected?.id;
  }

  whatIfEntries(): Array<{ label: string; value: string }> {
    const w = this.insight?.whatIf;
    if (!w || typeof w !== 'object') return [];
    const o = w as Record<string, unknown>;
    return Object.entries(o).map(([k, v]) => ({
      label: this.prettyWhatIfKey(k),
      value: this.prettyWhatIfValue(v),
    }));
  }

  private prettyWhatIfKey(key: string): string {
    const map: Record<string, string> = {
      riskDelta: 'Variation de risque',
      note: 'Note IA',
      scenario: 'Scénario simulé',
      newEstimatedScore: 'Nouveau score estimé',
      confidence: 'Confiance',
    };
    if (map[key]) return map[key];
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/_/g, ' ')
      .trim()
      .replace(/^./, (c) => c.toUpperCase());
  }

  private prettyWhatIfValue(v: unknown): string {
    if (v == null) return '—';
    if (typeof v === 'number' || typeof v === 'boolean') return String(v);
    if (typeof v === 'string') return v;
    return JSON.stringify(v);
  }

  freelancerActionPlan(): ActionPlanItem[] {
    const actions = this.insight?.actions ?? [];
    return actions.slice(0, 3).map((a, i) => ({
      title: a.title || `Action ${i + 1}`,
      detail: a.detail || 'Sans détail complémentaire.',
      deadline: this.deadlineLabel(a.priority, i),
      priority: a.priority || 'OPTIONAL',
    }));
  }

  private deadlineLabel(priority?: string, index = 0): string {
    const p = (priority || '').toUpperCase();
    if (p === 'URGENT') return 'À faire aujourd’hui';
    if (p === 'THIS_WEEK') return 'À planifier cette semaine';
    return index === 0 ? 'À traiter dès que possible' : 'À intégrer au prochain sprint';
  }

  freelancerReadyMessage(): string {
    const s = this.selected;
    const mission = s?.title || 'la mission';
    const first = this.insight?.actions?.[0];
    const second = this.insight?.actions?.[1];
    const risk = this.insight?.globalRisk?.score;
    const riskTxt = risk != null ? `${risk}/100` : 'élevé';

    return [
      `Bonjour,`,
      ``,
      `Suite à l’analyse IA de "${mission}", j’ai préparé un plan d’exécution immédiat.`,
      `Niveau de risque actuel : ${riskTxt}.`,
      ``,
      `Actions proposées :`,
      `1) ${first?.title || 'Clarifier le scope'} — ${first?.detail || 'définir des critères d’acceptation clairs.'}`,
      `2) ${second?.title || 'Sécuriser le planning'} — ${second?.detail || 'ajouter un point de contrôle intermédiaire.'}`,
      ``,
      `Si vous validez ces points, je lance l’exécution dès aujourd’hui avec suivi hebdomadaire.`,
      ``,
      `Cordialement,`,
      `Freelancer`,
    ].join('\n');
  }

  copyReadyMessage(): void {
    const text = this.freelancerReadyMessage();
    navigator.clipboard.writeText(text).then(
      () => {
        this.copiedPlanMessage = true;
        setTimeout(() => (this.copiedPlanMessage = false), 1800);
      },
      () => {
        this.actionError = 'Copie impossible. Veuillez copier manuellement le texte.';
      }
    );
  }
}
