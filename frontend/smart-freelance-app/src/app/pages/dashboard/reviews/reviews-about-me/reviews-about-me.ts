import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { StarRating } from '../../../../shared/components/star-rating/star-rating';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService, User } from '../../../../core/services/user.service';
import { ReviewService, Review, ReviewStats } from '../../../../core/services/review.service';
import { ReviewResponseService, ReviewResponseItem } from '../../../../core/services/review-response.service';
import { ProjectService } from '../../../../core/services/project.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-reviews-about-me',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StarRating],
  templateUrl: './reviews-about-me.html',
  styleUrl: './reviews-about-me.scss',
})
export class ReviewsAboutMe implements OnInit, OnDestroy {
  reviews: Review[] = [];
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  reviewerNames: Record<number, string> = {};
  projectTitles: Record<number, string> = {};
  stats: ReviewStats | null = null;
  searchTerm = '';
  ratingFilter: number | null = null;
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  pageSizes = [5, 10, 20, 50];
  /** Review ID -> list of response messages (thread) */
  responsesByReview: Record<number, ReviewResponseItem[]> = {};
  /** Draft message per review ID */
  newMessageByReview: Record<number, string> = {};
  /** Review ID for which send is in progress */
  sendingReviewId: number | null = null;
  /** Message pending delete confirmation (modal) */
  messageToDelete: ReviewResponseItem | null = null;
  /** Review context for the message being deleted */
  deleteMessageReview: Review | null = null;
  deletingMessage = false;
  private searchSubject = new Subject<string>();
  private pollInterval: ReturnType<typeof setInterval> | null = null;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private reviewService: ReviewService,
    private reviewResponseService: ReviewResponseService,
    private projectService: ProjectService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
      this.searchTerm = term;
      this.page = 0;
      this.load();
    });
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (this.currentUser) {
        this.load();
        this.loadStats();
        this.pollInterval = setInterval(() => {
          if (this.reviews.length > 0) this.loadResponsesForReviews();
        }, 5000);
      } else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  onRatingFilterChange(): void {
    this.page = 0;
    this.load();
  }

  load(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.reviewService
      .getByRevieweeIdPage(this.currentUser.id, {
        page: this.page,
        size: this.size,
        search: this.searchTerm || undefined,
        rating: this.ratingFilter ?? undefined,
      })
      .subscribe({
        next: (res) => {
          if (res) {
            this.reviews = res.content ?? [];
            this.totalElements = res.totalElements;
            this.totalPages = res.totalPages;
            this.loadReviewerNames();
            this.loadProjectTitles();
            this.loadResponsesForReviews();
          } else {
            this.reviews = [];
            this.totalElements = 0;
            this.totalPages = 0;
          }
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorMessage = 'Failed to load reviews.';
          this.loading = false;
          this.cdr.detectChanges();
        },
      });
  }

  loadStats(): void {
    if (!this.currentUser?.id) return;
    this.reviewService.getStatsByReviewee(this.currentUser.id).subscribe((s) => {
      this.stats = s ?? null;
      this.cdr.detectChanges();
    });
  }

  private loadReviewerNames(): void {
    const ids = [...new Set(this.reviews.map((r) => r.reviewerId))];
    ids.forEach((id) => {
      if (this.reviewerNames[id]) return;
      this.userService.getById(id).subscribe((u) => {
        if (u) this.reviewerNames[id] = (u.firstName + ' ' + u.lastName).trim() || u.email;
        else this.reviewerNames[id] = 'User #' + id;
        this.cdr.detectChanges();
      });
    });
  }

  getReviewerName(r: Review): string {
    return this.reviewerNames[r.reviewerId] ?? 'User #' + r.reviewerId;
  }

  private loadProjectTitles(): void {
    const ids = [...new Set(this.reviews.map((r) => r.projectId))];
    ids.forEach((id) => {
      if (this.projectTitles[id]) return;
      this.projectService.getById(id).subscribe((p) => {
        this.projectTitles[id] = p?.title ?? 'Project #' + id;
        this.cdr.detectChanges();
      });
    });
  }

  getProjectTitle(r: Review): string {
    return this.projectTitles[r.projectId] ?? 'Project #' + r.projectId;
  }

  private loadResponsesForReviews(): void {
    this.reviews.forEach((r) => {
      if (r.id == null) return;
      this.reviewResponseService.getByReviewId(r.id).subscribe((list) => {
        this.responsesByReview[r.id!] = list ?? [];
        this.cdr.detectChanges();
      });
    });
  }

  getResponses(reviewId: number | undefined): ReviewResponseItem[] {
    return reviewId != null ? (this.responsesByReview[reviewId] ?? []) : [];
  }

  getRespondentName(resp: ReviewResponseItem, r: Review): string {
    if (this.currentUser && resp.respondentId === this.currentUser.id) return 'You';
    return this.reviewerNames[resp.respondentId] ?? 'User #' + resp.respondentId;
  }

  isMyMessage(msg: ReviewResponseItem): boolean {
    return !!(this.currentUser && msg.respondentId === this.currentUser.id);
  }

  openDeleteMessageModal(msg: ReviewResponseItem, r: Review): void {
    if (!this.isMyMessage(msg)) return;
    this.messageToDelete = msg;
    this.deleteMessageReview = r;
  }

  closeDeleteMessageModal(): void {
    if (!this.deletingMessage) {
      this.messageToDelete = null;
      this.deleteMessageReview = null;
    }
  }

  confirmDeleteMessage(): void {
    const msg = this.messageToDelete;
    const r = this.deleteMessageReview;
    if (!msg?.id || !r?.id || !this.isMyMessage(msg)) {
      this.closeDeleteMessageModal();
      return;
    }
    const reviewId = r.id;
    this.deletingMessage = true;
    this.reviewResponseService.delete(msg.id).subscribe((ok) => {
      this.deletingMessage = false;
      this.messageToDelete = null;
      this.deleteMessageReview = null;
      if (ok) {
        this.responsesByReview[reviewId] = (this.responsesByReview[reviewId] ?? []).filter((m) => m.id !== msg.id);
      }
      this.cdr.detectChanges();
    });
  }

  /** Format respondedAt (string or Java LocalDateTime array) for display. */
  getResponseTime(msg: ReviewResponseItem): string {
    const at = msg.respondedAt;
    if (at == null) return '';
    if (Array.isArray(at)) {
      const [y, m, d, h = 0, min = 0, s = 0] = at;
      const date = new Date(y ?? 0, (m ?? 1) - 1, d ?? 1, h, min, s);
      return date.toLocaleString();
    }
    try {
      return new Date(at).toLocaleString();
    } catch {
      return String(at);
    }
  }

  getRatingCount(r: number): number {
    return this.stats?.countByRating?.[r] ?? 0;
  }

  getDraft(reviewId: number | undefined): string {
    return reviewId != null ? (this.newMessageByReview[reviewId] ?? '') : '';
  }

  setDraft(reviewId: number | undefined, value: string): void {
    if (reviewId != null) this.newMessageByReview[reviewId] = value;
  }

  sendMessage(r: Review): void {
    const reviewId = r.id!;
    const text = (this.newMessageByReview[reviewId] ?? '').trim();
    if (!text || !this.currentUser?.id) return;
    this.sendingReviewId = reviewId;
    this.reviewResponseService
      .create({ reviewId, respondentId: this.currentUser.id, message: text })
      .subscribe({
        next: (created) => {
          this.sendingReviewId = null;
          if (created) {
            this.responsesByReview[reviewId] = [...(this.responsesByReview[reviewId] ?? []), created];
            this.newMessageByReview[reviewId] = '';
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.sendingReviewId = null;
          this.cdr.detectChanges();
        },
      });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.load();
  }

  setPageSize(newSize: number): void {
    this.size = newSize;
    this.page = 0;
    this.load();
  }

  get roundedAverageRating(): number {
    return this.stats ? Math.round(this.stats.averageRating) : 0;
  }

  get ratingOptions(): { value: number | null; label: string }[] {
    return [
      { value: null, label: 'All ratings' },
      { value: 5, label: '5 stars' },
      { value: 4, label: '4 stars' },
      { value: 3, label: '3 stars' },
      { value: 2, label: '2 stars' },
      { value: 1, label: '1 star' },
    ];
  }
}
