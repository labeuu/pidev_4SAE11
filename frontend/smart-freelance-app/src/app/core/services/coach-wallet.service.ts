import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const BASE = `${environment.apiGatewayUrl}/subcontracting/api/coach`;

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CoachWalletDto {
  id: number;
  userId: number;
  balance: number;
  currency: string;
  blocked: boolean;
  firstFreeUsed: boolean;
  lowBalanceAlerted: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface WalletMeResponse {
  userId: number;
  balance: number;
  currency: string;
  blocked: boolean;
  firstFreeUsed: boolean;
  remainingAnalysesByFeature: Record<string, RemainingAnalysisDto>;
}

export interface RemainingAnalysisDto {
  featureCode: string;
  costPoints: number;
  canAfford: boolean;
  remainingCount: number;
}

export interface WalletTransactionDto {
  id: number;
  type: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  reason: string | null;
  featureUsed: string | null;
  performedByRole: string | null;
  createdAt: string;
}

export interface CoachInsightRequest {
  subcontractId?: number | null;
  scope?: string | null;
  category?: string | null;
  budget?: number | null;
  durationDays?: number | null;
  subcontractorId?: number | null;
  featureCode?: string | null;
}

export interface CoachInsightResponse {
  globalRisk?: { score: number; level: string; summary: string };
  causes?: { title: string; detail: string; impact: number }[];
  actions?: { title: string; detail: string; priority: string; expectedRiskReduction: number }[];
  expectedImpact?: {
    riskReductionIfApplied: number;
    newEstimatedScore: number;
    confidenceLevel: string;
  };
  professionalTip?: string;
  urgencyVerdict?: string;
  coachSignature?: string;
  free?: boolean;
  pointsSpent?: number | null;
  whatIf?: Record<string, unknown> | null;
}

export interface InsightHistoryEntry {
  id: number;
  userId: number;
  subcontractId: number | null;
  featureCode: string | null;
  free: boolean;
  pointsSpent: number;
  insightResult: unknown;
  createdAt: string;
}

export interface CoachFeatureCostDto {
  id: number;
  featureCode: string;
  label: string | null;
  costPoints: number;
  active: boolean;
  description?: string | null;
}

export interface InsufficientPointsBody {
  currentBalance: number;
  requiredPoints: number;
  shortage: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class CoachWalletService {
  constructor(private http: HttpClient) {}

  getMyWallet(userId: number): Observable<WalletMeResponse> {
    return this.http.get<WalletMeResponse>(`${BASE}/wallet/me`, { params: { userId: String(userId) } });
  }

  getMyTransactions(userId: number, page = 0, size = 20): Observable<SpringPage<WalletTransactionDto>> {
    const params = new HttpParams()
      .set('userId', String(userId))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<SpringPage<WalletTransactionDto>>(`${BASE}/wallet/me/transactions`, { params });
  }

  getRemainingAnalyses(userId: number): Observable<Record<string, RemainingAnalysisDto>> {
    return this.http.get<Record<string, RemainingAnalysisDto>>(`${BASE}/wallet/me/remaining-analyses`, {
      params: { userId: String(userId) },
    });
  }

  postRechargeRequest(userId: number, body?: { suggestedPoints?: number; message?: string }): Observable<void> {
    return this.http.post<void>(`${BASE}/wallet/me/recharge-request`, body ?? {}, { params: { userId: String(userId) } });
  }

  postInitialInsight(userId: number, req: CoachInsightRequest): Observable<CoachInsightResponse> {
    return this.http.post<CoachInsightResponse>(`${BASE}/insights/initial`, req, { params: { userId: String(userId) } });
  }

  postAdvancedInsight(userId: number, req: CoachInsightRequest): Observable<CoachInsightResponse> {
    return this.http.post<CoachInsightResponse>(`${BASE}/insights/advanced`, req, { params: { userId: String(userId) } });
  }

  getMyInsightHistory(userId: number, page = 0, size = 20): Observable<SpringPage<InsightHistoryEntry>> {
    const params = new HttpParams().set('userId', String(userId)).set('page', String(page)).set('size', String(size));
    return this.http.get<SpringPage<InsightHistoryEntry>>(`${BASE}/insights/history/me`, { params });
  }

  // —— Admin —— //

  adminListWallets(adminUserId: number, page = 0, size = 20): Observable<SpringPage<CoachWalletDto>> {
    const params = new HttpParams()
      .set('adminUserId', String(adminUserId))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<SpringPage<CoachWalletDto>>(`${BASE}/wallet/admin/all`, { params });
  }

  adminGetWallet(adminUserId: number, targetUserId: number): Observable<CoachWalletDto> {
    const params = new HttpParams().set('adminUserId', String(adminUserId));
    return this.http.get<CoachWalletDto>(`${BASE}/wallet/admin/${targetUserId}`, { params });
  }

  adminCredit(adminUserId: number, targetUserId: number, body: { amount: number; reason: string; adminNote?: string }): Observable<void> {
    const params = new HttpParams().set('adminUserId', String(adminUserId));
    return this.http.post<void>(`${BASE}/wallet/admin/${targetUserId}/credit`, body, { params });
  }

  adminDebit(adminUserId: number, targetUserId: number, body: { amount: number; reason: string }): Observable<void> {
    const params = new HttpParams().set('adminUserId', String(adminUserId));
    return this.http.post<void>(`${BASE}/wallet/admin/${targetUserId}/debit`, body, { params });
  }

  adminUserTransactions(adminUserId: number, targetUserId: number, page = 0, size = 50): Observable<SpringPage<WalletTransactionDto>> {
    const params = new HttpParams()
      .set('adminUserId', String(adminUserId))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<SpringPage<WalletTransactionDto>>(`${BASE}/wallet/admin/${targetUserId}/transactions`, { params });
  }

  adminAuditLog(adminUserId: number, page = 0, size = 50): Observable<SpringPage<WalletTransactionDto>> {
    const params = new HttpParams()
      .set('adminUserId', String(adminUserId))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<SpringPage<WalletTransactionDto>>(`${BASE}/wallet/admin/audit-log`, { params });
  }

  adminSetBlocked(adminUserId: number, targetUserId: number, blocked: boolean): Observable<void> {
    const params = new HttpParams().set('adminUserId', String(adminUserId)).set('blocked', String(blocked));
    return this.http.patch<void>(`${BASE}/wallet/admin/${targetUserId}/block`, null, { params });
  }

  listFeatureCosts(): Observable<CoachFeatureCostDto[]> {
    return this.http.get<CoachFeatureCostDto[]>(`${BASE}/feature-costs`);
  }

  adminInsightHistory(adminUserId: number, targetUserId: number, page = 0, size = 50): Observable<SpringPage<InsightHistoryEntry>> {
    const params = new HttpParams()
      .set('adminUserId', String(adminUserId))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<SpringPage<InsightHistoryEntry>>(`${BASE}/insights/history/${targetUserId}`, { params });
  }
}
