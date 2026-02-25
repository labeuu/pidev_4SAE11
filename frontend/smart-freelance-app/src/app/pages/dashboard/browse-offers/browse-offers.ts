import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer } from '../../../core/services/offer.service';

@Component({
  selector: 'app-browse-offers',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './browse-offers.html',
  styleUrl: './browse-offers.scss',
})
export class BrowseOffers implements OnInit {
  offers: Offer[] = [];
  recommendedOffers: Offer[] = [];
  loading = false;
  loadingRecommendations = false;
  page = 0;
  size = 12;
  totalElements = 0;
  totalPages = 0;
  currentUser: User | null = null;

  constructor(
    private offerService: OfferService,
    private auth: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe((u) => {
        this.currentUser = u ?? null;
        if (this.currentUser?.id) this.loadRecommendations();
        this.cdr.detectChanges();
      });
    }
    this.load();
  }

  loadRecommendations(): void {
    if (!this.currentUser?.id) return;
    this.loadingRecommendations = true;
    this.offerService.getRecommendedOffers(this.currentUser.id, 8).subscribe((list) => {
      this.recommendedOffers = list ?? [];
      this.loadingRecommendations = false;
      this.cdr.detectChanges();
    });
  }

  load(): void {
    this.loading = true;
    this.offerService.getActiveOffers(this.page, this.size).subscribe((p) => {
      this.offers = p.content ?? [];
      this.totalElements = p.totalElements ?? 0;
      this.totalPages = p.totalPages ?? 0;
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  prev(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  next(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.load();
    }
  }
}
