import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Response from AImodel GET /api/ai/status */
export interface AiModelLiveStatus {
  service: string;
  status: string;
  /** Ollama reachable flag (kept for backward-compatible payload shape). */
  ollamaReachable: boolean;
  model: string;
  modelReady: boolean;
}

export interface AiModelLiveStatusPoll {
  snapshot: AiModelLiveStatus | null;
  /** True when the browser could not reach the AI service via the gateway */
  reachabilityError: boolean;
}

@Injectable({ providedIn: 'root' })
export class AiModelStatusService {
  constructor(private readonly http: HttpClient) {}

  private statusUrl(): string {
    return `${environment.apiGatewayUrl}/aimodel/api/ai/status`;
  }

  /**
   * Fast probe (short timeout) for UI. Does not block on slow generation.
   */
  getLiveStatus(): Observable<AiModelLiveStatusPoll> {
    const url = this.statusUrl();
    return this.http.get<AiModelLiveStatus>(url).pipe(
      timeout(8000),
      map((snapshot) => ({ snapshot, reachabilityError: false })),
      catchError(() => of({ snapshot: null, reachabilityError: true }))
    );
  }
}
