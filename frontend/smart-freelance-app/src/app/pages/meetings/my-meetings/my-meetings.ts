import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MeetingService } from '../../../core/services/meeting.service';
import { AuthService } from '../../../core/services/auth.service';
import { Meeting, MeetingStatus } from '../../../core/models/meeting.models';

@Component({
  selector: 'app-my-meetings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-meetings.html',
  styleUrl: './my-meetings.scss',
})
export class MyMeetings implements OnInit {
  meetings = signal<Meeting[]>([]);
  loading = signal(true);
  activeTab = signal<'all' | MeetingStatus>('all');

  isClient = computed(() => this.auth.isClient());

  filtered = computed(() => {
    const tab = this.activeTab();
    if (tab === 'all') return this.meetings();
    return this.meetings().filter(m => m.status === tab);
  });

  tabs: { label: string; value: 'all' | MeetingStatus }[] = [
    { label: 'All', value: 'all' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Accepted', value: 'ACCEPTED' },
    { label: 'Completed', value: 'COMPLETED' },
    { label: 'Cancelled', value: 'CANCELLED' },
  ];

  constructor(
    private meetingService: MeetingService,
    private auth: AuthService,
  ) {}

  ngOnInit() {
    this.meetingService.getMyMeetings().subscribe(list => {
      this.meetings.set(list.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime()));
      this.loading.set(false);
    });
  }

  statusClass(status: MeetingStatus): string {
    return {
      PENDING: 'badge-warning',
      ACCEPTED: 'badge-success',
      DECLINED: 'badge-danger',
      CANCELLED: 'badge-secondary',
      COMPLETED: 'badge-info',
    }[status] ?? 'badge-secondary';
  }

  statusIcon(status: MeetingStatus): string {
    return {
      PENDING: '⏳',
      ACCEPTED: '✅',
      DECLINED: '❌',
      CANCELLED: '🚫',
      COMPLETED: '✔',
    }[status] ?? '';
  }

  typeIcon(type: string): string {
    return { VIDEO_CALL: '📹', VOICE_CALL: '📞', IN_PERSON: '🤝' }[type] ?? '📅';
  }

  formatDate(dt: string): string {
    return new Date(dt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatTime(dt: string): string {
    return new Date(dt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }
}
