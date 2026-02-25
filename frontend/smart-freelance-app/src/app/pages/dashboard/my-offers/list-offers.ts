import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import {
  OfferService,
  Offer,
  OfferFilterRequest,
  OfferStatus,
  PageResponse,
  AcceptanceRate,
  MonthlyEvolution,
} from '../../../core/services/offer.service';

const DEBOUNCE_MS = 350;
const PAGE_SIZE = 10;

@Component({
  selector: 'app-list-offers',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './list-offers.html',
  styleUrl: './list-offers.scss',
})
export class ListOffers implements OnInit, OnDestroy {
  offers: Offer[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  offerToDelete: Offer | null = null;
  deleting = false;
  offerToPublish: Offer | null = null;
  publishing = false;

  // Filtres cumulables (recherche dynamique + filtrage avancé)
  keyword = '';
  category = '';
  offerStatus: OfferStatus | '' = '';
  createdAtFrom = '';
  createdAtTo = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  private keyword$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  // Statistiques (calcul backend, affichage uniquement)
  statsByStatus: Record<string, number> | null = null;
  acceptanceRate: AcceptanceRate | null = null;
  monthlyEvolution: MonthlyEvolution | null = null;
  statsLoading = false;
  selectedYear = new Date().getFullYear();

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
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
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (this.currentUser) {
        this.setupSearchDebounce();
        this.loadOffers();
        this.loadStats();
      } else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearchDebounce(): void {
    this.keyword$
      .pipe(debounceTime(DEBOUNCE_MS), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadOffers();
      });
  }

  onKeywordInput(): void {
    this.keyword$.next(this.keyword);
  }

  buildFilter(): OfferFilterRequest {
    const f: OfferFilterRequest = {
      freelancerId: this.currentUser?.id,
      page: this.currentPage,
      size: PAGE_SIZE,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    };
    if (this.keyword?.trim()) f.keyword = this.keyword.trim();
    if (this.category?.trim()) f.category = this.category.trim();
    if (this.offerStatus) f.offerStatus = this.offerStatus as OfferStatus;
    if (this.createdAtFrom) f.createdAtFrom = this.createdAtFrom;
    if (this.createdAtTo) f.createdAtTo = this.createdAtTo;
    if (this.minPrice != null && this.minPrice !== undefined) f.minPrice = this.minPrice;
    if (this.maxPrice != null && this.maxPrice !== undefined) f.maxPrice = this.maxPrice;
    return f;
  }

  loadOffers(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.offerService.searchOffers(this.buildFilter()).subscribe({
      next: (res: PageResponse<Offer>) => {
        this.offers = res?.content ?? [];
        this.totalElements = res?.totalElements ?? 0;
        this.totalPages = res?.totalPages ?? 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load offers.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadStats(): void {
    if (!this.currentUser?.id) return;
    this.statsLoading = true;
    this.offerService.getOffersCountByStatus(this.currentUser.id).subscribe({
      next: (res) => {
        this.statsByStatus = res?.countByStatus ?? null;
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
    });
    this.offerService.getAcceptanceRate(this.currentUser.id).subscribe({
      next: (res) => {
        this.acceptanceRate = res ?? null;
        this.cdr.detectChanges();
      },
    });
    this.offerService.getMonthlyEvolution(this.currentUser.id, this.selectedYear).subscribe({
      next: (res) => {
        this.monthlyEvolution = res ?? null;
        this.cdr.detectChanges();
      },
    });
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadOffers();
  }

  clearFilters(): void {
    this.keyword = '';
    this.category = '';
    this.offerStatus = '';
    this.createdAtFrom = '';
    this.createdAtTo = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.currentPage = 0;
    this.loadOffers();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadOffers();
  }

  refreshMonthlyEvolution(): void {
    if (!this.currentUser?.id) return;
    this.offerService.getMonthlyEvolution(this.currentUser.id, this.selectedYear).subscribe({
      next: (res) => {
        this.monthlyEvolution = res ?? null;
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(offer: Offer): void {
    this.offerToDelete = offer;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.offerToDelete = null;
  }

  doDelete(): void {
    if (!this.offerToDelete || !this.currentUser?.id) return;
    this.deleting = true;
    this.offerService.deleteOffer(this.offerToDelete.id, this.currentUser.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.offerToDelete = null;
        if (ok) {
          this.loadOffers();
          this.loadStats();
        } else this.errorMessage = 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.deleting = false;
        this.offerToDelete = null;
        this.errorMessage = err?.error?.message || 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
    });
  }

  openPublishModal(offer: Offer): void {
    this.offerToPublish = offer;
  }

  closePublishModal(): void {
    if (!this.publishing) this.offerToPublish = null;
  }

  doPublish(): void {
    if (!this.offerToPublish || !this.currentUser?.id) return;
    this.publishing = true;
    this.offerService.publishOffer(this.offerToPublish.id, this.currentUser.id).subscribe((updated) => {
      this.publishing = false;
      this.offerToPublish = null;
      if (updated) {
        this.loadOffers();
        this.loadStats();
      } else this.errorMessage = 'Failed to publish offer.';
      this.cdr.detectChanges();
    });
  }

  totalOffersByStatus(): number {
    if (!this.statsByStatus) return 0;
    return Object.values(this.statsByStatus).reduce((a, b) => a + b, 0);
  }

  /** Slug pour la classe CSS du badge (ex: AVAILABLE → available, IN_PROGRESS → in-progress). */
  statusSlug(key: string): string {
    return (key || '').toLowerCase().replace(/_/g, '-');
  }

  formatRate(rate: number): string {
    return (rate * 100).toFixed(1) + '%';
  }

  monthName(month: number): string {
    const names = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    return names[month - 1] ?? '';
  }

  get maxMonthCount(): number {
    if (!this.monthlyEvolution?.months?.length) return 1;
    return Math.max(1, ...this.monthlyEvolution.months.map((m) => m.count));
  }

  get yearOptions(): number[] {
    const y = new Date().getFullYear();
    return [y - 2, y - 1, y, y + 1];
  }
}
