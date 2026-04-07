import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const VENDOR_API = `${environment.apiGatewayUrl}/vendor/api/vendors`;

export type VendorApprovalStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'SUSPENDED'
  | 'EXPIRED';

export interface VendorApproval {
  id: number;
  organizationId: number;
  freelancerId: number;
  status: VendorApprovalStatus;
  domain: string;
  validFrom: string;
  validUntil: string;
  nextReviewDate: string;
  approvedBy: number;
  approvalNotes: string;
  rejectionReason: string;
  suspensionReason: string;
  reviewCount: number;
  createdAt: string;
  updatedAt: string;
  statusChangedAt: string;
  isActive: boolean;
  isReviewOverdue: boolean;
  isReviewUpcoming: boolean;
  clientSignedAt: string | null;
  clientSignerName: string | null;
  freelancerSignedAt: string | null;
  freelancerSignerName: string | null;
  fullySigned: boolean;
  /** MÉTIER 6 — Secteur métier (ex. IT, Design). */
  professionalSector?: string | null;
  /** MÉTIER 5 — Rappel d’expiration déjà envoyé. */
  expiryReminderSentAt?: string | null;
  /** Jours avant fin de validité (calculé côté API). */
  daysUntilValidUntilExpiry?: number | null;
}

export interface VendorSignatureRequest {
  signerUserId: number;
  fullName: string;
}

export interface VendorApprovalRequest {
  organizationId: number;
  freelancerId: number;
  domain?: string;
  /** MÉTIER 6 — Secteur métier (optionnel). */
  professionalSector?: string;
  notes?: string;
}

/** MÉTIER 4 — entrées du journal d'audit (workflow + révisions + système). */
export interface VendorAuditEntry {
  id: number;
  vendorApprovalId: number;
  fromStatus: VendorApprovalStatus | null;
  toStatus: VendorApprovalStatus;
  action: string;
  actorUserId: number | null;
  detail: string;
  createdAt: string;
}

/** MÉTIER 3 — éligibilité détaillée (offres B2B / domaine requis). */
export interface EligibilityDetail {
  eligible: boolean;
  reasonCode: string;
  message: string;
}

/** Synthèse admin : avis client → freelancer + projets communs (aide à la décision agrément). */
export interface VendorDecisionInsight {
  vendorApprovalId: number;
  organizationId: number;
  freelancerId: number;
  clientDisplayName: string;
  freelancerDisplayName: string;
  agreementDomain: string | null;
  professionalSector: string | null;
  status: string;
  sharedProjectCount: number;
  sharedProjects: Array<{
    id: number;
    title: string;
    status: string;
    category: string;
  }>;
  reviewCount: number;
  averageRatingFromClient: number;
  ratingDistribution: Record<string, number>;
  reviews: Array<{
    id: number;
    projectId: number;
    rating: number;
    comment: string;
    createdAt: string;
  }>;
  dataWarnings: string[];
}

@Injectable({ providedIn: 'root' })
export class VendorService {

  constructor(private http: HttpClient) {}

  // ── CRUD ──────────────────────────────────────

  getAll(): Observable<VendorApproval[]> {
    return this.http.get<VendorApproval[]>(VENDOR_API).pipe(
      catchError(err => { console.error('[VendorService] getAll error:', err); return of([]); })
    );
  }

  getById(id: number): Observable<VendorApproval> {
    return this.http.get<VendorApproval>(`${VENDOR_API}/${id}`);
  }

  getByFreelancer(freelancerId: number): Observable<VendorApproval[]> {
    return this.http.get<VendorApproval[]>(`${VENDOR_API}/freelancer/${freelancerId}`).pipe(
      catchError(err => { console.error('[VendorService] getByFreelancer error:', err); return of([]); })
    );
  }

  getByOrganization(orgId: number): Observable<VendorApproval[]> {
    return this.http.get<VendorApproval[]>(`${VENDOR_API}/organization/${orgId}`).pipe(
      catchError(err => { console.error('[VendorService] getByOrganization error:', err); return of([]); })
    );
  }

