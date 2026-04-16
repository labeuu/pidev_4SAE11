import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { asyncScheduler } from 'rxjs';
import { observeOn } from 'rxjs/operators';
import { JobService } from '../../../core/services/job.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { PortfolioService, Skill } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-add-job',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './add-job.html',
  styleUrl: './add-job.scss',
})
export class AddJob implements OnInit {
  private static readonly MIN_POSITIVE_VALUE = 0.01;

  form: FormGroup;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  minDate!: string;
  allSkills: Skill[] = [];
  userId: number | null = null;

  // AI generation state
  aiPrompt = '';
  isGenerating = false;
  aiError: string | null = null;
  aiDraftReady = false;

  readonly CATEGORIES = [
    'Web Development', 'Mobile Development', 'UI/UX Design',
    'Data Science', 'DevOps', 'Content Writing', 'Marketing', 'Backend'
  ];

  constructor(
    private fb: FormBuilder,
    private jobService: JobService,
    private authService: AuthService,
    private userService: UserService,
    private portfolioService: PortfolioService,
    private router: Router
  ) {
    this.form = this.buildForm();
  }

  ngOnInit(): void {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.minDate = tomorrow.toISOString().split('T')[0];

    const email = this.authService.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).pipe(observeOn(asyncScheduler)).subscribe({
        next: user => this.userId = user?.id ?? null
      });
    }

    this.portfolioService.getAllSkills().pipe(observeOn(asyncScheduler)).subscribe({
      next: skills => this.allSkills = skills,
      error: () => {}
    });

    this.form.get('clientType')?.valueChanges.subscribe(type => {
      const companyName = this.form.get('companyName');
      if (!companyName) return;
      if (type === 'COMPANY') {
        companyName.setValidators([Validators.required, Validators.minLength(2)]);
      } else {
        companyName.clearValidators();
        companyName.setValue('');
      }
      companyName.updateValueAndValidity({ emitEvent: false });
    });
  }

  get isCompany(): boolean {
    return this.form.get('clientType')?.value === 'COMPANY';
  }

  toggleSkill(skillId: number): void {
    const ctrl = this.form.get('requiredSkillIds')!;
    const ids: number[] = ctrl.value ?? [];
    const idx = ids.indexOf(skillId);
    ctrl.setValue(idx >= 0 ? ids.filter(id => id !== skillId) : [...ids, skillId]);
  }

  isSkillSelected(skillId: number): boolean {
    return (this.form.get('requiredSkillIds')?.value ?? []).includes(skillId);
  }

  generateWithAI(): void {
    if (!this.aiPrompt.trim()) return;
    this.isGenerating = true;
    this.aiError = null;
    this.aiDraftReady = false;

    this.jobService.generateJobDraft(this.aiPrompt.trim()).subscribe({
      next: draft => {
        this.isGenerating = false;
        if (!draft) {
          this.aiError = 'AI generation failed. Please try again or fill the form manually.';
          return;
        }

        // Patch form fields with AI-generated values
        this.form.patchValue({
          title: draft.title,
          description: draft.description,
          budgetMin: draft.budgetMin,
          budgetMax: draft.budgetMax,
          currency: draft.currency || 'USD',
          category: this.CATEGORIES.includes(draft.category) ? draft.category : '',
          locationType: ['REMOTE', 'ONSITE', 'HYBRID'].includes(draft.locationType)
            ? draft.locationType : 'REMOTE',
        });

        // Auto-select skills by name matching
        if (draft.requiredSkills?.length && this.allSkills.length) {
          const draftSkillNames = draft.requiredSkills.map(s => s.toLowerCase());
          const matchedIds = this.allSkills
            .filter(s => draftSkillNames.some(n => s.name?.toLowerCase().includes(n) || n.includes(s.name?.toLowerCase() ?? '')))
            .map(s => s.id!);
          this.form.get('requiredSkillIds')!.setValue(matchedIds);
        }

        this.aiDraftReady = true;
      },
      error: (error: HttpErrorResponse) => {
        this.isGenerating = false;
        this.aiError = error.status === 503
          ? 'AI service is temporarily unavailable. Please try again in a moment or fill the form manually.'
          : 'AI generation failed. Please try again or fill the form manually.';
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.userId) {
      this.form.markAllAsTouched();
      if (!this.userId) this.submitError = 'User not identified. Please re-login.';
      return;
    }

    const raw = this.form.value;
    if (raw.deadline && raw.deadline < this.minDate) {
      this.submitError = 'The deadline must be after today.';
      this.form.get('deadline')?.markAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.submitError = null;
    const payload = {
      ...raw,
      clientId: this.userId,
      companyName: raw.clientType === 'COMPANY' ? raw.companyName?.trim() || null : null,
      deadline: raw.deadline ? raw.deadline + 'T00:00:00' : null,
    };
    this.jobService.createJob(payload).subscribe({
      next: job => {
        this.isSubmitting = false;
        if (job) {
          this.submitSuccess = true;
          setTimeout(() => this.router.navigate(['/dashboard/my-jobs']), 1200);
        } else {
          this.submitError = 'Failed to create job. Please try again.';
        }
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting = false;
        if (error.status === 400) {
          this.submitError = this.extractValidationMessage(error)
            ?? 'Please check the form. The deadline must be after today and numeric values must be positive.';
          return;
        }
        this.submitError = 'Failed to create job. Please try again.';
      }
    });
  }

  f(name: string) { return this.form.get(name); }

  private buildForm(): FormGroup {
    return this.fb.group({
      clientType: ['INDIVIDUAL', Validators.required],
      companyName: [''],
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      budgetMin: [null, [Validators.min(AddJob.MIN_POSITIVE_VALUE)]],
      budgetMax: [null, [Validators.min(AddJob.MIN_POSITIVE_VALUE)]],
      currency: ['USD'],
      deadline: [''],
      category: ['', Validators.required],
      locationType: ['REMOTE', Validators.required],
      requiredSkillIds: [[]]
    });
  }

  private extractValidationMessage(error: HttpErrorResponse): string | null {
    const payload = error.error;
    if (typeof payload === 'string' && payload.trim()) {
      return payload;
    }
    if (payload?.message) {
      return String(payload.message);
    }
    if (Array.isArray(payload?.errors) && payload.errors.length > 0) {
      return String(payload.errors[0]);
    }
    return null;
  }
}
