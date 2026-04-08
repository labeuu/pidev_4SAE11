import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  GamificationService,
  LeaderboardEntry,
  isTopFreelancerFlag
} from '../../../core/services/gamification.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Card],
  templateUrl: './leaderboard.html',
  styleUrl: './leaderboard.scss',
})
export class LeaderboardPage implements OnInit {
  private gamification = inject(GamificationService);

  loading = true;
  leaderboard: LeaderboardEntry[] = [];
  top3: LeaderboardEntry[] = [];
  others: LeaderboardEntry[] = [];

  ngOnInit(): void {
    this.loadLeaderboard();
  }

  loadLeaderboard(): void {
    this.loading = true;
    this.gamification.getLeaderboard(50)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (data) => {
          this.leaderboard = data;
          this.splitData();
        },
        error: () => {
          // Fallback or error message could be added
        }
      });
  }

  private splitData(): void {
    // Rankings 1, 2, 3
    this.top3 = this.leaderboard.slice(0, 3);
    // Rankings 4+
    this.others = this.leaderboard.slice(3);
  }

  isTop(entry: LeaderboardEntry): boolean {
    return isTopFreelancerFlag(entry as any);
  }

  podiumOrder(entry: LeaderboardEntry): number {
    // 1st (rank 1) -> center (2)
    // 2nd (rank 2) -> left (1)
    // 3rd (rank 3) -> right (3)
    if (entry.rank === 1) return 2;
    if (entry.rank === 2) return 1;
    return 3;
  }

  sortedTop3(): LeaderboardEntry[] {
    // Return in order [2nd, 1st, 3rd] for visual podium
    const result = [...this.top3];
    return result.sort((a, b) => this.podiumOrder(a) - this.podiumOrder(b));
  }
}
