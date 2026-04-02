import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import {
  NotificationService,
  NotificationItem,
} from '../../../core/services/notification.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, Card],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications implements OnInit {
  notifications = signal<NotificationItem[]>([]);
  loading = signal(true);
  errorMessage = signal('');
  deletingId = signal<string | null>(null);

  unreadCount = computed(() =>
    this.notifications().filter((n) => !n.read).length
  );

  constructor(
    public auth: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const userId = this.auth.getUserId();
    if (userId == null) {
      this.errorMessage.set('You must be logged in to view notifications.');
      this.loading.set(false);
      return;
    }
    this.errorMessage.set('');
    this.loading.set(true);
    this.notificationService.getByUserId(userId).subscribe({
      next: (list) => {
        this.notifications.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load notifications. Please try again.');
        this.loading.set(false);
      },
    });
  }

  markRead(n: NotificationItem): void {
    if (n.read) return;
    this.notificationService.markRead(n.id).subscribe({
      next: (updated) => {
        if (updated) {
          this.notifications.update((list) =>
            list.map((item) =>
              item.id === n.id ? { ...item, read: true } : item
            )
          );
        }
      },
    });
  }

  deleteNotification(n: NotificationItem): void {
    this.deletingId.set(n.id);
    this.notificationService.delete(n.id).subscribe({
      next: (ok) => {
        this.deletingId.set(null);
        if (ok) {
          this.notifications.update((list) => list.filter((i) => i.id !== n.id));
        }
      },
      error: () => this.deletingId.set(null),
    });
  }

  /** Navigate to the relevant page for this notification. */
  goToNotification(n: NotificationItem): void {
    const { route, queryParams } = this.notificationService.getNotificationRoute(
      n,
      this.auth.isClient()
    );
    this.router.navigate([route], { queryParams });
    if (!n.read) this.markRead(n);
  }

  formatDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} h ago`;
    if (diffDays < 7) return `${diffDays} day(s) ago`;
    return d.toLocaleDateString(undefined, {
      day: 'numeric',
      month: 'short',
      year: d.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
    });
  }

  typeLabel(type: string): string {
    if (type === 'PROGRESS_UPDATE') return 'Progress update';
    if (type === 'PROGRESS_COMMENT') return 'Comment';
    if (type === 'REVIEW_RESPONSE') return 'Review response';
    if (type === 'TASK_STATUS_UPDATE') return 'Task update';
    if (type === 'TASK_PRIORITY_ESCALATED') return 'Task priority';
    if (type === 'PROGRESS_NEXT_DUE_OVERDUE') return 'Progress due overdue';
    if (type === 'GAMIFICATION_FAST_RESPONDER') return 'Achievement';
    if (type === 'GAMIFICATION_TOP_FREELANCER') return 'Top freelancer';
    if (type === 'GAMIFICATION_TOP_FREELANCER_REVOKED') return 'Rank update';
    return type || 'Notification';
  }
}
