import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';

const REQUEST_TIMEOUT_MS = 15_000;
const APP_API = `${environment.apiGatewayUrl}/freelancia-job/job-applications`;

export interface JobApplication {
  id?: number;
  jobId: number;
  jobTitle?: string;
  freelancerId: number;
  proposalMessage?: string;
  expectedRate?: number;
  availabilityStart?: string;
  status?: 'PENDING' | 'SHORTLISTED' | 'REJECTED' | 'ACCEPTED' | 'WITHDRAWN';
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class JobApplicationService {
  constructor(private http: HttpClient) {}

  addApplication(app: Partial<JobApplication>): Observable<JobApplication | null> {
    return this.http.post<JobApplication>(`${APP_API}/add`, app).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  updateApplication(id: number, app: Partial<JobApplication>): Observable<JobApplication | null> {
    return this.http.put<JobApplication>(`${APP_API}/update/${id}`, app).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  deleteApplication(id: number): Observable<boolean> {
    return this.http.delete(`${APP_API}/${id}`, { observe: 'response' }).pipe(
      map(res => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  getApplicationById(id: number): Observable<JobApplication | null> {
    return this.http.get<JobApplication>(`${APP_API}/${id}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  getAllApplications(): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>(`${APP_API}/list`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  getApplicationsByJob(jobId: number): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>(`${APP_API}/job/${jobId}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  getApplicationsByFreelancer(freelancerId: number): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>(`${APP_API}/freelancer/${freelancerId}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  updateStatus(id: number, value: string): Observable<JobApplication | null> {
    return this.http.patch<JobApplication>(
      `${APP_API}/${id}/status`,
      null,
      { params: { value } }
    ).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }
}
