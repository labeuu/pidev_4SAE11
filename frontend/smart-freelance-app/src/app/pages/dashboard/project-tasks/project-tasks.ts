import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  Subject,
  of,
  debounceTime,
  distinctUntilChanged,
  takeUntil,
  map,
  forkJoin,
  catchError,
} from 'rxjs';
import {
  TaskService,
  Task,
  TaskFilterParams,
  PageResponse,
  TaskStatsExtendedDto,
  TaskStatus,
  TaskPriority,
  Subtask,
} from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { UserService, User } from '../../../core/services/user.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-project-tasks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './project-tasks.html',
  styleUrl: './project-tasks.scss',
})
export class ProjectTasks implements OnInit, OnDestroy {
  myProjects: Project[] = [];
  selectedProjectId: number | null = null;
  tasks: Task[] = [];
  loading = true;
  /** True while reloading when the list already had rows. */
  listRefreshing = false;
  errorMessage = '';
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  projectForm: FormGroup;
  filterForm: FormGroup;
  private destroy$ = new Subject<void>();

  extendedStats: TaskStatsExtendedDto | null = null;
  extendedStatsLoading = false;
  extendedStatsError = false;

  listViewMode: 'paginated' | 'overdue' = 'paginated';
  filterOpenTasksOnly = false;
  statsFocusKey: string | null = null;

  /** Expand/collapse root tasks to show subtasks (read-only for clients). */
  expandedByTaskId: Record<number, boolean> = {};
  subtasksByTaskId: Record<number, Subtask[]> = {};
  subtasksLoading: Record<number, boolean> = {};

