import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  PlanningService,
  GitHubBranchDto,
  GitHubCommitDto,
  GitHubIssueResponseDto,
} from '../../../core/services/planning.service';

/**
 * GitHub integration page: checks if GitHub is enabled, lists branches, shows latest commit, and allows creating issues.
 * Owner/repo can be set via query params. Uses PlanningService as proxy to the planning microservice.
 */
@Component({
  selector: 'app-github',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './github.html',
  styleUrl: './github.scss',
})
export class Github implements OnInit, OnDestroy {
  owner = '';
  repo = '';
  branch: string | null = null;

  enabled = false;
  /** True while waiting for GET /github/enabled. Stays false until response or timeout (8s). */
  loadingEnabled = true;
  /** Safety: if /github/enabled never completes, stop showing "Checking..." after this (ms). */
  private readonly enabledCheckMaxWaitMs = 10_000;
  private enabledCheckFallbackTimer: ReturnType<typeof setTimeout> | null = null;
  branches: GitHubBranchDto[] = [];
  loadingBranches = false;
  latestCommit: GitHubCommitDto | null = null;
  loadingCommit = false;
  commits: GitHubCommitDto[] = [];
  loadingCommits = false;
  issueTitle = '';
  issueBody = '';
  creatingIssue = false;
  createdIssue: GitHubIssueResponseDto | null = null;
  errorMessage: string | null = null;

  constructor(
    private readonly planning: PlanningService,
    private readonly route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const o = params['owner']?.trim();
      const r = params['repo']?.trim();
      if (o) this.owner = o;
      if (r) this.repo = r;
      this.cdr.detectChanges();
    });

    const stopLoading = (isEnabled: boolean) => {
      if (this.enabledCheckFallbackTimer != null) {
        clearTimeout(this.enabledCheckFallbackTimer);
        this.enabledCheckFallbackTimer = null;
      }
      setTimeout(() => {
        this.enabled = isEnabled;
        this.loadingEnabled = false;
        this.cdr.detectChanges();
      }, 0);
    };

    this.enabledCheckFallbackTimer = setTimeout(() => {
      if (this.loadingEnabled) {
        this.enabledCheckFallbackTimer = null;
        stopLoading(false);
      }
    }, this.enabledCheckMaxWaitMs);

    this.planning.isGitHubEnabled().subscribe({
      next: (enabled) => stopLoading(enabled),
      error: () => stopLoading(false),
    });
  }

  ngOnDestroy(): void {
    if (this.enabledCheckFallbackTimer != null) {
      clearTimeout(this.enabledCheckFallbackTimer);
      this.enabledCheckFallbackTimer = null;
    }
  }

  /** URL to open this repo on GitHub (codebase, readme, etc.). */
  get repoUrl(): string {
    if (!this.owner.trim() || !this.repo.trim()) return '';
    return `https://github.com/${encodeURIComponent(this.owner.trim())}/${encodeURIComponent(this.repo.trim())}`;
  }

  get repoFilled(): boolean {
    return this.owner.trim().length > 0 && this.repo.trim().length > 0;
  }

  onRepoInputChange(): void {
    this.branch = null;
    this.branches = [];
    this.latestCommit = null;
    this.commits = [];
    this.createdIssue = null;
    this.errorMessage = null;
  }

  formatGitHubDate(s: string | null | undefined): string {
    if (!s) return '—';
    const d = new Date(s);
    return Number.isNaN(d.getTime()) ? s : d.toLocaleString();
  }

  loadBranches(): void {
    if (!this.repoFilled) return;
    this.errorMessage = null;
    this.loadingBranches = true;
    this.cdr.detectChanges();
    this.planning.getGitHubBranches(this.owner.trim(), this.repo.trim()).subscribe({
      next: (list) => {
        this.branches = list ?? [];
        this.loadingBranches = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.branches = [];
        this.loadingBranches = false;
        this.errorMessage = 'Failed to load branches. Check owner/repo and that GitHub integration is enabled.';
        this.cdr.detectChanges();
      },
    });
  }

  loadLatestCommit(): void {
    if (!this.repoFilled) return;
    this.errorMessage = null;
    this.loadingCommit = true;
    this.latestCommit = null;
    this.cdr.detectChanges();
    this.planning
      .getGitHubLatestCommit(this.owner.trim(), this.repo.trim(), this.branch)
      .subscribe({
        next: (commit) => {
          this.latestCommit = commit ?? null;
          this.loadingCommit = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.latestCommit = null;
          this.loadingCommit = false;
          this.errorMessage = 'Failed to load latest commit.';
          this.cdr.detectChanges();
        },
      });
  }

  loadCommits(): void {
    if (!this.repoFilled) return;
    this.errorMessage = null;
    this.loadingCommits = true;
    this.commits = [];
    this.cdr.detectChanges();
    this.planning
      .getGitHubCommits(this.owner.trim(), this.repo.trim(), this.branch, 30)
      .subscribe({
        next: (list) => {
          this.commits = list ?? [];
          this.loadingCommits = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.commits = [];
          this.loadingCommits = false;
          this.errorMessage = 'Failed to load commit history.';
          this.cdr.detectChanges();
        },
      });
  }

  createIssue(): void {
    const title = this.issueTitle?.trim();
    if (!this.repoFilled || !title) return;
    this.errorMessage = null;
    this.createdIssue = null;
    this.creatingIssue = true;
    this.cdr.detectChanges();
    this.planning
      .createGitHubIssue(this.owner.trim(), this.repo.trim(), title, this.issueBody?.trim() || null)
      .subscribe({
        next: (issue) => {
          this.createdIssue = issue ?? null;
          this.creatingIssue = false;
          if (issue) {
            this.issueTitle = '';
            this.issueBody = '';
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.creatingIssue = false;
          this.errorMessage = 'Failed to create issue. Check repo permissions and GitHub integration.';
          this.cdr.detectChanges();
        },
      });
  }
}
