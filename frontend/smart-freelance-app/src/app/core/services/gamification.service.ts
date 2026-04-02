import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Base path via API Gateway → Gamification microservice (StripPrefix removes `/gamification`). */
const GAMIFICATION_API = `${environment.apiGatewayUrl}/gamification/api`;

export type GamificationConditionType =
  | 'PROJECT_COMPLETED'
  | 'PROJECT_CREATED'
  | 'FIRST_PROJECT'
  | 'FAST_RESPONDER'
  | 'TOP_FREELANCER';

export interface GamificationAchievement {
  id: number;
  title: string;
  description: string;
  xpReward: number;
  conditionType: GamificationConditionType;
}

/** Body for POST `/api/achievements` (admin). Omit `id` for create. */
export interface GamificationAchievementCreatePayload {
  title: string;
  description: string;
  xpReward: number;
  conditionType: GamificationConditionType;
}

export const GAMIFICATION_CONDITION_OPTIONS: { value: GamificationConditionType; label: string }[] = [
  { value: 'PROJECT_COMPLETED', label: 'Project completed' },
  { value: 'PROJECT_CREATED', label: 'Project created' },
  { value: 'FIRST_PROJECT', label: 'First project' },
  { value: 'FAST_RESPONDER', label: 'Fast responder' },
  { value: 'TOP_FREELANCER', label: 'Top freelancer' },
];

export interface GamificationUserAchievement {
  id: number;
  userId: number;
  achievement: GamificationAchievement;
  unlockedAt: string;
}

export interface GamificationUserLevel {
  id?: number;
  userId?: number;
  xp?: number;
  level?: number;
  fastResponderStreak?: number;
  /** Jackson may serialize Lombok `isTopFreelancer` as `topFreelancer`. */
  topFreelancer?: boolean;
  isTopFreelancer?: boolean;
}

@Injectable({ providedIn: 'root' })
export class GamificationService {
  constructor(private readonly http: HttpClient) {}

  /** All achievement definitions (catalog). */
  getAchievements(): Observable<GamificationAchievement[]> {
    return this.http.get<GamificationAchievement[]>(`${GAMIFICATION_API}/achievements`).pipe(
      catchError(() => of([])),
      map((list) => (Array.isArray(list) ? list : []))
    );
  }

  /** Same as {@link getAchievements} but propagates HTTP errors (for admin screens). */
  getAchievementsStrict(): Observable<GamificationAchievement[]> {
    return this.http.get<GamificationAchievement[]>(`${GAMIFICATION_API}/achievements`).pipe(
      map((list) => (Array.isArray(list) ? list : []))
    );
  }

  getUserAchievements(userId: number): Observable<GamificationUserAchievement[]> {
    return this.http.get<GamificationUserAchievement[]>(`${GAMIFICATION_API}/user-achievements/${userId}`).pipe(
      catchError(() => of([])),
      map((list) => (Array.isArray(list) ? list : []))
    );
  }

  /** Backend creates a default row if none exists. */
  getUserLevel(userId: number): Observable<GamificationUserLevel | null> {
    return this.http.get<GamificationUserLevel>(`${GAMIFICATION_API}/user-level/${userId}`).pipe(
      catchError(() => of(null))
    );
  }

  /**
   * Create a catalog achievement. Requires ADMIN (backend `@PreAuthorize`).
   * Errors (401/403/validation) are propagated to the caller.
   */
  createAchievement(payload: GamificationAchievementCreatePayload): Observable<GamificationAchievement> {
    return this.http.post<GamificationAchievement>(`${GAMIFICATION_API}/achievements`, payload);
  }
}

export function isTopFreelancerFlag(level: GamificationUserLevel | null | undefined): boolean {
  if (!level) return false;
  return level.topFreelancer === true || level.isTopFreelancer === true;
}

/** XP progress within the current 100-XP tier (matches backend level = xp/100 + 1). */
export function xpProgressInCurrentTier(xp: number): number {
  const safe = Math.max(0, xp);
  return safe % 100;
}
