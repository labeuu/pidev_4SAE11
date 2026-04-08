import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Meeting,
  CreateMeetingRequest,
  UpdateMeetingRequest,
  StatusUpdateRequest,
  MeetingStatus,
} from '../models/meeting.models';

const API = `${environment.apiGatewayUrl}/meeting/api/meetings`;

@Injectable({ providedIn: 'root' })
export class MeetingService {
  constructor(private http: HttpClient) {}

  getMyMeetings(): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(API).pipe(catchError(() => of([])));
  }

  getById(id: number): Observable<Meeting | null> {
    return this.http.get<Meeting>(`${API}/${id}`).pipe(catchError(() => of(null)));
  }

  getUpcoming(): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${API}/upcoming`).pipe(catchError(() => of([])));
  }

  getByStatus(status: MeetingStatus): Observable<Meeting[]> {
    return this.http
      .get<Meeting[]>(`${API}/by-status`, { params: { status } })
      .pipe(catchError(() => of([])));
  }

  create(req: CreateMeetingRequest): Observable<Meeting> {
    return this.http.post<Meeting>(API, req);
  }

  update(id: number, req: UpdateMeetingRequest): Observable<Meeting> {
    return this.http.put<Meeting>(`${API}/${id}`, req);
  }

  updateStatus(id: number, req: StatusUpdateRequest): Observable<Meeting> {
    return this.http.patch<Meeting>(`${API}/${id}/status`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }

  accept(id: number): Observable<Meeting> {
    return this.updateStatus(id, { status: 'ACCEPTED' });
  }

  decline(id: number, reason?: string): Observable<Meeting> {
    return this.updateStatus(id, { status: 'DECLINED', reason });
  }

  cancel(id: number, reason?: string): Observable<Meeting> {
    return this.updateStatus(id, { status: 'CANCELLED', reason });
  }

  complete(id: number): Observable<Meeting> {
    return this.updateStatus(id, { status: 'COMPLETED' });
  }
}
