import { Component, OnInit, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobService, Job, FitScoreResult } from '../../../core/services/job.service';
import { JobApplicationService, JobApplication } from '../../../core/services/job-application.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { TranslationService, Language } from '../../../core/services/translation.service';
import { forkJoin } from 'rxjs';

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

  // ── Fit score state ────────────────────────────────────────
  fitScore: FitScoreResult | null = null;
  isScoringLoading = false;
  scoringError: string | null = null;

  // ── Translation state ──────────────────────────────────────
  languages: Language[] = [];
  selectedLang = 'fr';
  isTranslating = false;
  isTranslated = false;
  translatedTitle = '';
  translatedDescription = '';
  showLangPicker = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService,
    private appService: JobApplicationService,
    private authService: AuthService,
    private userService: UserService,
    public translationService: TranslationService,
    private elRef: ElementRef
  ) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.showLangPicker && !this.elRef.nativeElement.querySelector('.lang-picker')?.contains(event.target)) {
      this.showLangPicker = false;
    }
  }

  ngOnInit(): void {
    this.languages = this.translationService.LANGUAGES;
    this.userRole = this.authService.getUserRole();
    const email = this.authService.getPreferredUsername();
    const jobId = Number(this.route.snapshot.paramMap.get('id'));

    const loadJob = () => {
      this.jobService.getById(jobId).subscribe({
        next: job => {
          this.job = job;
          this.isLoading = false;
          if (this.isClient && this.isOwner) this.loadApplications(jobId);
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

  // ── Translation ────────────────────────────────────────────

  selectLang(code: string): void {
    this.selectedLang = code;
    this.showLangPicker = false;
    if (this.isTranslated) {
      // Re-translate with new language
      this.isTranslated = false;
      this.translate();
    }
  }

  translate(): void {
    if (!this.job || this.isTranslating) return;
    this.isTranslating = true;
    this.isTranslated = false;
    this.showLangPicker = false;

    forkJoin({
      title: this.translationService.translate(this.job.title, this.selectedLang),
      desc:  this.translationService.translate(this.job.description, this.selectedLang),
    }).subscribe({
      next: ({ title, desc }) => {
        this.translatedTitle       = title;
        this.translatedDescription = desc;
        this.isTranslating = false;
        this.isTranslated  = true;
      },
      error: () => {
        this.translatedTitle       = this.job?.title ?? '';
        this.translatedDescription = this.job?.description ?? '';
        this.isTranslating = false;
        this.isTranslated  = true;
      }
    });
  }

  showOriginal(): void {
    this.isTranslated = false;
    this.translatedTitle = '';
    this.translatedDescription = '';
  }

  get displayTitle(): string {
    return this.isTranslated ? this.translatedTitle : (this.job?.title ?? '');
  }

  get displayDescription(): string {
    return this.isTranslated ? this.translatedDescription : (this.job?.description ?? '');
  }

  get currentLangFlag(): string {
    return this.translationService.getLangFlag(this.selectedLang);
  }

  get currentLangName(): string {
    return this.translationService.getLangName(this.selectedLang);
  }

  // ── Existing logic ─────────────────────────────────────────

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

  analyzeMyFit(): void {
    if (!this.job?.id || !this.userId || this.isScoringLoading) return;
    this.isScoringLoading = true;
    this.scoringError = null;
    this.fitScore = null;

    this.jobService.getFitScore(this.job.id, this.userId).subscribe({
      next: result => {
        this.fitScore = result;
        this.isScoringLoading = false;
        if (!result) {
          this.scoringError = 'Could not analyse your profile. Please try again.';
        }
      },
      error: () => {
        this.isScoringLoading = false;
        this.scoringError = 'Could not analyse your profile. Please try again.';
      }
    });
  }

  applyNow(): void {
    this.router.navigate(['/dashboard/my-job-applications/add', this.job?.id]);
  }

  messageFreelancer(freelancerId: number): void {
    this.router.navigate(['/dashboard/messages'], { queryParams: { partnerId: freelancerId } });
  }

  back(): void { window.history.back(); }

  /** Uses names from API when present; otherwise falls back to Freelancer #id. */
  freelancerDisplayName(app: JobApplication): string {
    const f = app.freelancerFirstName?.trim();
    const l = app.freelancerLastName?.trim();
    if (f || l) {
      return [f, l].filter(Boolean).join(' ');
    }
    return `Freelancer #${app.freelancerId}`;
  }
}
