import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const TICKET_API = `${environment.apiGatewayUrl}/ticket`;

export type ReplySender = 'USER' | 'ADMIN';

export interface TicketReply {
  id: number;
  ticketId: number;
  message: string;
  sender: ReplySender;
  authorUserId: number;
  createdAt: string;
}

export interface CreateReplyRequest {
  ticketId: number;
  message: string;
}

export interface UpdateReplyRequest {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ReplyService {
  constructor(private http: HttpClient) {}

  create(body: CreateReplyRequest): Observable<TicketReply | null> {
    return this.http.post<TicketReply>(`${TICKET_API}/replies`, body).pipe(catchError(() => of(null)));
  }

  getByTicketId(ticketId: number): Observable<TicketReply[]> {
    return this.http.get<TicketReply[]>(`${TICKET_API}/replies/${ticketId}`).pipe(catchError(() => of([])));
  }

  update(id: number, body: UpdateReplyRequest): Observable<TicketReply | null> {
    return this.http.put<TicketReply>(`${TICKET_API}/replies/${id}`, body).pipe(catchError(() => of(null)));
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete<void>(`${TICKET_API}/replies/${id}`).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }
}

