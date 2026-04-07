import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { JobService, Job, JobFilters, JobStats } from '../../../core/services/job.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-list-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './list-jobs.html',
  styleUrl: './list-jobs.scss',
})
export class ListJobs implements OnInit, OnDestroy {
  jobs: Job[] = [];
  filteredJobs: Job[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  deleteError: string | null = null;
  showDeleteModal = false;
  jobToDelete: number | null = null;
  userRole: string | null = null;
  userId: number | null = null;

  // Statistics
  statistics: Record<string, number> = {};
  applicationStats: JobStats[] = [];
  showStats = false;

  // Filters
  filters: JobFilters = {};
  filterKeyword = '';
  filterCategory = '';
  filterLocationType = '';
  filterStatus = '';
  showFilters = false;

  get hasActiveFilters(): boolean {
    return !!(this.filterCategory || this.filterLocationType || this.filterStatus);
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  // Pagination
  currentPage = 1;
  readonly pageSize = 9;

  get totalPages(): number {
    return Math.ceil(this.filteredJobs.length / this.pageSize);
  }

  get pagedJobs(): Job[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredJobs.slice(start, start + this.pageSize);
  }

  get pageNumbers(): (number | '...')[] {
    const total = this.totalPages;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    const pages: (number | '...')[] = [1];
    if (this.currentPage > 3) pages.push('...');
    for (let i = Math.max(2, this.currentPage - 1); i <= Math.min(total - 1, this.currentPage + 1); i++) {
      pages.push(i);
    }
    if (this.currentPage < total - 2) pages.push('...');
    pages.push(total);
    return pages;
  }

  get pageEnd(): number {
    return Math.min(this.currentPage * this.pageSize, this.filteredJobs.length);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  private refreshInterval: any;
  readonly CATEGORIES = [
    'Web Development', 'Mobile Development', 'UI/UX Design',
    'Data Science', 'DevOps', 'Content Writing', 'Marketing', 'Backend'
  ];

  constructor(
    private jobService: JobService,
    private authService: AuthService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userRole = this.authService.getUserRole();
    const email = this.authService.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe({
        next: user => {
          this.userId = user?.id ?? null;
          this.loadJobs();
        },
        error: () => this.loadJobs()
      });
    } else {
      this.loadJobs();
    }
    this.refreshInterval = setInterval(() => this.loadJobs(), 60_000);
  }

  ngOnDestroy(): void {
    clearInterval(this.refreshInterval);
  }

  loadJobs(): void {
    this.isLoading = true;
    if (this.userRole === 'CLIENT' && this.userId) {
      this.jobService.getByClientId(this.userId).subscribe({
        next: jobs => { this.jobs = jobs; this.applyFilters(); this.isLoading = false; },
        error: () => { this.errorMessage = 'Failed to load jobs'; this.isLoading = false; }
      });
      this.loadStats();
    } else if (this.userRole === 'FREELANCER') {
      this.jobService.getAllJobs().subscribe({
        next: jobs => {
          this.jobs = jobs.filter(j => j.status === 'OPEN');
          this.applyFilters();
          this.isLoading = false;
        },
        error: () => { this.errorMessage = 'Failed to load jobs'; this.isLoading = false; }
      });
    } else {
      this.jobService.getAllJobs().subscribe({
        next: jobs => { this.jobs = jobs; this.applyFilters(); this.isLoading = false; },
        error: () => { this.errorMessage = 'Failed to load jobs'; this.isLoading = false; }
      });
      this.loadStats();
    }
  }

  loadStats(): void {
    this.jobService.getJobStatistics().subscribe(stats => this.statistics = stats);
    this.jobService.getApplicationStats().subscribe(s => this.applicationStats = s);
  }

  applyFilters(): void {
    let result = [...this.jobs];
    if (this.filterKeyword) {
      const kw = this.filterKeyword.toLowerCase();
      result = result.filter(j =>
        j.title?.toLowerCase().includes(kw) || j.description?.toLowerCase().includes(kw)
      );
    }
    if (this.filterCategory) {
      result = result.filter(j => j.category === this.filterCategory);
    }
    if (this.filterLocationType) {
      result = result.filter(j => j.locationType === this.filterLocationType);
    }
    if (this.filterStatus) {
      result = result.filter(j => j.status === this.filterStatus);
    }
    this.filteredJobs = result;
    this.currentPage = 1;
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.filterKeyword = '';
    this.filterCategory = '';
    this.filterLocationType = '';
    this.filterStatus = '';
    this.applyFilters();
  }

  viewJob(id: number): void {
    if (this.userRole === 'CLIENT') {
      this.router.navigate(['/dashboard/my-jobs', id, 'show']);
    } else {
      this.router.navigate(['/dashboard/browse-freelancia-jobs', id, 'show']);
    }
  }

  editJob(id: number): void {
    this.router.navigate(['/dashboard/my-jobs', id, 'edit']);
  }

  confirmDelete(id: number): void {
    this.jobToDelete = id;
    this.showDeleteModal = true;
  }

  cancelDelete(): void {
    this.jobToDelete = null;
    this.showDeleteModal = false;
    this.deleteError = null;
  }

  doDelete(): void {
    if (!this.jobToDelete) return;
    this.jobService.deleteJob(this.jobToDelete).subscribe({
      next: ok => {
        if (ok) {
          this.jobs = this.jobs.filter(j => j.id !== this.jobToDelete);
          this.applyFilters();
          this.showDeleteModal = false;
          this.jobToDelete = null;
        } else {
          this.deleteError = 'Failed to delete job.';
        }
      },
      error: () => { this.deleteError = 'Failed to delete job.'; }
    });
  }

  toggleStats(): void {
    this.showStats = !this.showStats;
  }

  isOwner(job: Job): boolean {
    return this.userId !== null && job.clientId === this.userId;
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'badge bg-success';
      case 'IN_PROGRESS': return 'badge bg-primary';
      case 'FILLED': return 'badge bg-secondary';
      case 'CANCELLED': return 'badge bg-danger';
      default: return 'badge bg-light text-dark';
    }
  }
}
