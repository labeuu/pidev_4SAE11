import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const API = `${environment.apiGatewayUrl}/subcontracting/api/subcontracts`;

export type SubcontractStatus =
  | 'DRAFT'
  | 'PROPOSED'
  | 'COUNTER_OFFERED'
  | 'AI_MEDIATION'
  | 'NEGOTIATED'
  | 'NEGOTIATION_IMPASSE'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'CLOSED';
export type SubcontractCategory = 'DEVELOPMENT' | 'DESIGN' | 'TESTING' | 'CONTENT' | 'CONSULTING';
export type DeliverableStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface Subcontract {
  id: number;
  mainFreelancerId: number;
  mainFreelancerName: string;
  subcontractorId: number;
  subcontractorName: string;
  projectId: number | null;
  offerId?: number | null;
  projectTitle: string;
  contractId: number | null;
  title: string;
  scope: string;
  category: SubcontractCategory;
  budget: number;
  currency: string;
  status: SubcontractStatus;
  startDate: string;
  deadline: string;
  rejectionReason: string | null;
  cancellationReason: string | null;
  negotiationRoundCount?: number | null;
  negotiationStatus?: string | null;
  createdAt: string;
  updatedAt: string;
  statusChangedAt: string;
  totalDeliverables: number;
  approvedDeliverables: number;
  pendingDeliverables: number;
  deliverables: Deliverable[];
  requiredSkills?: string[];
  mediaUrl?: string | null;
  mediaType?: 'VIDEO' | 'AUDIO' | string | null;
}

export interface Deliverable {
  id: number;
  subcontractId: number;
  title: string;
  description: string;
  status: DeliverableStatus;
  deadline: string;
  submissionUrl: string | null;
  submissionNote: string | null;
  submittedAt: string | null;
  reviewNote: string | null;
  reviewedAt: string | null;
  createdAt: string;
  overdue: boolean;
}

export interface SubcontractMessage {
  id: number;
  subcontractId: number;
  senderUserId: number;
  senderName: string;
  message: string;
  createdAt: string;
}

export interface SubcontractRequest {
  subcontractorId: number;
  projectId?: number;
  offerId?: number;
  contractId?: number;
  title: string;
  scope?: string;
  category: string;
  budget?: number;
  currency?: string;
  startDate?: string;
  deadline?: string;
  requiredSkills?: string[];
  mediaUrl?: string;
  mediaType?: string;
}

export interface CounterOfferRequest {
  proposedBudget: number;
  proposedDurationDays: number;
  note?: string;
}

export interface AiMediateRequest {
  note?: string;
}

export interface NegotiationOfferPosition {
  budget: number;
  durationDays: number;
}

export interface NegotiationRoundResponse {
  primaryOffer: NegotiationOfferPosition | null;
  subcontractorOffer: NegotiationOfferPosition | null;
  aiCompromise: NegotiationOfferPosition | null;
  compromiseJustification: string | null;
  roundNumber: number;
  negotiationStatus: string;
}

export interface MediaUploadResponse {
  mediaUrl: string;
  mediaType: string;
}

export interface DeliverableRequest {
  title: string;
  description?: string;
  deadline?: string;
}

export interface SubcontractorScore {
  subcontractorId: number;
  subcontractorName: string;
  score: number;
  label: string;
  totalSubcontracts: number;
  completedSubcontracts: number;
  totalDeliverables: number;
  approvedDeliverables: number;
  overdueDeliverables: number;
  completionRate: number;
  onTimeRate: number;
  breakdown: string[];
}

export interface SubcontractDashboard {
  totalSubcontracts: number;
  byStatus: Record<string, number>;
  byCategory: Record<string, number>;
  totalDeliverables: number;
  approvedDeliverables: number;
  pendingDeliverables: number;
  overdueDeliverables: number;
  avgDeliverablesPerSubcontract: number;
  globalCompletionRate: number;
  alerts: string[];
}

export interface AuditTimelineEntry {
  id: number;
  subcontractId: number;
  subcontractTitle: string;
  action: string;
  actionLabel: string;
  fromStatus: string | null;
  toStatus: string | null;
  detail: string;
  targetEntity: string | null;
  targetEntityId: number | null;
  actorUserId: number;
  actorName: string;
  createdAt: string;
  icon: string;
  color: string;
}

