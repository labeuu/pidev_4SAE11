import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subject, Subscription, asyncScheduler } from 'rxjs';
import { debounceTime, distinctUntilChanged, observeOn, switchMap } from 'rxjs/operators';
import {
  JobService, Job, JobSearchRequest, JobPage, JobStats
} from '../../../core/services/job.service';
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

  // ── Data ──────────────────────────────────────────────────────────────────
  jobs: Job[] = [];
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

  // ── Filters (currentFilters tracks the live UI state) ────────────────────
  currentFilters: JobSearchRequest = {
    page: 0, size: 9, sortBy: 'createdAt', sortDir: 'desc'
  };
  filterKeyword = '';
  filterCategory = '';
  filterLocationType = '';
  filterStatus = '';
  filterBudgetMin: number | null = null;
  filterBudgetMax: number | null = null;
  showFilters = false;

  /** True when any non-keyword filter is active (drives the chip display). */
  get hasActiveFilters(): boolean {
    return !!(
      this.filterCategory ||
      this.filterLocationType ||
      this.filterStatus ||
      this.filterBudgetMin != null ||
      this.filterBudgetMax != null
    );
  }

  toggleFilters(): void { this.showFilters = !this.showFilters; }

  // ── Pagination (server-driven) ────────────────────────────────────────────
  totalElements = 0;
  totalPages = 0;
  readonly pageSize = 9;

  get currentPage(): number { return (this.currentFilters.page ?? 0) + 1; }

  get pageEnd(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalElements);
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

  goToPage(page: number): void {
    const zeroPage = page - 1;
    if (zeroPage >= 0 && zeroPage < this.totalPages) {
      this.currentFilters = { ...this.currentFilters, page: zeroPage };
      this.triggerFilter$.next(this.currentFilters);
    }
  }

  // ── Debounce stream ───────────────────────────────────────────────────────
  private readonly triggerFilter$ = new Subject<JobSearchRequest>();
  private readonly keywordInput$ = new Subject<string>();
  private subscriptions = new Subscription();

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
        next: user => { this.userId = user?.id ?? null; this.initFiltersAndLoad(); },
        error: () => this.initFiltersAndLoad()
      });
    } else {
      this.initFiltersAndLoad();
    }
  }

  private initFiltersAndLoad(): void {
    // For CLIENT: always scope to their own jobs
    if (this.userRole === 'CLIENT' && this.userId) {
      this.currentFilters = { ...this.currentFilters, clientId: this.userId };
      this.loadStats();
    }
    // For FREELANCER: only show OPEN jobs
    if (this.userRole === 'FREELANCER') {
      this.currentFilters = { ...this.currentFilters, status: 'OPEN' };
    }

    // Keyword gets its own debounce stream (300 ms)
    this.subscriptions.add(
      this.keywordInput$.pipe(
        debounceTime(300),
        distinctUntilChanged()
      ).subscribe(kw => {
        this.currentFilters = { ...this.currentFilters, keyword: kw || undefined, page: 0 };
        this.triggerFilter$.next(this.currentFilters);
      })
    );

    // Main filter stream: switchMap cancels in-flight requests on rapid changes
    this.subscriptions.add(
      this.triggerFilter$.pipe(
        observeOn(asyncScheduler),
        switchMap(req => {
          this.isLoading = true;
          this.errorMessage = null;
          return this.jobService.filterJobs(req);
        })
      ).subscribe({
        next: (page: JobPage) => {
          this.jobs          = page.content;
          this.totalElements = page.totalElements;
          this.totalPages    = page.totalPages;
          this.isLoading     = false;
        },
        error: () => {
          this.errorMessage = 'Failed to load jobs';
          this.isLoading    = false;
        }
      })
    );

    // Initial load
    this.triggerFilter$.next(this.currentFilters);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  // ── Filter change handlers ────────────────────────────────────────────────

  onKeywordChange(): void {
    this.keywordInput$.next(this.filterKeyword);
  }

  onFilterChange(): void {
    const filters: JobSearchRequest = {
      ...this.currentFilters,
      keyword:       this.filterKeyword || undefined,
      category:      this.filterCategory || undefined,
      locationType:  this.filterLocationType || undefined,
      budgetMin:     this.filterBudgetMin ?? undefined,
      budgetMax:     this.filterBudgetMax ?? undefined,
      page:          0,
    };

    // Status: FREELANCER is always locked to OPEN; others use the dropdown
    if (this.userRole !== 'FREELANCER') {
      filters.status = this.filterStatus || undefined;
    }

    this.currentFilters = filters;
    this.triggerFilter$.next(this.currentFilters);
  }

  clearFilters(): void {
    this.filterKeyword    = '';
    this.filterCategory   = '';
    this.filterLocationType = '';
    this.filterStatus     = '';
    this.filterBudgetMin  = null;
    this.filterBudgetMax  = null;
    this.onFilterChange();
  }

  // ── Remaining methods (unchanged) ─────────────────────────────────────────

  loadStats(): void {
    this.jobService.getJobStatistics().subscribe(stats => this.statistics = stats);
    this.jobService.getApplicationStats().subscribe(s => this.applicationStats = s);
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
          this.showDeleteModal = false;
          this.jobToDelete = null;
          this.triggerFilter$.next(this.currentFilters); // refresh current page
        } else {
          this.deleteError = 'Failed to delete job.';
        }
      },
      error: () => { this.deleteError = 'Failed to delete job.'; }
    });
  }

  toggleStats(): void { this.showStats = !this.showStats; }

  isOwner(job: Job): boolean {
    return this.userId !== null && job.clientId === this.userId;
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'OPEN':        return 'badge bg-success';
      case 'IN_PROGRESS': return 'badge bg-primary';
      case 'FILLED':      return 'badge bg-secondary';
      case 'CANCELLED':   return 'badge bg-danger';
      default:            return 'badge bg-light text-dark';
    }
  }
}
