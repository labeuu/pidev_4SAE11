import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import {
  GamificationAchievement,
  GamificationService,
  GamificationUserAchievement,
  GamificationUserLevel,
  isTopFreelancerFlag,
  xpProgressInCurrentTier,
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

  level: GamificationUserLevel | null = null;
  catalog: GamificationAchievement[] = [];
  unlocked: GamificationUserAchievement[] = [];
  private unlockedIds = new Set<number>();

  ngOnInit(): void {
    const directId = this.auth.getUserId();
    if (directId != null) {
      this.userId = directId;
      this.loadAll(directId);
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
      level: this.gamification.getUserLevel(uid).pipe(catchError(() => of(null))),
      catalog: this.gamification.getAchievements().pipe(catchError(() => of([]))),
      unlocked: this.gamification.getUserAchievements(uid).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ level, catalog, unlocked }) => {
        this.level = level;
        this.catalog = [...(catalog ?? [])].sort((a, b) => (b.xpReward ?? 0) - (a.xpReward ?? 0));
        this.unlocked = unlocked ?? [];
        this.unlockedIds = new Set(
          this.unlocked.map((u) => u.achievement?.id).filter((id): id is number => id != null)
        );
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to load gamification data.';
      },
    });
  }

  isUnlocked(achievementId: number): boolean {
    return this.unlockedIds.has(achievementId);
  }

  unlockedEntry(achievementId: number): GamificationUserAchievement | undefined {
    return this.unlocked.find((u) => u.achievement?.id === achievementId);
  }

  xpInTier(): number {
    return xpProgressInCurrentTier(this.level?.xp ?? 0);
  }

  displayLevel(): number {
    const l = this.level?.level;
    if (l != null && l > 0) return l;
    const xp = this.level?.xp ?? 0;
    return Math.floor(xp / 100) + 1;
  }

  totalXp(): number {
    return this.level?.xp ?? 0;
  }

  streak(): number {
    return this.level?.fastResponderStreak ?? 0;
  }

  topFreelancer(): boolean {
    return isTopFreelancerFlag(this.level);
  }

  conditionLabel(t: string): string {
    const map: Record<string, string> = {
      PROJECT_COMPLETED: 'Project completed',
      PROJECT_CREATED: 'Project created',
      FIRST_PROJECT: 'First project',
      FAST_RESPONDER: 'Fast responder',
      TOP_FREELANCER: 'Top freelancer',
    };
    return map[t] ?? t;
  }
}