  /** Resolved users for assignee display (clients fetch via getById; admins may also cache from getAll). */
  private assigneeCache: Record<number, User> = {};
  private assigneeIdsFetching = new Set<number>();

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
    this.filterForm = this.fb.group({
      search: [''],
      status: [''],
      priority: [''],
      sort: ['createdAt,desc'],
    });
  }

  get statsActiveOpen(): number {
    const s = this.extendedStats;
    if (!s) return 0;
    return (Number(s.todoCount) || 0) + (Number(s.inProgressCount) || 0) + (Number(s.inReviewCount) || 0);
  }

  get isClient(): boolean {
    return this.auth.isClient();
  }

  ngOnInit(): void {
    this.setupFilterReload();
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
          this.resetFiltersForProjectSwitch();
          this.loadExtendedStats();
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
    (isAdmin ? this.userService.getAll() : of([])).pipe(takeUntil(this.destroy$)).subscribe((users) => {
      this.freelancersForAssign = users?.filter((u) => u.role === 'FREELANCER') ?? [];
      for (const u of users ?? []) {
        if (u.id != null) this.assigneeCache[u.id] = u;
      }
      this.cdr.markForCheck();
    });
  }

  onProjectChange(): void {
    this.selectedProjectId = this.projectForm.get('projectId')?.value ?? null;
    this.page = 0;
    this.clearAssigneeDirectory();
    this.resetFiltersForProjectSwitch();
    this.loadExtendedStats();
    this.loadTasks();
  }

  private resetFiltersForProjectSwitch(): void {
    this.listViewMode = 'paginated';
    this.statsFocusKey = null;
    this.filterOpenTasksOnly = false;
    this.filterForm.patchValue(
      { search: '', status: '', priority: '', sort: 'createdAt,desc' },
      { emitEvent: false }
    );
  }

  private setupFilterReload(): void {
    this.filterForm.valueChanges
      .pipe(
        debounceTime(350),
        map(() => this.filterFingerprint()),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        if (this.listViewMode === 'overdue') {
          this.listViewMode = 'paginated';
        }
        const st = String(this.filterForm.get('status')?.value ?? '').trim();
        if (st) this.filterOpenTasksOnly = false;
        this.statsFocusKey = null;
        this.page = 0;
        if (this.selectedProjectId) this.loadTasks();
      });
  }

  private filterFingerprint(): string {
    const v = this.filterForm.getRawValue();
    return JSON.stringify({
      search: String(v.search ?? '').trim(),
      status: String(v.status ?? ''),
      priority: String(v.priority ?? ''),
      sort: String(v.sort ?? ''),
      openOnly: this.filterOpenTasksOnly,
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadTasks();
  }

  loadExtendedStats(): void {
    if (!this.selectedProjectId) {
      this.extendedStats = null;
      this.extendedStatsLoading = false;
      return;
    }
    this.extendedStatsLoading = true;
    this.extendedStatsError = false;
    this.taskService.getExtendedStatsByProject(this.selectedProjectId).subscribe({
      next: (data) => {
        this.extendedStatsLoading = false;
        this.extendedStats = data;
        if (data == null) this.extendedStatsError = true;
        this.cdr.markForCheck();
      },
      error: () => {
        this.extendedStatsLoading = false;
        this.extendedStatsError = true;
        this.cdr.markForCheck();
      },
    });
  }

  refreshAll(): void {
    this.loadExtendedStats();
    this.loadTasks();
  }

  loadTasks(): void {
    if (!this.selectedProjectId) {
      this.tasks = [];
      this.totalElements = 0;
      this.totalPages = 0;
      this.resetSubtaskUiState();
      this.cdr.detectChanges();
      return;
    }
    const hadRows = this.tasks.length > 0;
    this.listRefreshing = hadRows;
    this.loading = !hadRows;

    if (this.listViewMode === 'overdue') {
      this.taskService.getOverdueTasks(this.selectedProjectId, null).subscribe({
        next: (list) => {
          this.tasks = list ?? [];
          this.totalElements = this.tasks.length;
          this.totalPages = 1;
          this.loading = false;
          this.listRefreshing = false;
          this.errorMessage = '';
        this.resetSubtaskUiState();
        this.prefetchSubtasksForRoots();
        this.resolveMissingAssignees(this.collectAssigneeIdsFromTasks());
        this.cdr.detectChanges();
        },
        error: () => {
          this.errorMessage = 'Failed to load overdue tasks.';
          this.loading = false;
          this.listRefreshing = false;
          this.cdr.detectChanges();
        },
      });
      return;
    }

    this.loadTasksPaginated();
  }

  private loadTasksPaginated(): void {
    const v = this.filterForm.getRawValue();
    const st = String(v.status ?? '').trim();
    const pr = String(v.priority ?? '').trim();
    const useOpenOnly = this.filterOpenTasksOnly && !st;
    const params: TaskFilterParams = {
      page: this.page,
      size: this.size,
      sort: String(v.sort ?? '').trim() || 'createdAt,desc',
      projectId: this.selectedProjectId!,
      search: String(v.search ?? '').trim() || null,
      status: st ? (st as TaskStatus) : null,
      priority: pr ? (pr as TaskPriority) : null,
      openTasksOnly: useOpenOnly ? true : null,
    };
    this.taskService.getFilteredTasks(params).subscribe({
      next: (p: PageResponse<Task>) => {
        this.tasks = p.content ?? [];
        this.totalElements = p.totalElements ?? 0;
        this.totalPages = p.totalPages ?? 0;
        this.loading = false;
        this.listRefreshing = false;
        this.errorMessage = '';
        this.resetSubtaskUiState();
        this.prefetchSubtasksForRoots();
        this.resolveMissingAssignees(this.collectAssigneeIdsFromTasks());
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load tasks.';
        this.loading = false;
        this.listRefreshing = false;
        this.cdr.detectChanges();
      },
    });
  }

  freelancersForAssign: { id?: number; firstName?: string; lastName?: string }[] = [];

  private clearAssigneeDirectory(): void {
    this.assigneeCache = {};
    this.assigneeIdsFetching.clear();
  }

  private collectAssigneeIdsFromTasks(): number[] {
    return this.tasks.map((t) => t.assigneeId).filter((id): id is number => id != null);
  }

  private collectAssigneeIdsFromTasksAndSubtasks(): number[] {
    const ids = new Set<number>();
    for (const t of this.tasks) {
      if (t.assigneeId != null) ids.add(t.assigneeId);
    }
    for (const list of Object.values(this.subtasksByTaskId)) {
      for (const s of list) {
        if (s.assigneeId != null) ids.add(s.assigneeId);
      }
    }
    return [...ids];
  }

  /** Loads user profiles for assignee IDs so clients see freelancer names, not "User {id}". */
  private resolveMissingAssignees(ids: number[]): void {
    const unique = [...new Set(ids.filter((id) => Number.isFinite(id) && id > 0))];
    const toFetch = unique.filter(
      (id) => !this.assigneeCache[id] && !this.assigneeIdsFetching.has(id)
    );
    if (toFetch.length === 0) return;
    toFetch.forEach((id) => this.assigneeIdsFetching.add(id));
    forkJoin(
      toFetch.map((id) =>
        this.userService.getById(id).pipe(
          map((u) => ({ id, u })),
          catchError(() => of({ id, u: null as User | null }))
        )
      )
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pairs) => {
          for (const { id, u } of pairs) {
            this.assigneeIdsFetching.delete(id);
            if (u) this.assigneeCache[id] = u;
          }
          this.cdr.markForCheck();
        },
        error: () => {
          toFetch.forEach((id) => this.assigneeIdsFetching.delete(id));
        },
      });
  }

  getProjectTitle(p: Project): string {
    return p.title?.trim() || `Project ${p.id}`;
  }

  getAssigneeName(id: number | null): string {
    if (id == null) return '—';
    const cached = this.assigneeCache[id];
    if (cached) {
      const name = `${cached.firstName ?? ''} ${cached.lastName ?? ''}`.trim();
      if (name) return name;
    }
    const u = this.freelancersForAssign.find((f) => f.id === id);
    if (u) {
      const name = `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim();
      if (name) return name;
    }
    return `User ${id}`;
  }

  formatDueDate(d: string | null): string {
    return d ? new Date(d).toLocaleDateString() : '—';
  }

  formatPriorityLabel(priority: string | undefined | null): string {
    switch (priority) {
      case 'LOW':
        return 'Low';
      case 'MEDIUM':
        return 'Medium';
      case 'HIGH':
        return 'High';
      case 'URGENT':
        return 'Urgent';
      default:
        return '—';
    }
  }

  friendlyTaskStatus(status: string | undefined | null): string {
    switch (status) {
      case 'TODO':
        return 'To do';
      case 'IN_PROGRESS':
        return 'In progress';
      case 'IN_REVIEW':
        return 'In review';
      case 'DONE':
        return 'Done';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status ?? '—';
    }
  }

  private isOpenTaskStatus(status: string | undefined | null): boolean {
    return status !== 'DONE' && status !== 'CANCELLED';
  }

  isOverdueDueDate(dueDate: string | null | undefined, status: string | undefined | null): boolean {
    if (!dueDate || !this.isOpenTaskStatus(status)) return false;
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(dueDate).trim());
    if (!m) return false;
    const due = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    return due.getTime() < todayStart.getTime();
  }

  dueDateDisplayLine(dueDate: string | null | undefined, status: string | undefined | null): string {
    if (!dueDate) return 'No due date';
    const formatted = this.formatDueDate(dueDate);
    if (!this.isOpenTaskStatus(status)) return `Due ${formatted}`;
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(dueDate).trim());
    if (!m) return `Due ${formatted}`;
    const due = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const diffDays = Math.round((todayStart.getTime() - due.getTime()) / 86400000);
    if (diffDays === 0) return `Due today (${formatted})`;
    if (diffDays < 0) {
      const ahead = -diffDays;
      return ahead === 1 ? `Due tomorrow (${formatted})` : `Due in ${ahead} days (${formatted})`;
    }
    if (diffDays === 1) return `1 day overdue · was ${formatted}`;
    return `${diffDays} days overdue · was ${formatted}`;
  }

  taskRowTrackId(t: Task): string {
    return `${t.id}-${t.subtask ? 'st' : 'root'}-${t.parentTaskId ?? 'p'}`;
  }

  taskRowLooksLikeSubtask(t: Task): boolean {
    return !!t.subtask;
  }

  private resetSubtaskUiState(): void {
    this.expandedByTaskId = {};
    this.subtasksByTaskId = {};
    this.subtasksLoading = {};
  }

  private prefetchSubtasksForRoots(): void {
    const roots = this.tasks.filter((t) => t.id != null && !t.subtask).map((t) => t.id!);
    if (roots.length === 0) return;
    forkJoin(
      roots.map((id) =>
        this.taskService.listSubtasks(id).pipe(
          map((list) => ({ id, list: list ?? [] })),
          catchError(() => of({ id, list: [] as Subtask[] }))
        )
      )
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe((results) => {
        for (const { id, list } of results) {
          this.subtasksByTaskId[id] = list;
        }
        this.resolveMissingAssignees(this.collectAssigneeIdsFromTasksAndSubtasks());
        this.cdr.markForCheck();
      });
  }

  isRootExpanded(taskId: number | undefined): boolean {
    if (taskId == null) return false;
    return !!this.expandedByTaskId[taskId];
  }

  toggleRootExpanded(t: Task): void {
    const id = t.id;
    if (id == null || t.subtask) return;
    if (this.expandedByTaskId[id]) {
      delete this.expandedByTaskId[id];
    } else {
      this.expandedByTaskId[id] = true;
      this.ensureSubtasksLoaded(id);
    }
    this.cdr.markForCheck();
  }

  expandLabel(t: Task): string {
    if (t.id == null) return 'Show subtasks';
    return this.isRootExpanded(t.id) ? 'Hide subtasks' : 'Show subtasks';
  }

  private ensureSubtasksLoaded(parentTaskId: number): void {
    if (this.subtasksLoading[parentTaskId]) return;
    if (this.subtasksByTaskId[parentTaskId] !== undefined) return;
    this.subtasksLoading[parentTaskId] = true;
    this.cdr.markForCheck();
    this.taskService.listSubtasks(parentTaskId).subscribe({
      next: (list) => {
        this.subtasksByTaskId[parentTaskId] = list ?? [];
        this.subtasksLoading[parentTaskId] = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.subtasksLoading[parentTaskId] = false;
        this.subtasksByTaskId[parentTaskId] = [];
        this.cdr.markForCheck();
      },
    });
  }

  subtasksFor(parentTaskId: number | undefined): Subtask[] {
    if (parentTaskId == null) return [];
    return this.subtasksByTaskId[parentTaskId] ?? [];
  }

  /** Aligns with task service: non-cancelled subtasks count; completed = DONE. */
  private subtaskProgressFromList(list: Subtask[]): { total: number; completed: number } {
    const nonCancelled = list.filter((s) => s.status !== 'CANCELLED');
    const completed = nonCancelled.filter((s) => s.status === 'DONE').length;
    return { total: nonCancelled.length, completed };
  }

  subtaskProgressForTask(taskId: number | undefined): { total: number; completed: number } | null {
    if (taskId == null) return null;
    const raw = this.subtasksByTaskId[taskId];
    if (raw === undefined) return null;
    return this.subtaskProgressFromList(raw);
  }

  subtaskProgressPercent(taskId: number | undefined): number {
    const p = this.subtaskProgressForTask(taskId);
    if (!p || p.total <= 0) return 0;
    return Math.round((100 * p.completed) / p.total);
  }

  get extendedCompletionPct(): number {
    const n = Number(this.extendedStats?.completionPercentage);
    if (Number.isNaN(n)) return 0;
    return Math.min(100, Math.max(0, Math.round(n)));
  }

  extendedStatusCount(status: TaskStatus): number {
    const s = this.extendedStats;
    if (!s) return 0;
    switch (status) {
      case 'TODO':
        return s.todoCount ?? 0;
      case 'IN_PROGRESS':
        return s.inProgressCount ?? 0;
      case 'IN_REVIEW':
        return s.inReviewCount ?? 0;
      case 'DONE':
        return s.doneCount ?? 0;
      case 'CANCELLED':
        return s.cancelledCount ?? 0;
      default:
        return 0;
    }
  }

  extendedPriorityCount(priority: TaskPriority): number {
    const rows = this.extendedStats?.priorityBreakdown ?? [];
    const row = rows.find((r) => r.priority === priority);
    return row?.count ?? 0;
  }

  /** Alias for templates mirroring My Tasks wording. */
  priorityBreakdownCount(priority: TaskPriority): number {
    return this.extendedPriorityCount(priority);
  }

  formatCompletionPct(pct: number | undefined | null): string {
    if (pct == null || Number.isNaN(Number(pct))) return '0';
    return Math.round(Number(pct)).toString();
  }

  completionPctForAria(stats: TaskStatsExtendedDto): number {
    const n = Number(stats.completionPercentage);
    if (Number.isNaN(n)) return 0;
    return Math.min(100, Math.max(0, Math.round(n)));
  }

  scrollToTasksList(): void {
    document.getElementById('project-tasks-list')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  isStatFocus(key: string): boolean {
    return this.statsFocusKey === key;
  }

  get statsDrilldownBannerMessage(): string {
    if (this.listViewMode === 'overdue') {
      return 'Viewing all overdue work in this project (tasks and subtasks). Sidebar filters do not apply to this list.';
    }
    if (this.statsFocusKey === 'active') {
      return 'Viewing open root tasks only (not done or cancelled). Overview totals still include subtasks.';
    }
    if (this.statsFocusKey?.startsWith('status_')) {
      return 'List filtered by status from the overview. Change filters anytime.';
    }
    if (this.statsFocusKey?.startsWith('priority_')) {
      return 'List filtered by priority from the overview.';
    }
    return '';
  }

  get showStatsDrilldownBanner(): boolean {
    return this.listViewMode === 'overdue' || !!this.statsFocusKey;
  }

  applyOverdueDrilldown(): void {
    this.listViewMode = 'overdue';
    this.filterOpenTasksOnly = false;
    this.statsFocusKey = 'overdue';
    this.page = 0;
    this.filterForm.patchValue({ sort: 'dueDate,asc' }, { emitEvent: false });
    this.loadTasks();
    this.scrollToTasksList();
    this.cdr.markForCheck();
  }

  applyActiveDrilldown(): void {
    this.listViewMode = 'paginated';
    this.filterOpenTasksOnly = true;
    this.statsFocusKey = 'active';
    this.page = 0;
    this.filterForm.patchValue({ status: '', priority: '', sort: 'dueDate,asc' }, { emitEvent: false });
    this.loadTasks();
    this.scrollToTasksList();
    this.cdr.markForCheck();
  }

  applyStatusDrilldown(status: TaskStatus): void {
    this.listViewMode = 'paginated';
    this.filterOpenTasksOnly = false;
    this.statsFocusKey = `status_${status}`;
    this.page = 0;
    this.filterForm.patchValue({ status, priority: '', sort: 'dueDate,asc' }, { emitEvent: false });
    this.loadTasks();
    this.scrollToTasksList();
    this.cdr.markForCheck();
  }

  applyPriorityDrilldown(priority: TaskPriority): void {
    this.listViewMode = 'paginated';
    this.filterOpenTasksOnly = false;
    this.statsFocusKey = `priority_${priority}`;
    this.page = 0;
    this.filterForm.patchValue({ priority, status: '', sort: 'dueDate,asc' }, { emitEvent: false });
    this.loadTasks();
    this.scrollToTasksList();
    this.cdr.markForCheck();
  }

  resetStatsListView(): void {
    this.listViewMode = 'paginated';
    this.filterOpenTasksOnly = false;
    this.statsFocusKey = null;
    this.page = 0;
    this.filterForm.patchValue(
      { search: '', status: '', priority: '', sort: 'createdAt,desc' },
      { emitEvent: false }
    );
    this.loadTasks();
    this.scrollToTasksList();
    this.cdr.markForCheck();
  }

  clearStatDrilldownAndReload(): void {
    this.resetStatsListView();
  }
}
