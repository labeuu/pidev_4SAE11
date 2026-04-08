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
  GAMIFICATION_TARGET_ROLE_OPTIONS,
  GamificationTargetRole,
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
  readonly targetRoleOptions = GAMIFICATION_TARGET_ROLE_OPTIONS; // 🆕

  // Form fields
  formTitle = '';
  formDescription = '';
  formXpReward = 50;
  formConditionType: GamificationConditionType = 'PROJECT_COMPLETED';
  formConditionThreshold = 1;
  formTargetRole: GamificationTargetRole = 'ALL'; // 🆕
  formIconEmoji = '🏅';

  // State
  formError: string | null = null;
  saving = false;
  successMessage: string | null = null;
  editMode = false;
  editingId: number | null = null;

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

  // --- CREATE / UPDATE ---
  
  submitForm(): void {
    if (this.editMode && this.editingId) {
      this.submitUpdate();
    } else {
      this.submitCreate();
    }
  }

  submitCreate(): void {
    const payload = this.getPayload();
    if (!payload) return;

    this.saving = true;
    this.gamification.createAchievement(payload).subscribe({
      next: () => {
        this.onSuccess('Achievement created.');
        this.resetForm();
      },
      error: (err: unknown) => {
        this.onError(err);
      },
    });
  }

  submitUpdate(): void {
    if (!this.editingId) return;
    const payload = this.getPayload();
    if (!payload) return;

    this.saving = true;
    // We reuse create service if backend handles "save" (upsert), 
    // otherwise we need an explicit update method in service.
    // Assuming we need to add update to service:
    this.gamification.updateAchievement(this.editingId, payload).subscribe({
      next: () => {
        this.onSuccess('Achievement updated.');
        this.cancelEdit();
      },
      error: (err: unknown) => {
        this.onError(err);
      },
    });
  }

  // --- EDIT MODE ---

  editAchievement(a: GamificationAchievement): void {
    this.editMode = true;
    this.editingId = a.id;
    this.formTitle = a.title;
    this.formDescription = a.description;
    this.formXpReward = a.xpReward;
    this.formConditionType = a.conditionType;
    this.formConditionThreshold = a.conditionThreshold ?? 1;
    this.formTargetRole = a.targetRole ?? 'ALL'; // 🆕
    this.formIconEmoji = a.iconEmoji ?? '🏅';
    this.formError = null;
    this.successMessage = null;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.editMode = false;
    this.editingId = null;
    this.resetForm();
  }

  // --- DELETE ---

  deleteAchievement(id: number): void {
    if (!confirm('Are you sure you want to delete this achievement?')) return;
    
    this.loading = true;
    this.gamification.deleteAchievement(id).subscribe({
      next: () => {
        this.successMessage = 'Achievement deleted.';
        this.loadAchievements();
      },
      error: (err) => {
        this.listError = 'Could not delete achievement.';
        this.loading = false;
      }
    });
  }

  // --- HELPERS ---

  private getPayload(): GamificationAchievementCreatePayload | null {
    this.formError = null;
    const title = (this.formTitle || '').trim();
    const description = (this.formDescription || '').trim();
    
    if (!title) { this.formError = 'Title is required.'; return null; }
    if (!description) { this.formError = 'Description is required.'; return null; }
    
    return {
      title,
      description,
      xpReward: Math.floor(Number(this.formXpReward)),
      conditionType: this.formConditionType,
      conditionThreshold: Number(this.formConditionThreshold),
      targetRole: this.formTargetRole, // 🆕
      iconEmoji: (this.formIconEmoji || '🏅').trim()
    };
  }

  private resetForm(): void {
    this.formTitle = '';
    this.formDescription = '';
    this.formXpReward = 50;
    this.formConditionType = 'PROJECT_COMPLETED';
    this.formConditionThreshold = 1;
    this.formTargetRole = 'ALL'; // 🆕
    this.formIconEmoji = '🏅';
  }

  private onSuccess(msg: string): void {
    this.successMessage = msg;
    this.saving = false;
    this.loadAchievements();
    this.cdr.detectChanges();
  }

  private onError(err: unknown): void {
    this.saving = false;
    if (err instanceof HttpErrorResponse) {
      if (err.status === 401) this.formError = 'Session expired.';
      else if (err.status === 403) this.formError = 'Admin only.';
      else this.formError = err.error?.message || 'Error occurred.';
    } else {
      this.formError = 'Could not save.';
    }
    this.cdr.detectChanges();
  }

  conditionLabel(t: GamificationConditionType): string {
    return this.conditionOptions.find((o) => o.value === t)?.label ?? t;
  }
}
