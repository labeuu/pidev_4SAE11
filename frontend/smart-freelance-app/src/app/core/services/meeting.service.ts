import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Meeting,
  MeetingComment,
  MeetingStats,
  MeetingTranscript,
  MeetingSummary,
  ProjectDto,
  ContractDto,
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

  getStats(): Observable<MeetingStats> {
    return this.http.get<MeetingStats>(`${API}/stats`).pipe(
      catchError(() => of({ total: 0, pending: 0, accepted: 0, declined: 0, cancelled: 0, completed: 0 }))
    );
  }

  // ── Transcript & AI Summary ───────────────────────────────────────────────

  saveTranscript(meetingId: number, content: string): Observable<MeetingTranscript> {
    return this.http.post<MeetingTranscript>(`${API}/${meetingId}/transcript`, { content });
  }

  getTranscripts(meetingId: number): Observable<MeetingTranscript[]> {
    return this.http.get<MeetingTranscript[]>(`${API}/${meetingId}/transcripts`).pipe(catchError(() => of([])));
  }

  generateSummary(meetingId: number): Observable<MeetingSummary> {
    return this.http.post<MeetingSummary>(`${API}/${meetingId}/summarize`, {});
  }

  getSummary(meetingId: number): Observable<MeetingSummary | null> {
    return this.http.get<MeetingSummary>(`${API}/${meetingId}/summary`).pipe(catchError(() => of(null)));
  }

  // ── Projects & Contracts (for schedule form dropdowns) ────────────────────

  getMyProjects(): Observable<ProjectDto[]> {
    return this.http.get<ProjectDto[]>(`${API}/my-projects`).pipe(catchError(() => of([])));
  }

  getMyContracts(): Observable<ContractDto[]> {
    return this.http.get<ContractDto[]>(`${API}/my-contracts`).pipe(catchError(() => of([])));
  }

  // ── Comments ──────────────────────────────────────────────────────────────

  getComments(meetingId: number): Observable<MeetingComment[]> {
    return this.http.get<MeetingComment[]>(`${API}/${meetingId}/comments`).pipe(catchError(() => of([])));
  }

  addComment(meetingId: number, userId: number, userName: string, content: string): Observable<MeetingComment> {
    return this.http.post<MeetingComment>(`${API}/${meetingId}/comments`, { userId, userName, content });
  }

  updateComment(commentId: number, content: string): Observable<MeetingComment> {
    return this.http.put<MeetingComment>(`${API}/comments/${commentId}`, { content });
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${API}/comments/${commentId}`);
  }
}
