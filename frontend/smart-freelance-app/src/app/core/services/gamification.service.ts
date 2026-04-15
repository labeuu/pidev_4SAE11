import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Base path via API Gateway → Gamification microservice (StripPrefix removes `/gamification`). */
const GAMIFICATION_API = `${environment.apiGatewayUrl}/gamification/api`;

export type GamificationConditionType =
  | 'PROJECT_COMPLETED'
  | 'PROJECT_CREATED'
  | 'FIRST_PROJECT'
  | 'FAST_RESPONDER'
  | 'TOP_FREELANCER'
  | 'REVIEW_GIVEN'
  | 'STREAK_DAYS'
  | 'XP_REACHED';

/** 🆕 Who can receive this achievement */
export type GamificationTargetRole = 'FREELANCER' | 'CLIENT' | 'ALL';

export const GAMIFICATION_TARGET_ROLE_OPTIONS = [
  { value: 'FREELANCER', label: 'Freelancers only' },
  { value: 'CLIENT', label: 'Clients only' },
  { value: 'ALL', label: 'All users' }
];

export interface GamificationAchievement {
  id: number;
  title: string;
  description: string;
  xpReward: number;
  conditionType: GamificationConditionType;
  iconEmoji?: string;
  conditionThreshold?: number;
  targetRole?: GamificationTargetRole; // 🆕
}

/** Body for POST `/api/achievements` (admin). Omit `id` for create. */
export interface GamificationAchievementCreatePayload {
  title: string;
  description: string;
  xpReward: number;
  conditionType: GamificationConditionType;
  iconEmoji: string;
  conditionThreshold: number;
  targetRole: GamificationTargetRole; // 🆕
}

export const GAMIFICATION_CONDITION_OPTIONS: { value: GamificationConditionType; label: string }[] = [
  { value: 'PROJECT_COMPLETED', label: 'Project completed' },
  { value: 'PROJECT_CREATED', label: 'Project created' },
  { value: 'FIRST_PROJECT', label: 'First project' },
  { value: 'FAST_RESPONDER', label: 'Fast responder' },
  { value: 'TOP_FREELANCER', label: 'Top freelancer' },
  { value: 'REVIEW_GIVEN', label: 'Review given' },
  { value: 'STREAK_DAYS', label: 'Streak days' },
  { value: 'XP_REACHED', label: 'XP reached' },
];

export interface GamificationUserAchievement {
  id: number;
  userId: number;
  achievement: GamificationAchievement;
  unlockedAt: string;
}

export interface AchievementProgress {
  achievementId: number;
  title: string;
  description: string;
  iconEmoji: string;
  conditionType: string;
  targetRole: string; // 🆕
  currentValue: number;
  targetValue: number;
  progressPercent: number;
  xpReward: number;
  unlocked: boolean;
  unlockedAt?: string;
}

export interface UserLevelSummary {
  userId: number;
  xp: number;
  level: number;
  xpInCurrentTier: number;
  xpToNextLevel: number;
  xpRemaining: number;
  progressPercent: number;
  isTopFreelancer: boolean;
  fastResponderStreak: number;
  activeStreak: number; // 🆕
}

export interface LeaderboardEntry {
  rank: number;
  userId: number;
  fullName: string; // 🆕
  xp: number;
  level: number;
  isTopFreelancer: boolean;
  fastResponderStreak: number;
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

export interface GamificationRecommendation {
  message: string;
  priority: number; // 1 = High, 2 = Medium, 3 = Low
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

  /** 🆕 Fetch real-time progress for a user. */
  getUserProgress(userId: number): Observable<AchievementProgress[]> {
    return this.http.get<AchievementProgress[]>(`${GAMIFICATION_API}/user-achievements/${userId}/progress`).pipe(
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

  /** 🆕 Get enriched summary with progress bars metrics. */
  getUserLevelSummary(userId: number): Observable<UserLevelSummary | null> {
    return this.http.get<UserLevelSummary>(`${GAMIFICATION_API}/user-level/${userId}/summary`).pipe(
      catchError(() => of(null))
    );
  }

  /** 🆕 Get global leaderboard. */
  getLeaderboard(top: number = 10): Observable<LeaderboardEntry[]> {
    const params = new HttpParams().set('top', top.toString());
    return this.http.get<LeaderboardEntry[]>(`${GAMIFICATION_API}/user-level/leaderboard`, { params }).pipe(
      catchError(() => of([])),
      map((list) => (Array.isArray(list) ? list : []))
    );
  }

  /**
   * Create a catalog achievement. Requires ADMIN (backend `@PreAuthorize`).
   */
  createAchievement(payload: GamificationAchievementCreatePayload): Observable<GamificationAchievement> {
    return this.http.post<GamificationAchievement>(`${GAMIFICATION_API}/achievements`, payload);
  }

  /** 🆕 Update a catalog achievement. */
  updateAchievement(id: number, payload: GamificationAchievementCreatePayload): Observable<GamificationAchievement> {
    return this.http.put<GamificationAchievement>(`${GAMIFICATION_API}/achievements/${id}`, payload);
  }

  /** 🆕 Delete a catalog achievement. */
  deleteAchievement(id: number): Observable<void> {
    return this.http.delete<void>(`${GAMIFICATION_API}/achievements/${id}`);
  }

  /** 🆕 Fetch smart recommendations for a user. */
  getRecommendations(userId: number): Observable<GamificationRecommendation[]> {
    return this.http.get<GamificationRecommendation[]>(`${GAMIFICATION_API}/recommendations/${userId}`).pipe(
      catchError(() => of([])),
      map((list) => (Array.isArray(list) ? list : []))
    );
  }
}

export function isTopFreelancerFlag(level: GamificationUserLevel | UserLevelSummary | null | undefined): boolean {
  if (!level) return false;
  return (level as any).topFreelancer === true || (level as any).isTopFreelancer === true;
}
