import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
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
  form!: FormGroup;
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
  ) {}

  ngOnInit(): void {
    const today = new Date();
    this.minDate = today.toISOString().split('T')[0];

    this.form = this.fb.group({
      clientType: ['INDIVIDUAL', Validators.required],
      companyName: [''],
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      budgetMin: [null, [Validators.min(0)]],
      budgetMax: [null, [Validators.min(0)]],
      currency: ['USD'],
      deadline: [''],
      category: ['', Validators.required],
      locationType: ['REMOTE', Validators.required],
      requiredSkillIds: [[]]
    });

    const email = this.authService.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe({
        next: user => this.userId = user?.id ?? null
      });
    }

    this.portfolioService.getAllSkills().subscribe({
      next: skills => this.allSkills = skills,
      error: () => {}
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
      error: () => {
        this.isGenerating = false;
        this.aiError = 'AI generation failed. Please try again or fill the form manually.';
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.userId) {
      this.form.markAllAsTouched();
      if (!this.userId) this.submitError = 'User not identified. Please re-login.';
      return;
    }
    this.isSubmitting = true;
    this.submitError = null;
    const raw = this.form.value;
    const payload = {
      ...raw,
      clientId: this.userId,
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
      error: () => {
        this.isSubmitting = false;
        this.submitError = 'Failed to create job. Please try again.';
      }
    });
  }

  f(name: string) { return this.form.get(name); }
}
