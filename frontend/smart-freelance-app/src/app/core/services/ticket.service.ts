import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
  createdAt: string;
  lastActivityAt: string;
}

export interface CreateTicketRequest {
  subject: string;
}

export interface UpdateTicketRequest {
  subject?: string | null;
  priority?: TicketPriority | null;
}

@Injectable({ providedIn: 'root' })
export class TicketService {
  constructor(private http: HttpClient) {}

  create(body: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${TICKET_API}/tickets`, body);
  }

  getAll(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${TICKET_API}/tickets`);
  }

  getById(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${TICKET_API}/tickets/${id}`);
  }

  getByUserId(userId: number): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${TICKET_API}/tickets/user/${userId}`);
  }

  update(id: number, body: UpdateTicketRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}`, body);
  }

  close(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}/close`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${TICKET_API}/tickets/${id}`);
  }
}
