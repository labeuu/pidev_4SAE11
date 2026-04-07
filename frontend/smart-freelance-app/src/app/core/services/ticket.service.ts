import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
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

  create(body: CreateTicketRequest): Observable<Ticket | null> {
    return this.http.post<Ticket>(`${TICKET_API}/tickets`, body).pipe(catchError(() => of(null)));
  }

  getAll(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${TICKET_API}/tickets`).pipe(catchError(() => of([])));
  }

  getById(id: number): Observable<Ticket | null> {
    return this.http.get<Ticket>(`${TICKET_API}/tickets/${id}`).pipe(catchError(() => of(null)));
  }

  getByUserId(userId: number): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${TICKET_API}/tickets/user/${userId}`).pipe(catchError(() => of([])));
  }

  update(id: number, body: UpdateTicketRequest): Observable<Ticket | null> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}`, body).pipe(catchError(() => of(null)));
  }

  close(id: number): Observable<Ticket | null> {
    return this.http.put<Ticket>(`${TICKET_API}/tickets/${id}/close`, {}).pipe(catchError(() => of(null)));
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete<void>(`${TICKET_API}/tickets/${id}`).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }
}

