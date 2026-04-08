import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const API = `${environment.apiGatewayUrl}/subcontracting/api/subcontracts`;

export type SubcontractStatus = 'DRAFT' | 'PROPOSED' | 'ACCEPTED' | 'REJECTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'CLOSED';
export type SubcontractCategory = 'DEVELOPMENT' | 'DESIGN' | 'TESTING' | 'CONTENT' | 'CONSULTING';
export type DeliverableStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface Subcontract {
  id: number;
  mainFreelancerId: number;
  mainFreelancerName: string;
  subcontractorId: number;
  subcontractorName: string;
  projectId: number;
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
  createdAt: string;
  updatedAt: string;
  statusChangedAt: string;
  totalDeliverables: number;
  approvedDeliverables: number;
  pendingDeliverables: number;
  deliverables: Deliverable[];
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

export interface SubcontractRequest {
  subcontractorId: number;
  projectId: number;
  contractId?: number;
  title: string;
  scope?: string;
  category: string;
  budget?: number;
  currency?: string;
  startDate?: string;
  deadline?: string;
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
}
