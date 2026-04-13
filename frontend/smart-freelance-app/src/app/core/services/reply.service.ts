import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
  readByUser?: boolean;
  readByAdmin?: boolean;
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

  create(body: CreateReplyRequest): Observable<TicketReply> {
    return this.http.post<TicketReply>(`${TICKET_API}/replies`, body);
  }

  getByTicketId(ticketId: number): Observable<TicketReply[]> {
    return this.http.get<TicketReply[]>(`${TICKET_API}/replies/${ticketId}`);
  }

  update(id: number, body: UpdateReplyRequest): Observable<TicketReply> {
    return this.http.put<TicketReply>(`${TICKET_API}/replies/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${TICKET_API}/replies/${id}`);
  }
}
