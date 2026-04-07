import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';

const REQUEST_TIMEOUT_MS = 15_000;
const JOB_API = `${environment.apiGatewayUrl}/freelancia-job/jobs`;

export interface Job {
  id?: number;
  clientId?: number;
  clientType?: 'INDIVIDUAL' | 'COMPANY';
  companyName?: string;
  title: string;
  description: string;
  budgetMin?: number;
  budgetMax?: number;
  currency?: string;
  deadline?: string;
  category?: string;
  locationType?: 'REMOTE' | 'ONSITE' | 'HYBRID';
  status?: 'OPEN' | 'IN_PROGRESS' | 'FILLED' | 'CANCELLED';
  requiredSkillIds?: number[];
  skills?: { id: number; name: string; domain: string }[];
  createdAt?: string;
  updatedAt?: string;
}

export interface JobFilters {
  keyword?: string;
  category?: string;
  budgetMin?: number;
  budgetMax?: number;
  locationType?: string;
  skillId?: number;
}

export interface JobStats {
  jobId: number;
  jobTitle: string;
  applicationsCount: number;
}

@Injectable({ providedIn: 'root' })
export class JobService {
  constructor(private http: HttpClient) {}

  getAllJobs(): Observable<Job[]> {
    return this.http.get<Job[]>(`${JOB_API}/list`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  getById(id: number): Observable<Job | null> {
    return this.http.get<Job>(`${JOB_API}/${id}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  getByClientId(clientId: number): Observable<Job[]> {
    return this.http.get<Job[]>(`${JOB_API}/client/${clientId}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  getRecommendedJobs(userId: number): Observable<Job[]> {
    return this.http.get<Job[]>(`${JOB_API}/recommended`, { params: { userId } }).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  searchJobs(filters: JobFilters): Observable<Job[]> {
    const params: any = {};
    if (filters.keyword) params['keyword'] = filters.keyword;
    if (filters.category) params['category'] = filters.category;
    if (filters.budgetMin != null) params['budgetMin'] = filters.budgetMin;
    if (filters.budgetMax != null) params['budgetMax'] = filters.budgetMax;
    if (filters.locationType) params['locationType'] = filters.locationType;
    if (filters.skillId != null) params['skillId'] = filters.skillId;
    return this.http.get<Job[]>(`${JOB_API}/search`, { params }).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  createJob(job: Partial<Job>): Observable<Job | null> {
    return this.http.post<Job>(`${JOB_API}/add`, job).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  updateJob(id: number, job: Partial<Job>): Observable<Job | null> {
    return this.http.put<Job>(`${JOB_API}/update/${id}`, job).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  deleteJob(id: number): Observable<boolean> {
    return this.http.delete(`${JOB_API}/${id}`, { observe: 'response' }).pipe(
      map(res => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  getJobStatistics(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${JOB_API}/statistics`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of({}))
    );
  }

  getApplicationStats(): Observable<JobStats[]> {
    return this.http.get<JobStats[]>(`${JOB_API}/application-stats`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }
}
