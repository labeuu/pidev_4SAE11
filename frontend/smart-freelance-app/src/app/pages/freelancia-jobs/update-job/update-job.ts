import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobService, Job } from '../../../core/services/job.service';
import { PortfolioService, Skill } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-update-job',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './update-job.html',
  styleUrl: './update-job.scss',
})
export class UpdateJob implements OnInit {
  form!: FormGroup;
  jobId!: number;
  isLoading = true;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  loadError: string | null = null;
  allSkills: Skill[] = [];
  minDate!: string;

  readonly CATEGORIES = [
    'Web Development', 'Mobile Development', 'UI/UX Design',
    'Data Science', 'DevOps', 'Content Writing', 'Marketing', 'Backend'
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService,
    private portfolioService: PortfolioService
  ) {}

  ngOnInit(): void {
    this.jobId = Number(this.route.snapshot.paramMap.get('id'));
    this.minDate = new Date().toISOString().split('T')[0];

    this.form = this.fb.group({
      clientId: [null],
      clientType: ['INDIVIDUAL', Validators.required],
      companyName: [''],
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      budgetMin: [null],
      budgetMax: [null],
      currency: ['USD'],
      deadline: [''],
      category: ['', Validators.required],
      locationType: ['REMOTE', Validators.required],
      requiredSkillIds: [[]]
    });

    this.portfolioService.getAllSkills().subscribe({ next: skills => this.allSkills = skills });

    this.jobService.getById(this.jobId).subscribe({
      next: job => {
        if (!job) { this.loadError = 'Job not found'; this.isLoading = false; return; }
        this.patchForm(job);
        this.isLoading = false;
      },
      error: () => { this.loadError = 'Failed to load job'; this.isLoading = false; }
    });
  }

  private patchForm(job: Job): void {
    this.form.patchValue({
      clientId: job.clientId,
      clientType: job.clientType ?? 'INDIVIDUAL',
      companyName: job.companyName ?? '',
      title: job.title,
      description: job.description,
      budgetMin: job.budgetMin,
      budgetMax: job.budgetMax,
      currency: job.currency ?? 'USD',
      deadline: job.deadline ? job.deadline.split('T')[0] : '',
      category: job.category,
      locationType: job.locationType ?? 'REMOTE',
      requiredSkillIds: job.requiredSkillIds ?? []
    });
  }

  get isCompany(): boolean { return this.form.get('clientType')?.value === 'COMPANY'; }

  toggleSkill(skillId: number): void {
    const ctrl = this.form.get('requiredSkillIds')!;
    const ids: number[] = ctrl.value ?? [];
    const idx = ids.indexOf(skillId);
    ctrl.setValue(idx >= 0 ? ids.filter(id => id !== skillId) : [...ids, skillId]);
  }

  isSkillSelected(skillId: number): boolean {
    return (this.form.get('requiredSkillIds')?.value ?? []).includes(skillId);
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.isSubmitting = true;
    this.submitError = null;
    const raw = this.form.value;
    const payload = { ...raw, deadline: raw.deadline ? raw.deadline + 'T00:00:00' : null };
    this.jobService.updateJob(this.jobId, payload).subscribe({
      next: job => {
        this.isSubmitting = false;
        if (job) {
          this.submitSuccess = true;
          setTimeout(() => this.router.navigate(['/dashboard/my-jobs']), 1200);
        } else {
          this.submitError = 'Failed to update job.';
        }
      },
      error: () => { this.isSubmitting = false; this.submitError = 'Failed to update job.'; }
    });
  }

  f(name: string) { return this.form.get(name); }
}
