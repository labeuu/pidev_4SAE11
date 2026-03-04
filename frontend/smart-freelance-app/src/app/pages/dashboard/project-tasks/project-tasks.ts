import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, of, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { TaskService, Task, TaskFilterParams, PageResponse } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { Card } from '../../../shared/components/card/card';

Chart.register(...registerables);

const STATUS_ORDER = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'] as const;
const PRIORITY_ORDER = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const;
const STATUS_COLORS = ['#94a3b8', '#3b82f6', '#f59e0b', '#10b981', '#ef4444'];
const PRIORITY_COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#ef4444'];

@Component({
  selector: 'app-project-tasks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card, BaseChartDirective],
  templateUrl: './project-tasks.html',
  styleUrl: './project-tasks.scss',
})
export class ProjectTasks implements OnInit, OnDestroy {
  myProjects: Project[] = [];
  selectedProjectId: number | null = null;
  tasks: Task[] = [];
  loading = true;
  errorMessage = '';
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  projectForm: FormGroup;
  filterForm: FormGroup;
  private searchSubject$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  statusChartData: ChartData<'doughnut', number[], string> = {
    labels: [...STATUS_ORDER],
    datasets: [{ data: [0, 0, 0, 0, 0], backgroundColor: STATUS_COLORS, borderWidth: 1 }],
  };
  statusChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
  };

  priorityChartData: ChartData<'bar', number[], string> = {
    labels: [...PRIORITY_ORDER],
    datasets: [{ data: [0, 0, 0, 0], label: 'Tasks', backgroundColor: PRIORITY_COLORS }],
  };
  priorityChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { beginAtZero: true, ticks: { stepSize: 1 } },
    },
  };

  constructor(
    private taskService: TaskService,
    public auth: AuthService,
    private projectService: ProjectService,
    private userService: UserService,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.projectForm = this.fb.group({ projectId: [null as number | null] });
    this.filterForm = this.fb.group({ search: [''] });
  }

  get doneCount(): number {
    return this.tasks.filter((t) => t.status === 'DONE').length;
  }

  get completionPercentage(): number {
    if (this.tasks.length === 0) return 0;
    return (this.doneCount / this.tasks.length) * 100;
  }

  get inProgressCount(): number {
    return this.tasks.filter(
      (t) => t.status === 'IN_PROGRESS' || t.status === 'IN_REVIEW'
    ).length;
  }

  get overdueCount(): number {
    const today = new Date().toISOString().slice(0, 10);
    return this.tasks.filter(
      (t) =>
        t.dueDate &&
        t.dueDate < today &&
        t.status !== 'DONE' &&
        t.status !== 'CANCELLED'
    ).length;
  }

  ngOnInit(): void {
    this.setupSearchDebounce();
    const userId = this.auth.getUserId();
    if (userId == null) {
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }
    this.projectService.getByClientId(userId).subscribe({
      next: (projects) => {
        this.myProjects = projects ?? [];
        if (this.myProjects.length > 0 && !this.selectedProjectId) {
          const projectIdParam = this.route.snapshot.queryParamMap.get('projectId');
          if (projectIdParam) {
            const id = Number(projectIdParam);
            const found = this.myProjects.some((p) => p.id === id);
            this.selectedProjectId = found ? id : this.myProjects[0].id ?? null;
          } else {
            this.selectedProjectId = this.myProjects[0].id ?? null;
          }
          this.projectForm.patchValue({ projectId: this.selectedProjectId });
          this.loadTasks();
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
    const isAdmin = this.auth.getUserRole() === 'ADMIN';
    (isAdmin ? this.userService.getAll() : of([])).pipe(
      takeUntil(this.destroy$)
    ).subscribe((users) => {
      this.freelancersForAssign = users?.filter((u) => u.role === 'FREELANCER') ?? [];
    });
  }

  onProjectChange(): void {
    this.selectedProjectId = this.projectForm.get('projectId')?.value ?? null;
    this.page = 0;
    this.loadTasks();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearchDebounce(): void {
    this.searchSubject$.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.page = 0;
      this.loadTasks();
    });
  }

  onSearchInput(): void {
    this.searchSubject$.next(this.filterForm.get('search')?.value?.trim() ?? '');
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadTasks();
  }

  loadTasks(): void {
    if (!this.selectedProjectId) {
      this.tasks = [];
      this.totalElements = 0;
      this.totalPages = 0;
      this.cdr.detectChanges();
      return;
    }
    this.loading = true;
    const params: TaskFilterParams = {
      page: this.page,
      size: this.size,
      sort: 'createdAt,desc',
      projectId: this.selectedProjectId,
      search: this.filterForm.get('search')?.value?.trim() || null,
    };
    this.taskService.getFilteredTasks(params).subscribe({
      next: (p: PageResponse<Task>) => {
        this.tasks = p.content ?? [];
        this.totalElements = p.totalElements ?? 0;
        this.totalPages = p.totalPages ?? 0;
        this.updateCharts();
        this.loading = false;
        this.errorMessage = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load tasks.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getProjectTitle(p: Project): string {
    return p.title?.trim() || `Project ${p.id}`;
  }

  freelancersForAssign: { id?: number; firstName?: string; lastName?: string }[] = [];

  getAssigneeName(id: number | null): string {
    if (id == null) return '—';
    const u = this.freelancersForAssign.find((f) => f.id === id);
    return u ? `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || `User ${id}` : `User ${id}`;
  }

  formatDueDate(d: string | null): string {
    return d ? new Date(d).toLocaleDateString() : '—';
  }

  get isClient(): boolean {
    return this.auth.isClient();
  }

  private updateCharts(): void {
    const statusCounts = STATUS_ORDER.map(
      (s) => this.tasks.filter((t) => t.status === s).length
    );
    this.statusChartData = {
      ...this.statusChartData,
      datasets: [{
        ...this.statusChartData.datasets[0],
        data: statusCounts,
      }],
    };

    const priorityCounts = PRIORITY_ORDER.map(
      (p) => this.tasks.filter((t) => t.priority === p).length
    );
    this.priorityChartData = {
      ...this.priorityChartData,
      datasets: [{
        ...this.priorityChartData.datasets[0],
        data: priorityCounts,
      }],
    };
  }
}