export interface FreelancerHistory {
  userId: number;
  userName: string;
  totalEvents: number;
  eventsByAction: Record<string, number>;
  asMainFreelancer: number;
  asSubcontractor: number;
  timeline: AuditTimelineEntry[];
}

export type MatchRecommendation = 'HIGHLY_RECOMMENDED' | 'RECOMMENDED' | 'POSSIBLE';

export interface SubcontractMatchCandidate {
  freelancerId: number;
  fullName: string;
  email: string;
  matchScore: number;
  matchReasons: string[];
  trustScore: number;
  previousCollaborations: number;
  recommendation: MatchRecommendation | string;
}

export interface SubcontractMatchResponse {
  candidates: SubcontractMatchCandidate[];
}

export type FinancialVerdict = 'EXCELLENT_CHOICE' | 'GOOD_CHOICE' | 'RISKY' | 'NOT_RECOMMENDED';

export interface MissionFinanceRow {
  subcontractId: number;
  title: string;
  budget: number | null;
  status: string;
  hadLateDeliverables: boolean | null;
}

export interface PaymentHistorySummary {
  pastMissionsConsidered: number;
  missionsWithLateDeliverables: number;
  cancelledOrRejectedMissions: number;
  refundLikeEvents: number;
  recentMissions: MissionFinanceRow[];
}

export interface FinancialTimelineEntry {
  date: string;
  label: string;
  amount: number | null;
}

/** Réponse GET .../ai/financial-analysis — analyse BI + IA */
export interface SubcontractFinancialAnalysisResponse {
  rentabilityScore: number;
  verdict: FinancialVerdict | string;
  marginRate: number | null;
  estimatedRoi: number | null;
  breakEvenThreshold: number | null;
  recommendations: string[];
  principalContractBudget: number | null;
  subcontractBudget: number | null;
  remainingMarginForPrincipal: number | null;
  subcontractToPrincipalRatioPercent: number | null;
  currency: string;
  paymentHistorySummary: PaymentHistorySummary;
  financialTimeline: FinancialTimelineEntry[];
  otherSubcontractsOnContractTotal: number | null;
}

export interface SubcontractRiskCockpitRequest {
  mainFreelancerId: number;
  subcontractorId?: number;
  projectId?: number;
  offerId?: number;
  budget?: number;
  startDate?: string;
  deadline?: string;
  scope?: string;
  requiredSkills?: string[];
}

export interface RiskGauge {
  key: string;
  label: string;
  score: number;
  level: string;
  explanation: string;
}

export interface RiskRecommendationAction {
  type: 'ADJUST_BUDGET' | 'ADJUST_DURATION' | 'REFINE_SCOPE' | 'OPEN_ALTERNATIVES' | 'NO_OP' | string;
  budgetMultiplier?: number;
  durationDeltaDays?: number;
  suggestedSubcontractorId?: number;
  scopeHint?: string;
}

export interface RiskRecommendation {
  text: string;
  action: RiskRecommendationAction;
}

export interface RiskAlternative {
  label: string;
  score: number;
  changes: string;
}

export interface SubcontractRiskCockpitResponse {
  totalRiskScore: number;
  level: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | string;
  streamedNarrative: string;
  gauges: RiskGauge[];
  recommendations: RiskRecommendation[];
  alternatives: RiskAlternative[];
}

export interface SubcontractorInsight {
  name: string;
  profitabilityScore: number;
  riskScore: number;
  note: string;
}

export interface MonthInsight {
  month: string;
  score: number;
  rationale: string;
}

export interface RiskTrendPoint {
  month: string;
  avgRiskScore: number;
}

export interface PredictiveDashboardResponse {
  narrativeSummary: string;
  successRateByCategory: Record<string, number>;
  topProfitableSubcontractors: SubcontractorInsight[];
  topRiskySubcontractors: SubcontractorInsight[];
  bestMonthsForSubcontracting: MonthInsight[];
  riskTrend: RiskTrendPoint[];
  nextIncidentPrediction: string;
  monthlyReportHint: string;
  generatedAt: string;
}

export interface MyCoachingProfileResponse {
  strengths: string[];
  weaknesses: string[];
  patterns: string[];
  personalizedTips: string[];
  progressScore: number;
}