  create(req: VendorApprovalRequest): Observable<VendorApproval> {
    return this.http.post<VendorApproval>(VENDOR_API, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${VENDOR_API}/${id}`);
  }

  // ── WORKFLOW (Métier 1) ───────────────────────

  approve(id: number, adminId: number, notes?: string): Observable<VendorApproval> {
    let params = new HttpParams().set('adminId', adminId);
    if (notes) params = params.set('notes', notes);
    return this.http.patch<VendorApproval>(`${VENDOR_API}/${id}/approve`, null, { params });
  }

  reject(id: number, reason: string): Observable<VendorApproval> {
    const params = new HttpParams().set('reason', reason);
    return this.http.patch<VendorApproval>(`${VENDOR_API}/${id}/reject`, null, { params });
  }

  suspend(id: number, reason: string): Observable<VendorApproval> {
    const params = new HttpParams().set('reason', reason);
    return this.http.patch<VendorApproval>(`${VENDOR_API}/${id}/suspend`, null, { params });
  }

  resubmit(id: number): Observable<VendorApproval> {
    return this.http.patch<VendorApproval>(`${VENDOR_API}/${id}/resubmit`, null);
  }

  signAsClient(id: number, body: VendorSignatureRequest): Observable<VendorApproval> {
    return this.http.post<VendorApproval>(`${VENDOR_API}/${id}/sign/client`, body);
  }

  signAsFreelancer(id: number, body: VendorSignatureRequest): Observable<VendorApproval> {
    return this.http.post<VendorApproval>(`${VENDOR_API}/${id}/sign/freelancer`, body);
  }

  // ── RÉVISION (Métier 2) ───────────────────────

  renew(id: number, adminId: number): Observable<VendorApproval> {
    const params = new HttpParams().set('adminId', adminId);
    return this.http.patch<VendorApproval>(`${VENDOR_API}/${id}/renew`, null, { params });
  }

  expireOutdated(): Observable<{ expiredCount: number }> {
    return this.http.post<{ expiredCount: number }>(`${VENDOR_API}/expire-outdated`, null);
  }

  getReviewsDue(): Observable<VendorApproval[]> {
    return this.http.get<VendorApproval[]>(`${VENDOR_API}/reviews-due`).pipe(
      catchError(err => { console.error('[VendorService] getReviewsDue error:', err); return of([]); })
    );
  }

  getReviewsOverdue(): Observable<VendorApproval[]> {
    return this.http.get<VendorApproval[]>(`${VENDOR_API}/reviews-overdue`).pipe(
      catchError(err => { console.error('[VendorService] getReviewsOverdue error:', err); return of([]); })
    );
  }

  /** MÉTIER 5 — expire dans les N prochains jours (validUntil). */
  getExpiringSoon(days = 30): Observable<VendorApproval[]> {
    const params = new HttpParams().set('days', String(days));
    return this.http.get<VendorApproval[]>(`${VENDOR_API}/expiring-soon`, { params }).pipe(
      catchError(err => { console.error('[VendorService] getExpiringSoon error:', err); return of([]); })
    );
  }

  /** MÉTIER 5 — envoie les rappels J-30 (anti-doublon côté serveur). */
  sendExpiryReminders(): Observable<{ remindersSent: number }> {
    return this.http.post<{ remindersSent: number }>(`${VENDOR_API}/send-expiry-reminders`, null);
  }

  // ── ÉLIGIBILITÉ (Métier 3) ────────────────────

  checkEligibility(organizationId: number, freelancerId: number): Observable<{ eligible: boolean }> {
    const params = new HttpParams()
      .set('organizationId', organizationId)
      .set('freelancerId', freelancerId);
    return this.http.get<{ eligible: boolean }>(`${VENDOR_API}/eligibility`, { params });
  }

  getEligibilityDetail(organizationId: number, freelancerId: number, domain?: string): Observable<EligibilityDetail> {
    let params = new HttpParams()
      .set('organizationId', organizationId)
      .set('freelancerId', freelancerId);
    if (domain != null && domain !== '') {
      params = params.set('domain', domain);
    }
    return this.http.get<EligibilityDetail>(`${VENDOR_API}/eligibility/detail`, { params });
  }

  getAuditHistory(vendorApprovalId: number): Observable<VendorAuditEntry[]> {
    return this.http.get<VendorAuditEntry[]>(`${VENDOR_API}/${vendorApprovalId}/audit-history`).pipe(
      catchError(err => { console.error('[VendorService] getAuditHistory error:', err); return of([]); })
    );
  }

  // ── STATS (Métier 4) ──────────────────────────

  statsOrganization(orgId: number): Observable<{ approvedCount: number }> {
    return this.http.get<{ approvedCount: number }>(`${VENDOR_API}/stats/organization/${orgId}`);
  }

  statsFreelancer(fId: number): Observable<{ approvedCount: number }> {
    return this.http.get<{ approvedCount: number }>(`${VENDOR_API}/stats/freelancer/${fId}`);
  }

  /** Rapport décision (JSON) — avis + projets liés. */
  getDecisionInsight(vendorApprovalId: number): Observable<VendorDecisionInsight> {
    return this.http.get<VendorDecisionInsight>(`${VENDOR_API}/${vendorApprovalId}/decision-insight`);
  }

  /** Rapport PDF pour l’admin. */
  downloadDecisionInsightPdf(vendorApprovalId: number): Observable<Blob> {
    return this.http.get(`${VENDOR_API}/${vendorApprovalId}/decision-insight/pdf`, { responseType: 'blob' });
  }
}
