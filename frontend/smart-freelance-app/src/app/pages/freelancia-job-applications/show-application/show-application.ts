import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { JobApplicationService, JobApplication } from '../../../core/services/job-application.service';
import { JobService, Job } from '../../../core/services/job.service';

@Component({
  selector: 'app-show-application',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './show-application.html',
  styleUrl: './show-application.scss',
})
export class ShowApplication implements OnInit {
  application: JobApplication | null = null;
  job: Job | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private appService: JobApplicationService,
    private jobService: JobService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.appService.getApplicationById(id).subscribe({
      next: app => {
        this.application = app;
        this.isLoading = false;
        if (app?.jobId) {
          this.jobService.getById(app.jobId).subscribe({ next: job => this.job = job });
        }
      },
      error: () => { this.errorMessage = 'Application not found'; this.isLoading = false; }
    });
  }

  statusBadgeClass(status: string): string {
    switch (status) {
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
