import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { finalize } from 'rxjs/operators';

import { Card } from '../../../shared/components/card/card';
import { JobService, JobAdminStats } from '../../../core/services/job.service';

Chart.register(...registerables);

// Status → display colour mapping
const STATUS_COLORS: Record<string, string> = {
  OPEN:        '#10B981',
  IN_PROGRESS: '#3B82F6',
  FILLED:      '#E37E33',
  CANCELLED:   '#EF4444',
};

@Component({
  selector: 'app-freelancia-job-stats',
  standalone: true,
  imports: [CommonModule, Card, BaseChartDirective],
  templateUrl: './freelancia-job-stats.html',
  styleUrl:    './freelancia-job-stats.scss',
})
export class FreelanciaJobStats implements OnInit {

  loading = true;
  stats: JobAdminStats | null = null;

  // ── Line: jobs posted per month ────────────────────────────────────────────
  monthlyChartData: ChartData<'line'> = {
    labels:   [],
    datasets: [{ data: [], label: 'Jobs posted', borderColor: '#E37E33',
                 backgroundColor: 'rgba(227,126,51,0.12)', fill: true,
                 tension: 0.4, pointRadius: 4 }],
  };
  monthlyChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
  };

  // ── Pie: jobs by status ─────────────────────────────────────────────────────
  statusChartData: ChartData<'pie'> = {
    labels:   [],
    datasets: [{ data: [], backgroundColor: [] }],
  };
  statusChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
  };

  // ── Bar: top-5 applied jobs ─────────────────────────────────────────────────
  topJobsChartData: ChartData<'bar'> = {
    labels:   [],
    datasets: [{ data: [], label: 'Applications', backgroundColor: '#3B82F6', borderRadius: 4 }],
  };
  topJobsChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true, ticks: { stepSize: 1 } } },
  };

  constructor(
    private jobService: JobService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.jobService.getAdminStats()
      .pipe(finalize(() => { this.loading = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: data => {
          this.stats = data;
          if (data) this.buildCharts(data);
        },
      });
  }

  private buildCharts(data: JobAdminStats): void {

    // ── Line ──────────────────────────────────────────────────────────────────
    this.monthlyChartData = {
      labels:   data.jobsPerMonth.map(m => m.month),
      datasets: [{
        data:            data.jobsPerMonth.map(m => m.count),
        label:           'Jobs posted',
        borderColor:     '#E37E33',
        backgroundColor: 'rgba(227,126,51,0.12)',
        fill:            true,
        tension:         0.4,
        pointRadius:     4,
      }],
    };

    // ── Pie ───────────────────────────────────────────────────────────────────
    const statusEntries = Object.entries(data.jobsByStatus);
    this.statusChartData = {
      labels:   statusEntries.map(([k]) => k),
      datasets: [{
        data:            statusEntries.map(([, v]) => v),
        backgroundColor: statusEntries.map(([k]) => STATUS_COLORS[k] ?? '#6B7280'),
        hoverOffset:     4,
      }],
    };

    // ── Bar ───────────────────────────────────────────────────────────────────
    this.topJobsChartData = {
      labels:   data.top5Jobs.map(j => j.jobTitle),
      datasets: [{
        data:            data.top5Jobs.map(j => j.applicationsCount),
        label:           'Applications',
        backgroundColor: '#3B82F6',
        borderRadius:    4,
      }],
    };
  }
}
