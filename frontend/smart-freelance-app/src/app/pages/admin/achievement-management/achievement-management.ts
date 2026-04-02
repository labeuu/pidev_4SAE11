import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  GamificationAchievement,
  GamificationService,
  GamificationAchievementCreatePayload,
  GamificationConditionType,
  GAMIFICATION_CONDITION_OPTIONS,
} from '../../../core/services/gamification.service';

@Component({
  selector: 'app-admin-achievement-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './achievement-management.html',
  styleUrl: './achievement-management.scss',
})
export class AdminAchievementManagement implements OnInit {
  achievements: GamificationAchievement[] = [];
  loading = false;
  listError: string | null = null;

  readonly conditionOptions = GAMIFICATION_CONDITION_OPTIONS;

  formTitle = '';
  formDescription = '';
  formXpReward = 50;
  formConditionType: GamificationConditionType = 'PROJECT_COMPLETED';
  formError: string | null = null;
  saving = false;
  successMessage: string | null = null;

  constructor(
    private readonly gamification: GamificationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAchievements();
  }

  loadAchievements(): void {
    this.loading = true;
    this.listError = null;
    this.gamification.getAchievementsStrict().subscribe({
      next: (list) => {
        this.achievements = [...(list ?? [])].sort((a, b) => (a.id ?? 0) - (b.id ?? 0));
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.listError = 'Could not load achievements. Check the API gateway and Project service.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitCreate(): void {
    this.formError = null;
    this.successMessage = null;
    const title = (this.formTitle || '').trim();
    const description = (this.formDescription || '').trim();
    if (!title) {
      this.formError = 'Title is required.';
      return;
    }
    if (!description) {
      this.formError = 'Description is required.';
      return;
    }
    const xp = Number(this.formXpReward);
    if (!Number.isFinite(xp) || xp < 0) {
      this.formError = 'XP reward must be zero or a positive number.';
      return;
    }

    const payload: GamificationAchievementCreatePayload = {
      title,
      description,
      xpReward: Math.floor(xp),
      conditionType: this.formConditionType,
    };

    this.saving = true;
    this.gamification.createAchievement(payload).subscribe({
      next: () => {
        this.successMessage = 'Achievement created.';
        this.formTitle = '';
        this.formDescription = '';
        this.formXpReward = 50;
        this.formConditionType = 'PROJECT_COMPLETED';
        this.saving = false;
        this.loadAchievements();
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        this.saving = false;
        this.formError = this.formatCreateError(err);
        this.cdr.detectChanges();
      },
    });
  }

  private formatCreateError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 401) return 'You are not signed in, or your session expired.';
      if (err.status === 403) return 'Only administrators can create achievements.';
      if (err.status === 0) return 'Network error. Is the API gateway reachable?';
      const body = err.error;
      if (typeof body === 'string' && body.trim()) return body;
      if (body && typeof body === 'object' && 'message' in body && typeof (body as { message: string }).message === 'string') {
        return (body as { message: string }).message;
      }
    }
    return 'Could not create achievement.';
  }

  conditionLabel(t: GamificationConditionType): string {
    return this.conditionOptions.find((o) => o.value === t)?.label ?? t;
  }
}
