import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ReviewService, Review, ReviewStats } from '../../../core/services/review.service';
import { StarRating } from '../../../shared/components/star-rating/star-rating';

@Component({
  selector: 'app-review-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StarRating],
  templateUrl: './review-management.html',
  styleUrl: './review-management.scss',
})
export class ReviewManagement implements OnInit, OnDestroy {
  reviews: Review[] = [];
  loading = true;
  errorMessage = '';
  reviewToDelete: Review | null = null;
  deleting = false;
  addForm: FormGroup;
  editForm: FormGroup;
  addModalOpen = false;
  editingReview: Review | null = null;
  saving = false;
  adding = false;
  stats: ReviewStats | null = null;
  searchTerm = '';
  ratingFilter: number | null = null;
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  pageSizes = [5, 10, 20, 50];
  private searchSubject = new Subject<string>();

  constructor(
    private reviewService: ReviewService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      reviewerId: [null as number | null, [Validators.required, Validators.min(1)]],
      revieweeId: [null as number | null, [Validators.required, Validators.min(1)]],
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: [''],
    });
    this.editForm = this.fb.group({
      reviewerId: [null as number | null, [Validators.required, Validators.min(1)]],
      revieweeId: [null as number | null, [Validators.required, Validators.min(1)]],
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: [''],
    });
  }

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
      this.searchTerm = term;
      this.page = 0;
      this.loadReviews();
    });
    this.loadReviews();
    this.loadStats();
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  onRatingFilterChange(): void {
    this.page = 0;
    this.loadReviews();
  }

  loadStats(): void {
    this.reviewService.getStats().subscribe((s) => {
      this.stats = s ?? null;
      this.cdr.detectChanges();
    });
  }

  loadReviews(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reviewService
      .getPage({
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

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadReviews();
  }

  setPageSize(newSize: number): void {
    this.size = newSize;
    this.page = 0;
    this.loadReviews();
  }

  get roundedAverageRating(): number {
    return this.stats ? Math.round(this.stats.averageRating) : 0;
  }

  getRatingCount(r: number): number {
    return this.stats?.countByRating?.[r] ?? 0;
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

  openAdd(): void {
    this.errorMessage = '';
    this.addForm.reset({
      reviewerId: null,
      revieweeId: null,
      projectId: null,
      rating: 5,
      comment: '',
    });
    this.addModalOpen = true;
  }

  closeAdd(): void {
    if (!this.adding) this.addModalOpen = false;
  }

  saveAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    const review: Review = {
      reviewerId: Number(v.reviewerId),
      revieweeId: Number(v.revieweeId),
      projectId: Number(v.projectId),
      rating: Number(v.rating),
      comment: v.comment?.trim() || null,
    };
    this.adding = true;
    this.reviewService.create(review).subscribe({
      next: (created) => {
        this.adding = false;
        if (created) {
          this.addModalOpen = false;
          this.loadReviews();
          this.loadStats();
        } else this.errorMessage = 'Failed to create review.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.adding = false;
        this.errorMessage = 'Failed to create review.';
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(review: Review): void {
    this.editingReview = review;
    this.reviewToDelete = null;
    this.editForm.patchValue({
      reviewerId: review.reviewerId,
      revieweeId: review.revieweeId,
      projectId: review.projectId,
      rating: review.rating,
      comment: review.comment ?? '',
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingReview = null;
  }

  saveEdit(): void {
    if (!this.editingReview?.id || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const payload: Partial<Review> = {
      reviewerId: Number(v.reviewerId),
      revieweeId: Number(v.revieweeId),
      projectId: Number(v.projectId),
      rating: Number(v.rating),
      comment: v.comment?.trim() || null,
    };
    this.saving = true;
    this.reviewService.update(this.editingReview.id, payload).subscribe({
      next: (updated) => {
        this.saving = false;
        if (updated) {
          const idx = this.reviews.findIndex((r) => r.id === this.editingReview!.id);
          if (idx !== -1) this.reviews[idx] = { ...this.reviews[idx], ...updated };
          this.editingReview = null;
        } else this.errorMessage = 'Failed to update review.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update review.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(review: Review): void {
    this.reviewToDelete = review;
    this.closeEdit();
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.reviewToDelete = null;
  }

  doDelete(): void {
    if (!this.reviewToDelete?.id) return;
    this.deleting = true;
    this.reviewService.delete(this.reviewToDelete.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.reviewToDelete = null;
        if (ok) {
          this.loadReviews();
          this.loadStats();
        } else this.errorMessage = 'Failed to delete review.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete review.';
        this.cdr.detectChanges();
      },
    });
  }
}
