import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { OfferService, type DashboardStats } from '../../../core/services/offer.service';
import { Card } from '../../../shared/components/card/card';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard-home',
  imports: [Card, CommonModule, RouterLink],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
  standalone: true,
})
export class DashboardHome implements OnInit {
  stats: DashboardStats | null = null;
  loading = false;
  error = '';

  constructor(
    public auth: AuthService,
    private offerService: OfferService
  ) {}

  ngOnInit(): void {
    if (this.auth.isFreelancer()) {
      this.loadDashboardStats();
    }
  }

  loadDashboardStats(): void {
    const userId = this.auth.getUserId();
    if (userId == null) return;
    this.loading = true;
    this.error = '';
    this.offerService.getFreelancerDashboardStats(userId).subscribe({
      next: (data) => {
        this.stats = data ?? null;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load dashboard statistics.';
        this.loading = false;
      },
    });
  }

  formatCurrency(value: number | undefined): string {
    if (value == null) return '0';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0 }).format(value);
  }
}
