import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { StarRating } from '../../../../shared/components/star-rating/star-rating';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService, User } from '../../../../core/services/user.service';
import { ReviewService, Review, ReviewStats } from '../../../../core/services/review.service';
import { ReviewResponseService, ReviewResponseItem } from '../../../../core/services/review-response.service';
import { ProjectService } from '../../../../core/services/project.service';

const POLL_INTERVAL_MS = 5000;

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StarRating],
  templateUrl: './my-reviews.html',
  styleUrl: './my-reviews.scss',
})
export class MyReviews implements OnInit, OnDestroy {
  reviews: Review[] = [];
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  reviewToDelete: Review | null = null;
  deleting = false;
  revieweeNames: Record<number, string> = {};
  projectTitles: Record<number, string> = {};
  stats: ReviewStats | null = null;
  searchTerm = '';
  ratingFilter: number | null = null;
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  pageSizes = [5, 10, 20, 50];
  responsesByReview: Record<number, ReviewResponseItem[]> = {};
  newMessageByReview: Record<number, string> = {};
  sendingReviewId: number | null = null;
  messageToDelete: ReviewResponseItem | null = null;
  deleteMessageReview: Review | null = null;
  deletingMessage = false;
  private searchSubject = new Subject<string>();
  private pollTimer: ReturnType<typeof setInterval> | null = null;

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
        this.pollTimer = setInterval(() => {
          if (this.reviews.length > 0) this.loadResponsesForReviews();
        }, POLL_INTERVAL_MS);
      } else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
    if (this.pollTimer) clearInterval(this.pollTimer);
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
      .getByReviewerIdPage(this.currentUser.id, {
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
            this.loadRevieweeNames();
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
    this.reviewService.getStatsByReviewer(this.currentUser.id).subscribe((s) => {
      this.stats = s ?? null;
      this.cdr.detectChanges();
    });
  }

  private loadRevieweeNames(): void {
    const ids = [...new Set(this.reviews.map((r) => r.revieweeId))];
    ids.forEach((id) => {
      if (this.revieweeNames[id]) return;
      this.userService.getById(id).subscribe((u) => {
        if (u) this.revieweeNames[id] = `${u.firstName} ${u.lastName}`.trim() || u.email;
        else this.revieweeNames[id] = `User #${id}`;
        this.cdr.detectChanges();
      });
    });
  }

  getRevieweeName(r: Review): string {
    return this.revieweeNames[r.revieweeId] ?? `User #${r.revieweeId}`;
  }

  getProjectTitle(r: Review): string {
    return this.projectTitles[r.projectId] ?? `Project #${r.projectId}`;
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
    if (resp.respondentId === r.revieweeId) return this.getRevieweeName(r);
    return 'User #' + resp.respondentId;
  }

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

  private loadProjectTitles(): void {
    const ids = [...new Set(this.reviews.map((r) => r.projectId))];
    ids.forEach((id) => {
      if (this.projectTitles[id]) return;
      this.projectService.getById(id).subscribe((p) => {
        this.projectTitles[id] = p?.title ?? `Project #${id}`;
        this.cdr.detectChanges();
      });
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

  openDeleteModal(review: Review): void {
    this.reviewToDelete = review;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.reviewToDelete = null;
  }

  doDelete(): void {
    if (!this.reviewToDelete?.id) return;
    this.deleting = true;
    this.reviewService.delete(this.reviewToDelete.id).subscribe((ok) => {
      this.deleting = false;
      this.reviewToDelete = null;
      if (ok) {
        this.load();
        this.loadStats();
      } else this.errorMessage = 'Failed to delete review.';
      this.cdr.detectChanges();
    });
  }
}
