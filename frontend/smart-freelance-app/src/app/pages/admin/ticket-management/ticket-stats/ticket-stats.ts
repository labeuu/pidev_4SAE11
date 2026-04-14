import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { TicketService, TicketStats, MonthlyTicketCount } from '../../../../core/services/ticket.service';
import { messageFromHttpError } from '../../../../core/utils/http-error.util';

Chart.register(...registerables);

@Component({
  selector: 'app-ticket-stats-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective],
  templateUrl: './ticket-stats.html',
  styleUrl: './ticket-stats.scss',
})
export class TicketStatsDashboard implements OnInit {
  loading = true;
  exportingPdf = false;
  errorMessage = '';
  stats: TicketStats | null = null;
  monthly: MonthlyTicketCount[] = [];

  barData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Tickets',
        backgroundColor: 'rgba(227, 126, 51, 0.85)',
        borderColor: 'rgba(227, 126, 51, 1)',
        borderWidth: 1,
        borderRadius: 6,
      },
    ],
  };

  barOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { maxRotation: 45, minRotation: 0 } },
      y: { beginAtZero: true, ticks: { stepSize: 1 } },
    },
  };

  constructor(
    private ticketService: TicketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    forkJoin({
      stats: this.ticketService.getStats(),
      monthly: this.ticketService.getMonthlyStats(),
    })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: ({ stats, monthly }) => {
          this.stats = stats;
          this.monthly = monthly;
          const labels = monthly.map((m) => `${m.year}-${String(m.month).padStart(2, '0')}`);
          const data = monthly.map((m) => m.count);
          this.barData = {
            ...this.barData,
            labels,
            datasets: [{ ...this.barData.datasets[0], data }],
          };
        },
        error: (err: unknown) => {
          this.errorMessage = messageFromHttpError(err, 'Failed to load ticket statistics.');
          this.stats = null;
          this.monthly = [];
        },
      });
  }

  avgResponseLabel(): string {
    const v = this.stats?.averageResponseTimeMinutes;
    if (v == null) return '—';
    return `${v.toFixed(1)} min`;
  }

  downloadPdf(): void {
    this.exportingPdf = true;
    this.ticketService
      .exportPdf()
      .pipe(
        finalize(() => {
          this.exportingPdf = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'tickets-report.pdf';
          a.click();
          URL.revokeObjectURL(url);
        },
        error: (err: unknown) => {
          this.errorMessage = messageFromHttpError(err, 'Failed to download PDF.');
        },
      });
  }
}