@Injectable({ providedIn: 'root' })
export class SubcontractService {

  constructor(private http: HttpClient) {}

  // ── CRUD Subcontracts ─────────────────────────────

  getAll(): Observable<Subcontract[]> {
    return this.http.get<Subcontract[]>(API);
  }

  getById(id: number): Observable<Subcontract> {
    return this.http.get<Subcontract>(`${API}/${id}`);
  }

  getByFreelancer(id: number): Observable<Subcontract[]> {
    return this.http.get<Subcontract[]>(`${API}/freelancer/${id}`);
  }

  getBySubcontractor(id: number): Observable<Subcontract[]> {
    return this.http.get<Subcontract[]>(`${API}/subcontractor/${id}`);
  }

  getByProject(id: number): Observable<Subcontract[]> {
    return this.http.get<Subcontract[]>(`${API}/project/${id}`);
  }

  getByStatus(status: string): Observable<Subcontract[]> {
    return this.http.get<Subcontract[]>(`${API}/status/${status}`);
  }

  create(mainFreelancerId: number, req: SubcontractRequest): Observable<Subcontract> {
    const params = new HttpParams().set('mainFreelancerId', mainFreelancerId);
    return this.http.post<Subcontract>(API, req, { params });
  }

  update(id: number, req: SubcontractRequest): Observable<Subcontract> {
    return this.http.put<Subcontract>(`${API}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }

  // ── Workflow ──────────────────────────────────────

  propose(id: number): Observable<Subcontract> {
    return this.http.patch<Subcontract>(`${API}/${id}/propose`, null);
  }

  accept(id: number): Observable<Subcontract> {
    return this.http.patch<Subcontract>(`${API}/${id}/accept`, null);
  }

  counterOffer(id: number, subcontractorId: number, body: CounterOfferRequest): Observable<NegotiationRoundResponse> {
    const params = new HttpParams().set('subcontractorId', String(subcontractorId));
    return this.http.post<NegotiationRoundResponse>(`${API}/${id}/counter-offer`, body, { params });
  }

  aiMediate(id: number, mainFreelancerId: number, body?: AiMediateRequest): Observable<NegotiationRoundResponse> {
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.post<NegotiationRoundResponse>(`${API}/${id}/ai/mediate`, body ?? {}, { params });
  }

  reject(id: number, reason?: string): Observable<Subcontract> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.patch<Subcontract>(`${API}/${id}/reject`, null, { params });
  }

  startWork(id: number): Observable<Subcontract> {
    return this.http.patch<Subcontract>(`${API}/${id}/start`, null);
  }

  complete(id: number): Observable<Subcontract> {
    return this.http.patch<Subcontract>(`${API}/${id}/complete`, null);
  }

  cancel(id: number, reason?: string): Observable<Subcontract> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.patch<Subcontract>(`${API}/${id}/cancel`, null, { params });
  }

  close(id: number): Observable<Subcontract> {
    return this.http.patch<Subcontract>(`${API}/${id}/close`, null);
  }

  reopen(id: number): Observable<Subcontract> {
    return this.http.patch<Subcontract>(`${API}/${id}/reopen`, null);
  }

  // ── Deliverables ──────────────────────────────────

  getDeliverables(subcontractId: number): Observable<Deliverable[]> {
    return this.http.get<Deliverable[]>(`${API}/${subcontractId}/deliverables`);
  }

  addDeliverable(subcontractId: number, req: DeliverableRequest): Observable<Deliverable> {
    return this.http.post<Deliverable>(`${API}/${subcontractId}/deliverables`, req);
  }

  updateDeliverable(subcontractId: number, deliverableId: number, req: DeliverableRequest): Observable<Deliverable> {
    return this.http.put<Deliverable>(`${API}/${subcontractId}/deliverables/${deliverableId}`, req);
  }

  deleteDeliverable(subcontractId: number, deliverableId: number): Observable<void> {
    return this.http.delete<void>(`${API}/${subcontractId}/deliverables/${deliverableId}`);
  }

  submitDeliverable(subcontractId: number, deliverableId: number, body: { submissionUrl?: string; submissionNote?: string }): Observable<Deliverable> {
    return this.http.patch<Deliverable>(`${API}/${subcontractId}/deliverables/${deliverableId}/submit`, body);
  }

  reviewDeliverable(subcontractId: number, deliverableId: number, body: { approved: boolean; reviewNote?: string }): Observable<Deliverable> {
    return this.http.patch<Deliverable>(`${API}/${subcontractId}/deliverables/${deliverableId}/review`, body);
  }

  getMessages(subcontractId: number, viewerUserId: number): Observable<SubcontractMessage[]> {
    const params = new HttpParams().set('viewerUserId', String(viewerUserId));
    return this.http.get<SubcontractMessage[]>(`${API}/${subcontractId}/messages`, { params });
  }

  sendMessage(subcontractId: number, senderUserId: number, message: string): Observable<SubcontractMessage> {
    const params = new HttpParams().set('senderUserId', String(senderUserId));
    return this.http.post<SubcontractMessage>(`${API}/${subcontractId}/messages`, { message }, { params });
  }

  // ── Métier 3 — Score ──────────────────────────────

  getScore(subcontractorId: number): Observable<SubcontractorScore> {
    return this.http.get<SubcontractorScore>(`${API}/score/${subcontractorId}`);
  }

  // ── Métier 4 — Dashboard ──────────────────────────

  getDashboard(): Observable<SubcontractDashboard> {
    return this.http.get<SubcontractDashboard>(`${API}/dashboard`);
  }

  // ── Métier 5 — Historique & Timeline ─────────────

  getSubcontractHistory(subcontractId: number): Observable<AuditTimelineEntry[]> {
    return this.http.get<AuditTimelineEntry[]>(`${API}/${subcontractId}/history`);
  }

  getFreelancerHistory(userId: number): Observable<FreelancerHistory> {
    return this.http.get<FreelancerHistory>(`${API}/history/freelancer/${userId}`);
  }

  /** Analyse financière IA (Claude) — agrégats + score / verdict / recommandations. */
  getFinancialAnalysis(subcontractId: number, mainFreelancerId: number): Observable<SubcontractFinancialAnalysisResponse> {
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.get<SubcontractFinancialAnalysisResponse>(
      `${API}/${subcontractId}/ai/financial-analysis`,
      { params }
    );
  }

  getRiskCockpit(body: SubcontractRiskCockpitRequest): Observable<SubcontractRiskCockpitResponse> {
    return this.http.post<SubcontractRiskCockpitResponse>(`${API}/ai/risk-cockpit`, body);
  }

  getPredictiveDashboard(mainFreelancerId: number): Observable<PredictiveDashboardResponse> {
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.get<PredictiveDashboardResponse>(`${API}/ai/predictive-dashboard`, { params });
  }

  getMyCoachingProfile(mainFreelancerId: number): Observable<MyCoachingProfileResponse> {
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.get<MyCoachingProfileResponse>(`${API}/ai/my-coaching-profile`, { params });
  }

  auditRiskSimulation(mainFreelancerId: number, payload: SubcontractRiskCockpitResponse): Observable<{ recorded: boolean }> {
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.post<{ recorded: boolean }>(`${API}/ai/risk-cockpit/simulate`, payload, { params });
  }

  confirmRisk(subcontractId: number, mainFreelancerId: number, body: { totalRiskScore: number; selectedAlternativeLabel?: string; summary?: string }): Observable<{ recorded: boolean }> {
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.post<{ recorded: boolean }>(`${API}/${subcontractId}/ai/risk-confirm`, body, { params });
  }

  /** Matching IA (Claude) — compétences requises → tous les candidats avec score (tri décroissant). */
  matchSubcontractor(mainFreelancerId: number, requiredSkills: string[]): Observable<SubcontractMatchResponse> {
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.post<SubcontractMatchResponse>(
      `${API}/ai/match-subcontractor`,
      { requiredSkills },
      { params }
    );
  }

  /** Upload présentation MP4 (vidéo) ou MP3 (audio). */
  uploadPresentationMedia(mainFreelancerId: number, file: File): Observable<MediaUploadResponse> {
    const fd = new FormData();
    fd.append('file', file, file.name);
    const params = new HttpParams().set('mainFreelancerId', String(mainFreelancerId));
    return this.http.post<MediaUploadResponse>(`${API}/media/upload`, fd, { params });
  }
}
