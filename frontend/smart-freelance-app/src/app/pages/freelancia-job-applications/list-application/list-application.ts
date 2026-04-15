import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { JobApplicationService, JobApplication } from '../../../core/services/job-application.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-list-application',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './list-application.html',
  styleUrl: './list-application.scss',
})
export class ListApplication implements OnInit {
  applications: JobApplication[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  showDeleteModal = false;
  appToDelete: number | null = null;
  deleteError: string | null = null;
  userId: number | null = null;

  constructor(
    private appService: JobApplicationService,
    private authService: AuthService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const email = this.authService.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe({
        next: user => {
          this.userId = user?.id ?? null;
          this.loadApplications();
        },
        error: () => this.loadApplications()
      });
    } else {
      this.loadApplications();
    }
  }

  loadApplications(): void {
    if (!this.userId) { this.errorMessage = 'User not identified'; return; }
    this.isLoading = true;
    this.appService.getApplicationsByFreelancer(this.userId).subscribe({
      next: apps => { this.applications = apps; this.isLoading = false; },
      error: () => { this.errorMessage = 'Failed to load applications'; this.isLoading = false; }
    });
  }

  viewApplication(id: number): void {
    this.router.navigate(['/dashboard/my-job-applications', id, 'show']);
  }

  confirmDelete(id: number): void {
    this.appToDelete = id;
    this.showDeleteModal = true;
    this.deleteError = null;
  }

  cancelDelete(): void {
    this.appToDelete = null;
    this.showDeleteModal = false;
    this.deleteError = null;
  }

  doDelete(): void {
    if (!this.appToDelete) return;
    this.appService.deleteApplication(this.appToDelete).subscribe({
      next: ok => {
        if (ok) {
          this.applications = this.applications.filter(a => a.id !== this.appToDelete);
          this.showDeleteModal = false;
          this.appToDelete = null;
        } else {
          this.deleteError = 'Failed to withdraw application.';
        }
      },
      error: () => { this.deleteError = 'Failed to withdraw application.'; }
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
}
