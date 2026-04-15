import { Component, OnInit, signal, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { MeetingService } from '../../../core/services/meeting.service';
import { AuthService } from '../../../core/services/auth.service';
import { Meeting, MeetingStats, MeetingStatus } from '../../../core/models/meeting.models';

Chart.register(...registerables);

@Component({
  selector: 'app-my-meetings',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective],
  templateUrl: './my-meetings.html',
  styleUrl: './my-meetings.scss',
})
export class MyMeetings implements OnInit {
  meetings = signal<Meeting[]>([]);
  stats = signal<MeetingStats | null>(null);
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

  // Chart
  doughnutData: ChartData<'doughnut'> = {
    labels: ['Pending', 'Accepted', 'Completed', 'Cancelled', 'Declined'],
    datasets: [{
      data: [0, 0, 0, 0, 0],
      backgroundColor: ['#f59e0b', '#10b981', '#3b82f6', '#9ca3af', '#ef4444'],
      hoverOffset: 6,
      borderWidth: 2,
    }],
  };

  doughnutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
        labels: { font: { size: 12 }, padding: 12 },
      },
      tooltip: {
        callbacks: {
          label: ctx => ` ${ctx.label}: ${ctx.parsed}`,
        },
      },
    },
    cutout: '65%',
  };

  constructor(
    private meetingService: MeetingService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.meetingService.getMyMeetings().subscribe(list => {
      this.meetings.set(list.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime()));
      this.loading.set(false);
    });

    this.meetingService.getStats().subscribe(s => {
      this.stats.set(s);
      this.doughnutData = {
        ...this.doughnutData,
        datasets: [{
          ...this.doughnutData.datasets[0],
          data: [s.pending, s.accepted, s.completed, s.cancelled, s.declined],
        }],
      };
      this.cdr.detectChanges();
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
