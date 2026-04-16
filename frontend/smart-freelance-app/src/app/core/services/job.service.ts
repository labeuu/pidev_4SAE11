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

/** Mirrors JobSearchRequest DTO on the backend. */
export interface JobSearchRequest {
  keyword?: string;
  clientId?: number;
  status?: string;
  clientType?: string;
  locationType?: string;
  category?: string;
  budgetMin?: number;
  budgetMax?: number;
  skillIds?: number[];
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

/** Spring Data Page<JobResponse> serialised shape. */
export interface JobPage {
  content: Job[];
  totalElements: number;
  totalPages: number;
  number: number;   // current page (0-based)
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface GeneratedJobDraft {
  title: string;
  description: string;
  requiredSkills: string[];
  budgetMin: number | null;
  budgetMax: number | null;
  currency: string;
  estimatedDurationWeeks: number | null;
  category: string;
  locationType: string;
}

export interface JobStats {
  jobId: number;
  jobTitle: string;
  applicationsCount: number;
}

export interface FitScoreResult {
  score: number;
  tier: 'STRONG_MATCH' | 'GOOD_MATCH' | 'PARTIAL_MATCH' | 'LOW_MATCH';
  summary: string;
  matchedSkills: string[];
  missingSkills: string[];
  recommendations: string[];
}

export interface JobAdminStats {
  totalJobs: number;
  avgApplicationsPerJob: number;
  uniqueFreelancers: number;
  jobsByStatus: Record<string, number>;
  top5Jobs: JobStats[];
  jobsPerMonth: { month: string; count: number }[];
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
    return this.http.post<Job>(`${JOB_API}/add`, job).pipe(timeout(REQUEST_TIMEOUT_MS));
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

  generateJobDraft(prompt: string): Observable<GeneratedJobDraft | null> {
    return this.http.post<GeneratedJobDraft>(`${JOB_API}/generate`, { prompt }).pipe(timeout(30_000));
  }

  /** Server-side filter + pagination via POST /jobs/filter */
  filterJobs(request: JobSearchRequest): Observable<JobPage> {
    return this.http.post<JobPage>(`${JOB_API}/filter`, request).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of({
        content: [], totalElements: 0, totalPages: 0, number: 0,
        size: request.size ?? 9, first: true, last: true, empty: true
      }))
    );
  }

  getApplicationStats(): Observable<JobStats[]> {
    return this.http.get<JobStats[]>(`${JOB_API}/application-stats`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([]))
    );
  }

  getFitScore(jobId: number, freelancerId: number): Observable<FitScoreResult | null> {
    return this.http.get<FitScoreResult>(`${JOB_API}/${jobId}/fit-score`, {
      params: { freelancerId }
    }).pipe(
      timeout(60_000),
      catchError(() => of(null))
    );
  }

  getAdminStats(): Observable<JobAdminStats | null> {
    return this.http
      .get<JobAdminStats>(`${environment.apiGatewayUrl}/freelancia-job/api/admin/job-stats`)
      .pipe(timeout(REQUEST_TIMEOUT_MS), catchError(() => of(null)));
  }
}
