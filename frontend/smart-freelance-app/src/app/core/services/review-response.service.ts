import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const RESPONSE_API = `${environment.apiGatewayUrl}/review/api/review-responses`;

/** Backend may send respondedAt as ISO string or as array [y,m,d,h,min,s] (Java LocalDateTime). */
export interface ReviewResponseItem {
  id?: number;
  reviewId: number;
  respondentId: number;
  message: string;
  respondedAt?: string | number[];
}

@Injectable({ providedIn: 'root' })
export class ReviewResponseService {
  constructor(private http: HttpClient) {}

  getByReviewId(reviewId: number): Observable<ReviewResponseItem[]> {
    return this.http.get<ReviewResponseItem[]>(`${RESPONSE_API}/review/${reviewId}`).pipe(
      catchError(() => of([]))
    );
  }

  create(response: { reviewId: number; respondentId: number; message: string }): Observable<ReviewResponseItem | null> {
    return this.http.post<ReviewResponseItem>(RESPONSE_API, response).pipe(
      catchError(() => of(null))
    );
  }

  update(id: number, message: string): Observable<ReviewResponseItem | null> {
    const body = { message: String(message).trim() };
    return this.http.put<ReviewResponseItem>(`${RESPONSE_API}/${id}`, body, {
      headers: { 'Content-Type': 'application/json' },
      responseType: 'json',
    }).pipe(
      catchError(() => of(null))
    );
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete(`${RESPONSE_API}/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status === 204),
      catchError(() => of(false))
    );
  }
}
