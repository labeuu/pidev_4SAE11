import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobApplicationService } from '../../../core/services/job-application.service';

@Component({
  selector: 'app-update-application',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './update-application.html',
  styleUrl: './update-application.scss',
})
export class UpdateApplication implements OnInit {
  form!: FormGroup;
  appId!: number;
  isLoading = true;
  isSubmitting = false;
  loadError: string | null = null;
  submitError: string | null = null;
  submitSuccess = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private appService: JobApplicationService
  ) {}

  ngOnInit(): void {
    this.appId = Number(this.route.snapshot.paramMap.get('id'));

    this.form = this.fb.group({
      jobId: [null],
      freelancerId: [null],
      proposalMessage: ['', [Validators.required, Validators.minLength(20)]],
      expectedRate: [null, [Validators.min(0)]],
      availabilityStart: ['']
    });

    this.appService.getApplicationById(this.appId).subscribe({
      next: app => {
        if (!app) { this.loadError = 'Application not found'; this.isLoading = false; return; }
        this.form.patchValue({
          jobId: app.jobId,
          freelancerId: app.freelancerId,
          proposalMessage: app.proposalMessage ?? '',
          expectedRate: app.expectedRate ?? null,
          availabilityStart: app.availabilityStart ?? ''
        });
        this.isLoading = false;
      },
      error: () => { this.loadError = 'Failed to load application'; this.isLoading = false; }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.isSubmitting = true;
    this.submitError = null;
    const payload = this.form.value;
    this.appService.updateApplication(this.appId, payload).subscribe({
      next: app => {
        this.isSubmitting = false;
        if (app) {
          this.submitSuccess = true;
          setTimeout(() => this.router.navigate(['/dashboard/my-job-applications']), 1200);
        } else {
          this.submitError = 'Failed to update application.';
        }
      },
      error: () => { this.isSubmitting = false; this.submitError = 'Failed to update application.'; }
    });
  }

  f(name: string) { return this.form.get(name); }
}
