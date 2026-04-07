import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobService, Job } from '../../../core/services/job.service';
import { JobApplicationService, JobApplication } from '../../../core/services/job-application.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-show-job',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './show-job.html',
  styleUrl: './show-job.scss',
})
export class ShowJob implements OnInit {
  job: Job | null = null;
  applications: JobApplication[] = [];
  isLoading = true;
  errorMessage: string | null = null;
  userRole: string | null = null;
  userId: number | null = null;
  hasApplied = false;
  statusUpdateError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService,
    private appService: JobApplicationService,
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.userRole = this.authService.getUserRole();
    const email = this.authService.getPreferredUsername();
    const jobId = Number(this.route.snapshot.paramMap.get('id'));

    const loadJob = () => {
      this.jobService.getById(jobId).subscribe({
        next: job => {
          this.job = job;
          this.isLoading = false;
          if (this.isClient && this.isOwner) {
            this.loadApplications(jobId);
          }
          if (this.userRole === 'FREELANCER' && this.userId) {
            this.appService.getApplicationsByFreelancer(this.userId).subscribe({
              next: apps => { this.hasApplied = apps.some(a => a.jobId === jobId); }
            });
          }
        },
        error: () => { this.errorMessage = 'Job not found'; this.isLoading = false; }
      });
    };

    if (email) {
      this.userService.getByEmail(email).subscribe({
        next: user => { this.userId = user?.id ?? null; loadJob(); },
        error: () => loadJob()
      });
    } else {
      loadJob();
    }
  }

  loadApplications(jobId: number): void {
    this.appService.getApplicationsByJob(jobId).subscribe({
      next: apps => this.applications = apps
    });
  }

  get isClient(): boolean { return this.userRole === 'CLIENT'; }
  get isFreelancer(): boolean { return this.userRole === 'FREELANCER'; }
  get isOwner(): boolean { return this.userId !== null && this.job?.clientId === this.userId; }

  updateApplicationStatus(appId: number, status: string): void {
    this.statusUpdateError = null;
    this.appService.updateStatus(appId, status).subscribe({
      next: updated => {
        if (updated) {
          const idx = this.applications.findIndex(a => a.id === appId);
          if (idx >= 0) this.applications[idx] = { ...this.applications[idx], status: updated.status };
        } else {
          this.statusUpdateError = 'Failed to update status.';
        }
      },
      error: () => { this.statusUpdateError = 'Failed to update status.'; }
    });
  }

  applyNow(): void {
    this.router.navigate(['/dashboard/my-job-applications/add', this.job?.id]);
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'badge bg-success';
      case 'IN_PROGRESS': return 'badge bg-primary';
      case 'FILLED': return 'badge bg-secondary';
      case 'CANCELLED': return 'badge bg-danger';
      case 'PENDING': return 'badge bg-warning text-dark';
      case 'SHORTLISTED': return 'badge bg-info text-dark';
      case 'ACCEPTED': return 'badge bg-success';
      case 'REJECTED': return 'badge bg-danger';
      case 'WITHDRAWN': return 'badge bg-secondary';
      default: return 'badge bg-light text-dark';
    }
  }

  back(): void { window.history.back(); }
}
