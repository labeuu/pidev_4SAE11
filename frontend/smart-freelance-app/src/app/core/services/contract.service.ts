import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Domain types ────────────────────────────────────────────────────────────
export type ContractStatus =
  | 'DRAFT'
  | 'PENDING_SIGNATURE'
  | 'ACTIVE'
  | 'IN_CONFLICT'
  | 'COMPLETED'
  | 'CANCELLED';

export type ConflictStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED';

export interface ContractConflict {
  id?: number;
  raisedById?: number;
  reason: string;
  description: string;
  evidenceUrl?: string;
  status?: ConflictStatus;
  createdAt?: string;
  resolvedAt?: string;
  resolution?: string;
}

export interface ConflictComment {
  id?: number;
  conflictId?: number;
  userId: number;
  content: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ContractStats {
  total: number;
  draft: number;
  pendingSignature: number;
  active: number;
  completed: number;
  cancelled: number;
  inConflict: number;
}

export interface ConflictStats {
  total: number;
  open: number;
  inReview: number;
  resolved: number;
}

export interface Contract {
  id?: number;
  clientId: number;
  freelancerId: number;
  projectApplicationId?: number;
  offerApplicationId?: number;
  title: string;
  description?: string;
  terms: string;
  amount: number;
  startDate: string;
  endDate: string;
  status?: ContractStatus;
  clientSignatureUrl?: string;
  freelancerSignatureUrl?: string;
  signedAt?: string;
  createdAt?: string;
  conflicts?: ContractConflict[];
}

// ── Service ─────────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly base = `${environment.apiGatewayUrl}/contract/api`;

  constructor(private http: HttpClient) {}

  // ── Contracts ────────────────────────────────────────────────────────────

  getAll(): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.base}/contracts`);
  }

  getByClient(clientId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.base}/contracts/client/${clientId}`);
  }

  getByFreelancer(freelancerId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.base}/contracts/freelancer/${freelancerId}`);
  }

  getById(id: number): Observable<Contract> {
    return this.http.get<Contract>(`${this.base}/contracts/${id}`);
  }

  create(data: Omit<Contract, 'id' | 'status' | 'signedAt' | 'createdAt' | 'conflicts'>): Observable<Contract> {
    return this.http.post<Contract>(`${this.base}/contracts`, data);
  }

  update(id: number, data: Partial<Contract>): Observable<Contract> {
    return this.http.put<Contract>(`${this.base}/contracts/${id}`, data);
  }

  /**
   * Upload a signature image (Base64 data-URL) for this contract.
   * Sends a JSON body so large image data isn't URL-encoded in query params.
   * Auto-activates the contract when both parties have signed.
   */
  sign(id: number, role: 'CLIENT' | 'FREELANCER', signatureData: string): Observable<Contract> {
    return this.http.patch<Contract>(
      `${this.base}/contracts/${id}/sign`,
      { role, signatureData }
    );
  }

  /** Update the contract status independently (submit, cancel, complete, etc.). */
  updateStatus(id: number, status: ContractStatus): Observable<Contract> {
    return this.http.patch<Contract>(
      `${this.base}/contracts/${id}/status`,
      null,
      { params: { status } }
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/contracts/${id}`);
  }

  exportPdf(id: number, includeAttachments = false, clientName?: string | null, freelancerName?: string | null): Observable<Blob> {
    return this.http.post(
      `${this.base}/contracts/${id}/export-pdf`,
      { includeAttachments, clientName, freelancerName },
      { responseType: 'blob' }
    );
  }

  // ── Conflicts ────────────────────────────────────────────────────────────

  getConflicts(contractId: number): Observable<ContractConflict[]> {
    return this.http.get<ContractConflict[]>(`${this.base}/conflicts/contract/${contractId}`);
  }

  reportConflict(contractId: number, payload: Pick<ContractConflict, 'reason' | 'description' | 'evidenceUrl'> & { raisedById: number }): Observable<ContractConflict> {
    return this.http.post<ContractConflict>(`${this.base}/conflicts/contract/${contractId}`, payload);
  }

  // ── Statistics ───────────────────────────────────────────────────────────

  getContractStats(): Observable<ContractStats> {
    return this.http.get<ContractStats>(`${this.base}/contracts/stats`);
  }

  getConflictStats(): Observable<ConflictStats> {
    return this.http.get<ConflictStats>(`${this.base}/conflicts/stats`);
  }

  // ── Comments ─────────────────────────────────────────────────────────────

  getComments(conflictId: number): Observable<ConflictComment[]> {
    return this.http.get<ConflictComment[]>(`${this.base}/comments/conflict/${conflictId}`);
  }

  addComment(conflictId: number, userId: number, content: string): Observable<ConflictComment> {
    return this.http.post<ConflictComment>(`${this.base}/comments/conflict/${conflictId}`, { userId, content });
  }

  updateComment(id: number, content: string): Observable<ConflictComment> {
    return this.http.put<ConflictComment>(`${this.base}/comments/${id}`, { content });
  }

  deleteComment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/comments/${id}`);
  }
}
