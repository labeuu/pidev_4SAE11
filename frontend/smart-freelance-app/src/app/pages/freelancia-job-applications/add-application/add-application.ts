import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobApplicationService } from '../../../core/services/job-application.service';
import { JobService, Job } from '../../../core/services/job.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-add-application',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './add-application.html',
  styleUrl: './add-application.scss',
})
export class AddApplication implements OnInit {
  form!: FormGroup;
  job: Job | null = null;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  userId: number | null = null;
  jobId!: number;
  isLoading = true;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private appService: JobApplicationService,
    private jobService: JobService,
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.jobId = Number(this.route.snapshot.paramMap.get('id'));

    this.form = this.fb.group({
      proposalMessage: ['', [Validators.required, Validators.minLength(20)]],
      expectedRate: [null, [Validators.min(0)]],
      availabilityStart: ['']
    });

    this.jobService.getById(this.jobId).subscribe({
      next: job => { this.job = job; this.isLoading = false; },
      error: () => { this.isLoading = false; }
    });

    const email = this.authService.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe({
        next: user => this.userId = user?.id ?? null
      });
    }
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
      jobId: this.jobId,
      freelancerId: this.userId,
      proposalMessage: raw.proposalMessage,
      expectedRate: raw.expectedRate || null,
      availabilityStart: raw.availabilityStart || null
    };
    this.appService.addApplication(payload).subscribe({
      next: app => {
        this.isSubmitting = false;
        if (app) {
          this.submitSuccess = true;
          setTimeout(() => this.router.navigate(['/dashboard/my-job-applications']), 1200);
        } else {
          this.submitError = 'Failed to submit application. You may have already applied.';
        }
      },
      error: () => {
        this.isSubmitting = false;
        this.submitError = 'Failed to submit application. You may have already applied to this job.';
      }
    });
  }

  f(name: string) { return this.form.get(name); }
}
