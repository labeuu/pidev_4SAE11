import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const TICKET_API = `${environment.apiGatewayUrl}/ticket`;

export type TicketStatus = 'OPEN' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Ticket {
  id: number;
  userId: number;
  subject: string;
  status: TicketStatus;
  priority: TicketPriority;
  assignedAdminId?: number | null;
  assignedAdminName?: string | null;
  assignedAt?: string | null;
  createdAt: string;
  lastActivityAt: string;
  firstResponseAt?: string | null;
  resolvedAt?: string | null;
  responseTimeMinutes?: number | null;
  reopenCount?: number;
  canReopen?: boolean;
}

export interface CreateTicketRequest {
  subject: string;
}

export interface UpdateTicketRequest {
  subject?: string | null;
  priority?: TicketPriority | null;
}

export interface TicketStats {
  total: number;
  open: number;
  closed: number;
  averageResponseTimeMinutes: number | null;
}

export interface MonthlyTicketCount {
  year: number;
  month: number;
  count: number;
}

export interface TicketUnreadCountEntry {
  ticketId: number;
  unreadCount: number;
}

export interface TicketListQuery {
  priority?: TicketPriority;
  status?: TicketStatus;
  q?: string;
  sortBy?: 'createdAt' | 'updatedAt' | 'lastActivityAt' | 'priority';
  sortDir?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export interface TicketPageResponse {
  items: Ticket[];
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class TicketService {
  constructor(private readonly http: HttpClient) {}

  private buildQueryParams(query?: TicketListQuery): HttpParams {
    let params = new HttpParams();
    if (!query) return params;
    if (query.priority) params = params.set('priority', query.priority);
    if (query.status) params = params.set('status', query.status);
    if (query.q) params = params.set('q', query.q);
    if (query.sortBy) params = params.set('sortBy', query.sortBy);
    if (query.sortDir) params = params.set('sortDir', query.sortDir);
    if (query.page != null) params = params.set('page', String(query.page));
    if (query.size != null) params = params.set('size', String(query.size));
    return params;
  }

  create(body: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${TICKET_API}/tickets`, body);
  }

  getAll(query?: TicketListQuery): Observable<TicketPageResponse> {
    return this.http.get<TicketPageResponse>(`${TICKET_API}/tickets`, {
      params: this.buildQueryParams(query),
    });
  }

  getById(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${TICKET_API}/tickets/${id}`);
  }

  getByUserId(userId: number, query?: TicketListQuery): Observable<TicketPageResponse> {
    return this.http.get<TicketPageResponse>(`${TICKET_API}/tickets/user/${userId}`, {
      params: this.buildQueryParams(query),
    });
  }

  update(id: number, body: UpdateTicketRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}`, body);
  }

  close(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}/close`, {});
  }

  assign(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}/assign`, {});
  }

  unassign(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}/unassign`, {});
  }

  reopen(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}/reopen`, {});
  }

  markRead(id: number): Observable<void> {
    return this.http.put<void>(`${TICKET_API}/tickets/${id}/read`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${TICKET_API}/tickets/${id}`);
  }

  getUnreadCounts(): Observable<TicketUnreadCountEntry[]> {
    return this.http.get<TicketUnreadCountEntry[]>(`${TICKET_API}/tickets/unread-counts`);
  }

  getStats(): Observable<TicketStats> {
    return this.http.get<TicketStats>(`${TICKET_API}/tickets/stats`);
  }

  getMonthlyStats(): Observable<MonthlyTicketCount[]> {
    return this.http.get<MonthlyTicketCount[]>(`${TICKET_API}/tickets/stats/monthly`);
  }

  exportPdf(): Observable<Blob> {
    return this.http.get(`${TICKET_API}/tickets/export/pdf`, { responseType: 'blob' });
  }
}
