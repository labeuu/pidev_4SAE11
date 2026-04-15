import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import {
  AchievementProgress,
  GamificationService,
  UserLevelSummary,
  LeaderboardEntry,
  GamificationRecommendation, // 🆕
  isTopFreelancerFlag
} from '../../../core/services/gamification.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-gamification',
  standalone: true,
  imports: [CommonModule, RouterLink, Card],
  templateUrl: './gamification.html',
  styleUrl: './gamification.scss',
})
export class GamificationPage implements OnInit {
  private auth = inject(AuthService);
  private userService = inject(UserService);
  private gamification = inject(GamificationService);

  loading = true;
  errorMessage: string | null = null;
  userId: number | null = null;

  summary: UserLevelSummary | null = null;
  progressList: AchievementProgress[] = [];
  leaderboard: LeaderboardEntry[] = [];
  recommendations: GamificationRecommendation[] = []; // 🆕

  ngOnInit(): void {
    const directId = this.auth.getUserId();
    if (directId != null) {
      this.userId = Number(directId);
      this.loadAll(this.userId);
      return;
    }
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'Sign in to view your progress and achievements.';
      return;
    }
    this.userService.getByEmail(email).subscribe({
      next: (u) => {
        if (u?.id != null) {
          this.userId = u.id;
          this.loadAll(u.id);
        } else {
          this.loading = false;
          this.errorMessage = 'Could not resolve your user profile.';
        }
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
      },
    });
  }

  private loadAll(uid: number): void {
    this.loading = true;
    this.errorMessage = null;
    forkJoin({
      summary: this.gamification.getUserLevelSummary(uid).pipe(catchError(() => of(null))),
      progress: this.gamification.getUserProgress(uid).pipe(catchError(() => of([]))),
      leaderboard: this.gamification.getLeaderboard(10).pipe(catchError(() => of([]))),
      recommendations: this.gamification.getRecommendations(uid).pipe(catchError(() => of([]))), // 🆕
    }).subscribe({
      next: ({ summary, progress, leaderboard, recommendations }) => {
        this.summary = summary;
        this.leaderboard = leaderboard;
        this.recommendations = recommendations; // 🆕
        const userRole = (this.auth.getUserRole() || '').toUpperCase();
        console.log('🛡️ GAMIFICATION DEBUG: Detected User Role =', userRole);
        
        // Filter by role (Robust & Case-Insensitive)
        const filtered = (progress ?? []).filter(p => {
          const target = (p.targetRole || 'ALL').toUpperCase();
          console.log(`🔍 Item [${p.title}] Target [${target}]`);
          
          if (userRole === 'CLIENT') {
            return target === 'CLIENT' || target === 'ALL';
          }
          if (userRole === 'FREELANCER') {
            return target === 'FREELANCER' || target === 'ALL';
          }
          // Admin or Unknown?
          return target === 'ALL' || userRole === 'ADMIN'; 
        });

        console.log('✅ Final Count Visible =', filtered.length);

        // Sort: unlocked first, then highest percent
        this.progressList = filtered.sort((a, b) => {
          if (a.unlocked !== b.unlocked) return a.unlocked ? -1 : 1;
          return b.progressPercent - a.progressPercent;
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to load gamification data.';
      },
    });
  }

  xpInTier(): number {
    return this.summary?.xpInCurrentTier ?? 0;
  }

  totalXpNeeded(): number {
    return this.summary?.xpToNextLevel ?? 100;
  }

  progressPercent(): number {
    return this.summary?.progressPercent ?? 0;
  }

  displayLevel(): number {
    return this.summary?.level ?? 1;
  }

  totalXp(): number {
    return this.summary?.xp ?? 0;
  }

  streak(): number {
    return this.summary?.fastResponderStreak ?? 0;
  }

  topFreelancer(): boolean {
    return isTopFreelancerFlag(this.summary);
  }

  // 🆕 Détermine si un badge doit être affiché selon le rôle de l'utilisateur
  shouldShowAchievement(targetRole: string): boolean {
    const userRole = (this.auth.getUserRole() || '').toUpperCase();
    const target = (targetRole || 'ALL').toUpperCase();

    if (userRole === 'CLIENT') {
      return target === 'CLIENT' || target === 'ALL';
    }
    if (userRole === 'FREELANCER') {
      return target === 'FREELANCER' || target === 'ALL';
    }
    return target === 'ALL' || userRole === 'ADMIN'; 
  }

  conditionLabel(t: string): string {
    const map: Record<string, string> = {
      PROJECT_COMPLETED: 'Projects completed',
      PROJECT_CREATED: 'Projects posted',
      FIRST_PROJECT: 'Initial project',
      FAST_RESPONDER: 'Speed streak',
      TOP_FREELANCER: 'Global elite',
      REVIEW_GIVEN: 'Community reviews',
      STREAK_DAYS: 'Consecutive activity',
      XP_REACHED: 'Growth milestone'
    };
    return map[t] ?? t.replace('_', ' ').toLowerCase();
  }
}
