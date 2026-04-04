import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import {
  Observable,
  Subject,
  Subscription,
  catchError,
  debounceTime,
  distinctUntilChanged,
  interval,
  map,
  of,
  switchMap,
  takeUntil,
} from 'rxjs';
import {
  TaskService,
  Task,
  TaskRequest,
  TaskStatus,
  TaskPriority,
  TaskFilterParams,
  PageResponse,
  AiProposedTask,
  Subtask,
  SubtaskRequest,
  ProjectActivity,
} from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectApplicationService } from '../../../core/services/project-application.service';
import { Card } from '../../../shared/components/card/card';
import {
  AiModelLiveStatus,
  AiModelStatusService,
} from '../../../core/services/aimodel-status.service';

interface ProjectOption {
  id: number;
  title: string;
}

@Component({
  selector: 'app-my-tasks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, Card],
  templateUrl: './my-tasks.html',
  styleUrl: './my-tasks.scss',
})
export class MyTasks implements OnInit, OnDestroy {
  tasks: Task[] = [];
  loading = true;
  /** True while reloading when the list already had rows (keeps previous tasks visible). */
  searching = false;
  errorMessage = '';
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  projectIdToTitle: Record<number, string> = {};
  /** Subtask completion for root tasks on the current page. */
  subtaskProgressByTaskId: Record<number, { total: number; completed: number }> = {};
  /** Global project order (lower index = more recently active). */
  private projectOrderRank = new Map<number, number>();
  projectActivityByProjectId: Record<number, ProjectActivity> = {};
  private readonly taskLoad$ = new Subject<void>();
  private destroy$ = new Subject<void>();
  /** Projects the freelancer is associated with (has tasks or accepted application). */
  associatedProjects: ProjectOption[] = [];
  updatingStatus = false;

  addModalOpen = false;
  adding = false;
  addForm: FormGroup;
  editingTask: Task | null = null;
  saving = false;
  editForm: FormGroup;
  taskToDelete: Task | null = null;
  deleting = false;
  /** Expand/collapse root tasks to load nested subtasks */
  expandedByTaskId: Record<number, boolean> = {};
  subtasksByTaskId: Record<number, Subtask[]> = {};
  subtasksLoading: Record<number, boolean> = {};
  updatingSubtaskStatus = false;
  subtaskToDelete: { parentTaskId: number; subtask: Subtask } | null = null;
  deletingSubtask = false;
  /** Manual add/edit subtask modal */
  subtaskModalParent: Task | null = null;
  subtaskModalEditing: Subtask | null = null;
  savingSubtask = false;
  subtaskForm: FormGroup;
  filterForm: FormGroup;

  suggestDescriptionLoading = false;
  aiWizardOpen = false;
  /** 'project-tasks' from toolbar/add flow; 'subtasks' from edit modal on a root task */
  aiWizardMode: 'project-tasks' | 'subtasks' | null = null;
  aiProjectSelectedId: number | null = null;
  aiProposalRows: AiProposedTask[] = [];
  aiWizardLoading = false;
  aiWizardError = '';
  acceptingAi = false;
  /** When generating subtasks, parent task (root). */
  aiSubtaskParent: Task | null = null;
  /** After Accept-all subtasks, re-expand parent and reload subtasks once tasks refresh */
  private pendingRevealSubtasksForTaskId: number | null = null;

  /** Live pipeline: gateway → AImodel → Ollama (polled while AI wizard is open). */
  private aiLivePollSub: Subscription | null = null;
  aiPipelineStatus: AiModelLiveStatus | null = null;
  aiPipelineGatewayError = false;
  aiStatusRefreshing = false;

  constructor(
    private taskService: TaskService,
    public auth: AuthService,
    private projectService: ProjectService,
    private projectApplicationService: ProjectApplicationService,
    private aiModelStatusService: AiModelStatusService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      projectId: [null as number | null, [Validators.required]],
      title: ['', Validators.required],
      description: [''],
      status: ['TODO' as TaskStatus],
      priority: ['MEDIUM' as TaskPriority],
      dueDate: [null as string | null],
    });
    this.editForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      status: ['TODO' as TaskStatus],
      priority: ['MEDIUM' as TaskPriority],
      dueDate: [null as string | null],
    });
    this.filterForm = this.fb.group({
      search: [''],
      status: [''],
      priority: [''],
      projectId: [null as number | string | null],
      sort: ['createdAt,desc'],
    });
    this.subtaskForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      status: ['TODO' as TaskStatus],
      priority: ['MEDIUM' as TaskPriority],
      dueDate: [null as string | null],
    });
  }

  /** Projects the freelancer can add tasks to (only those they're associated with via tasks or accepted application). */
  /** Tasks grouped by project for sectioned layout */
  get projectTaskGroups(): { projectId: number; title: string; tasks: Task[] }[] {
    const map = new Map<number, Task[]>();
    for (const t of this.tasks) {
      const pid = t.projectId;
      if (pid == null) continue;
      if (!map.has(pid)) map.set(pid, []);
      map.get(pid)!.push(t);
    }
    const rank = (pid: number) => this.projectOrderRank.get(pid) ?? 10_000;
    return [...map.entries()]
      .map(([projectId, groupTasks]) => ({
        projectId,
        title: this.getProjectTitle(projectId),
        tasks: [...groupTasks].sort((a, b) =>
          (a.title ?? '').localeCompare(b.title ?? '', undefined, { sensitivity: 'base' })
        ),
      }))
      .sort((a, b) => {
        const ra = rank(a.projectId);
        const rb = rank(b.projectId);
        if (ra !== rb) return ra - rb;
        return a.title.localeCompare(b.title, undefined, { sensitivity: 'base' });
      });
  }

  get filterProjectOptions(): ProjectOption[] {
    const ids = new Set<number>();
    for (const key of Object.keys(this.projectIdToTitle)) {
      const n = Number(key);
      if (!Number.isNaN(n)) ids.add(n);
    }
    for (const p of this.associatedProjects) ids.add(p.id);
    return [...ids]
      .map((id) => ({ id, title: this.getProjectTitle(id) }))
      .sort((a, b) => (a.title ?? '').localeCompare(b.title ?? '', undefined, { sensitivity: 'base' }));
  }

  get projectsForAdd(): ProjectOption[] {
    const fromTasks = [...new Set(this.tasks.map((t) => t.projectId).filter((id): id is number => id != null))]
      .map((id) => ({ id, title: this.getProjectTitle(id) }));
    const fromAccepted = this.associatedProjects
      .filter((p) => !fromTasks.some((ft) => ft.id === p.id))
      .map((p) => ({ id: p.id, title: this.getProjectTitle(p.id) || p.title }));
    const merged = [...fromTasks, ...fromAccepted];
    return merged.sort((a, b) => (a.title ?? '').localeCompare(b.title ?? ''));
  }

  ngOnInit(): void {
    this.setupTaskLoadStream();
    this.filterForm.valueChanges
      .pipe(
        debounceTime(350),
        map(() => this.filterFingerprint()),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.page = 0;
        this.taskLoad$.next();
      });
    this.taskLoad$.next();
    this.projectService.getAllProjects().pipe(catchError(() => of([]))).subscribe((projects) => {
      (projects ?? []).forEach((p) => {
        if (p.id != null) this.projectIdToTitle[p.id] = p.title?.trim() || `Project ${p.id}`;
      });
      this.cdr.detectChanges();
    });
    const userId = this.auth.getUserId();
    if (userId != null) {
      this.loadProjectActivity();
      this.projectApplicationService
        .getApplicationsByFreelance(userId)
        .pipe(catchError(() => of([])))
        .subscribe((applications) => {
          const accepted = (applications ?? []).filter(
            (a) => (a.status ?? '').toUpperCase() === 'ACCEPTED'
          );
          this.associatedProjects = accepted
            .map((a) => {
              const projectId = a.projectId ?? a.project?.id;
              const title = a.project?.title ?? (projectId != null ? `Project ${projectId}` : '');
              return projectId != null ? { id: projectId, title } : null;
            })
            .filter((p): p is ProjectOption => p != null);
          const seen = new Set<number>();
          this.associatedProjects = this.associatedProjects.filter((p) => {
            if (seen.has(p.id)) return false;
            seen.add(p.id);
            return true;
          });
          this.cdr.detectChanges();
        });
    }
  }

  ngOnDestroy(): void {
    this.stopAiLiveStatusPoll();
    this.destroy$.next();
    this.destroy$.complete();
  }

  get aiPipelineOk(): boolean {
    return !this.aiPipelineGatewayError && !!this.aiPipelineStatus?.modelReady;
  }

  get aiPipelineWarn(): boolean {
    return (
      !this.aiPipelineGatewayError &&
      !!this.aiPipelineStatus?.ollamaReachable &&
      !this.aiPipelineStatus?.modelReady
    );
  }

  get aiPipelineBad(): boolean {
    return (
      this.aiPipelineGatewayError ||
      (!!this.aiPipelineStatus && !this.aiPipelineStatus.ollamaReachable)
    );
  }

  get aiPipelineStatusMessage(): string {
    if (this.aiPipelineGatewayError) {
      return 'Cannot reach the AI service (gateway / AImodel). Is Eureka up and AIMODEL registered, or try restarting the gateway.';
    }
    if (!this.aiPipelineStatus) {
      return 'Checking AI service and Ollama…';
    }
    if (!this.aiPipelineStatus.ollamaReachable) {
      return `Ollama not reachable from the AI service. Start Ollama or set OLLAMA_BASE_URL on AImodel. Configured model: ${this.aiPipelineStatus.model}`;
    }
    if (!this.aiPipelineStatus.modelReady) {
      return `Ollama is running but model "${this.aiPipelineStatus.model}" was not found. Run: ollama pull ${this.aiPipelineStatus.model}`;
    }
    return `Ready — Ollama OK, model "${this.aiPipelineStatus.model}" available`;
  }

  private startAiLiveStatusPoll(): void {
    this.stopAiLiveStatusPoll();
    this.aiPipelineStatus = null;
    this.aiPipelineGatewayError = false;
    this.pullAiLiveStatus();
    this.aiLivePollSub = interval(8000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.aiWizardOpen) this.pullAiLiveStatus();
      });
  }

  private stopAiLiveStatusPoll(): void {
    this.aiLivePollSub?.unsubscribe();
    this.aiLivePollSub = null;
  }

  private pullAiLiveStatus(): void {
    this.aiStatusRefreshing = true;
    this.cdr.markForCheck();
    this.aiModelStatusService.getLiveStatus().subscribe({
      next: ({ snapshot, reachabilityError }) => {
        this.aiStatusRefreshing = false;
        if (reachabilityError) {
          this.aiPipelineStatus = null;
          this.aiPipelineGatewayError = true;
        } else {
          this.aiPipelineGatewayError = false;
          this.aiPipelineStatus = snapshot;
        }
        this.cdr.markForCheck();
      },
    });
  }

  private filterFingerprint(): string {
    const v = this.filterForm.getRawValue();
    return JSON.stringify({
      search: String(v.search ?? '').trim(),
      status: String(v.status ?? ''),
      priority: String(v.priority ?? ''),
      projectId: v.projectId == null || v.projectId === '' ? '' : String(v.projectId),
      sort: String(v.sort ?? ''),
    });
  }

  private setupTaskLoadStream(): void {
    this.taskLoad$
      .pipe(
        switchMap(() => this.fetchTasksPage()),
        takeUntil(this.destroy$)
      )
      .subscribe((outcome) => {
        if (outcome.type === 'nouser') {
          this.tasks = [];
          this.subtaskProgressByTaskId = {};
          this.totalElements = 0;
          this.totalPages = 0;
          this.loading = false;
          this.searching = false;
          this.cdr.detectChanges();
          return;
        }
        if (outcome.type === 'error') {
          this.errorMessage = 'Failed to load tasks.';
          this.loading = false;
          this.searching = false;
          this.cdr.detectChanges();
          return;
        }
        const p = outcome.p;
        const progress = outcome.progress;
        this.tasks = p.content ?? [];
        this.totalElements = p.totalElements ?? 0;
        this.totalPages = p.totalPages ?? 0;
        const nextProgress: Record<number, { total: number; completed: number }> = {};
        for (const t of this.tasks) {
          if (t.id == null) continue;
          nextProgress[t.id] = progress[t.id] ?? { total: 0, completed: 0 };
        }
        this.subtaskProgressByTaskId = nextProgress;
        this.expandedByTaskId = {};
        this.subtasksByTaskId = {};
        this.subtasksLoading = {};
        const revealId = this.pendingRevealSubtasksForTaskId;
        this.pendingRevealSubtasksForTaskId = null;
        this.loading = false;
        this.searching = false;
        this.errorMessage = '';
        if (revealId != null && this.tasks.some((t) => t.id === revealId)) {
          this.expandedByTaskId[revealId] = true;
          this.ensureSubtasksLoaded(revealId);
        }
        this.cdr.detectChanges();
      });
  }

  private fetchTasksPage(): Observable<
    | { type: 'nouser' }
    | { type: 'error' }
    | {
        type: 'ok';
        p: PageResponse<Task>;
        progress: Record<number, { total: number; completed: number }>;
      }
  > {
    const userId = this.auth.getUserId();
    if (userId == null) {
      return of({ type: 'nouser' as const });
    }
    const hadRows = this.tasks.length > 0;
    this.searching = hadRows;
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    const v = this.filterForm.getRawValue();
    const st = String(v.status ?? '').trim();
    const pr = String(v.priority ?? '').trim();
    const proj = v.projectId;
    const params: TaskFilterParams = {
      page: this.page,
      size: this.size,
      sort: String(v.sort ?? '').trim() || 'createdAt,desc',
      assigneeId: userId,
      search: String(v.search ?? '').trim() || null,
      status: st ? (st as TaskStatus) : null,
      priority: pr ? (pr as TaskPriority) : null,
      projectId: proj != null && proj !== '' ? Number(proj) : null,
    };
    return this.taskService.getFilteredTasks(params).pipe(
      switchMap((p) => {
        const ids = (p.content ?? []).map((t) => t.id).filter((id): id is number => id != null);
        return this.taskService.getSubtaskProgress(userId, ids).pipe(
          map((progress) => ({ type: 'ok' as const, p, progress }))
        );
      }),
      catchError(() => of({ type: 'error' as const }))
    );
  }

  /** Full list reload (cancels in-flight request via switchMap). */
  requestTaskReload(): void {
    this.taskLoad$.next();
  }

  private loadProjectActivity(): void {
    const userId = this.auth.getUserId();
    if (userId == null) return;
    this.taskService.getProjectActivity(userId).subscribe({
      next: (list) => {
        this.projectOrderRank.clear();
        this.projectActivityByProjectId = {};
        (list ?? []).forEach((row, i) => {
          this.projectOrderRank.set(row.projectId, i);
          this.projectActivityByProjectId[row.projectId] = row;
        });
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  private refreshSubtaskProgressForParents(parentIds: number[]): void {
    const userId = this.auth.getUserId();
    if (userId == null || parentIds.length === 0) return;
    this.taskService.getSubtaskProgress(userId, parentIds).subscribe({
      next: (m) => {
        for (const id of parentIds) {
          this.subtaskProgressByTaskId[id] = m[id] ?? { total: 0, completed: 0 };
        }
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  subtaskProgressForTask(taskId: number | undefined): { total: number; completed: number } | null {
    if (taskId == null) return null;
    return this.subtaskProgressByTaskId[taskId] ?? null;
  }

  subtaskProgressPercent(taskId: number | undefined): number {
    const p = this.subtaskProgressForTask(taskId);
    if (!p || p.total <= 0) return 0;
    return Math.round((100 * p.completed) / p.total);
  }

  formatProjectActivityHint(projectId: number): string {
    const row = this.projectActivityByProjectId[projectId];
    if (!row?.lastActivityAt) return '';
    const d = new Date(row.lastActivityAt);
    if (Number.isNaN(d.getTime())) return '';
    return `Last activity ${d.toLocaleString()} · ${row.openTaskCount} open task(s)`;
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.taskLoad$.next();
  }

  getProjectTitle(id: number): string {
    return this.projectIdToTitle[id] ?? `Project ${id}`;
  }

  formatDueDate(d: string | null): string {
    return d ? new Date(d).toLocaleDateString() : '—';
  }

  isRootExpanded(taskId: number | undefined): boolean {
    if (taskId == null) return false;
    return !!this.expandedByTaskId[taskId];
  }

  toggleRootExpanded(t: Task): void {
    const id = t.id;
    if (id == null) return;
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
        this.errorMessage = 'Failed to load subtasks.';
        this.cdr.markForCheck();
      },
    });
  }

  subtasksFor(parentTaskId: number | undefined): Subtask[] {
    if (parentTaskId == null) return [];
    return this.subtasksByTaskId[parentTaskId] ?? [];
  }

  /** Human-readable priority for list badges (API uses LOW, MEDIUM, …). */
  formatPriorityLabel(p: TaskPriority | null | undefined): string {
    const v = p ?? 'MEDIUM';
    const labels: Record<TaskPriority, string> = {
      LOW: 'Low',
      MEDIUM: 'Medium',
      HIGH: 'High',
      URGENT: 'Urgent',
    };
    return labels[v] ?? String(v);
  }

  effectivePriority(p: TaskPriority | null | undefined): TaskPriority {
    return p ?? 'MEDIUM';
  }

  updateStatusFromSelect(t: Task, value: string): void {
    if (!value || value.trim() === '') return;
    this.updateStatus(t, value as TaskStatus);
  }

  updateStatus(t: Task, status: TaskStatus): void {
    if (!t.id || this.updatingStatus) return;
    if (status === t.status) return;
    this.updatingStatus = true;
    this.errorMessage = '';
    // Optimistic update for immediate feedback
    const idx = this.tasks.findIndex((x) => x.id === t.id);
    if (idx >= 0) this.tasks[idx] = { ...this.tasks[idx], status };
    this.cdr.detectChanges();
    this.taskService.patchStatus(t.id, status).subscribe({
      next: (updated) => {
        this.updatingStatus = false;
        if (updated) {
          if (idx >= 0) this.tasks[idx] = { ...this.tasks[idx], ...updated };
        } else {
          // API failed (catchError returned null) - revert optimistic update
          if (idx >= 0) this.tasks[idx] = { ...this.tasks[idx], status: t.status };
          this.errorMessage = 'Failed to update status.';
        }
        if (updated) this.loadProjectActivity();
        this.cdr.detectChanges();
      },
      error: () => {
        this.updatingStatus = false;
        if (idx >= 0) this.tasks[idx] = { ...this.tasks[idx], status: t.status };
        this.errorMessage = 'Failed to update status.';
        this.cdr.detectChanges();
      },
    });
  }

  updateSubtaskStatusFromSelect(parentTaskId: number, st: Subtask, value: string): void {
    if (!value?.trim()) return;
    this.updateSubtaskStatus(parentTaskId, st, value as TaskStatus);
  }

  updateSubtaskStatus(parentTaskId: number, st: Subtask, status: TaskStatus): void {
    if (!st.id || this.updatingSubtaskStatus) return;
    if (status === st.status) return;
    this.updatingSubtaskStatus = true;
    this.errorMessage = '';
    const list = this.subtasksByTaskId[parentTaskId] ?? [];
    const idx = list.findIndex((x) => x.id === st.id);
    if (idx >= 0) {
      list[idx] = { ...list[idx], status };
      this.subtasksByTaskId[parentTaskId] = [...list];
    }
    this.cdr.markForCheck();
    this.taskService.patchSubtaskStatus(st.id, status).subscribe({
      next: (updated) => {
        this.updatingSubtaskStatus = false;
        if (updated && idx >= 0) {
          const cur = this.subtasksByTaskId[parentTaskId] ?? [];
          const j = cur.findIndex((x) => x.id === st.id);
          if (j >= 0) {
            cur[j] = { ...cur[j], ...updated };
            this.subtasksByTaskId[parentTaskId] = [...cur];
          }
        } else if (idx >= 0) {
          const cur = this.subtasksByTaskId[parentTaskId] ?? [];
          const j = cur.findIndex((x) => x.id === st.id);
          if (j >= 0) {
            cur[j] = { ...cur[j], status: st.status };
            this.subtasksByTaskId[parentTaskId] = [...cur];
          }
          this.errorMessage = 'Failed to update subtask status.';
        }
        if (updated) {
          this.refreshSubtaskProgressForParents([parentTaskId]);
          this.loadProjectActivity();
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.updatingSubtaskStatus = false;
        if (idx >= 0) {
          const cur = this.subtasksByTaskId[parentTaskId] ?? [];
          const j = cur.findIndex((x) => x.id === st.id);
          if (j >= 0) {
            cur[j] = { ...cur[j], status: st.status };
            this.subtasksByTaskId[parentTaskId] = [...cur];
          }
        }
        this.errorMessage = 'Failed to update subtask status.';
        this.cdr.markForCheck();
      },
    });
  }

  openAddSubtask(parent: Task): void {
    if (parent.id == null) return;
    this.subtaskModalParent = parent;
    this.subtaskModalEditing = null;
    this.subtaskForm.reset({
      title: '',
      description: '',
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: null,
    });
    this.errorMessage = '';
    this.cdr.markForCheck();
  }

  openEditSubtask(parent: Task, st: Subtask): void {
    if (parent.id == null || st.id == null) return;
    this.subtaskModalParent = parent;
    this.subtaskModalEditing = st;
    const due = st.dueDate ? String(st.dueDate).slice(0, 10) : null;
    this.subtaskForm.patchValue({
      title: st.title ?? '',
      description: st.description ?? '',
      status: st.status ?? 'TODO',
      priority: st.priority ?? 'MEDIUM',
      dueDate: due,
    });
    this.errorMessage = '';
    this.cdr.markForCheck();
  }

  closeSubtaskModal(): void {
    if (this.savingSubtask) return;
    this.subtaskModalParent = null;
    this.subtaskModalEditing = null;
    this.cdr.markForCheck();
  }

  saveSubtask(): void {
    if (this.subtaskForm.invalid || this.subtaskModalParent?.id == null) {
      this.subtaskForm.markAllAsTouched();
      return;
    }
    const userId = this.auth.getUserId();
    const v = this.subtaskForm.getRawValue();
    const body: SubtaskRequest = {
      title: (v.title as string).trim(),
      description: (v.description as string)?.trim() || null,
      status: (v.status as TaskStatus) || 'TODO',
      priority: (v.priority as TaskPriority) || 'MEDIUM',
      assigneeId: userId,
      dueDate: v.dueDate ? (v.dueDate as string) : null,
    };
    const parentId = this.subtaskModalParent.id;
    this.savingSubtask = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    const finish = (ok: boolean): void => {
      this.savingSubtask = false;
      if (ok) {
        this.subtaskModalParent = null;
        this.subtaskModalEditing = null;
      }
      this.cdr.markForCheck();
    };
    if (this.subtaskModalEditing?.id != null) {
      this.taskService.updateSubtask(this.subtaskModalEditing.id, body).subscribe({
        next: (updated) => {
          if (updated) {
            const cur = this.subtasksByTaskId[parentId] ?? [];
            const idx = cur.findIndex((x) => x.id === updated.id);
            if (idx >= 0) {
              const next = [...cur];
              next[idx] = { ...next[idx], ...updated };
              this.subtasksByTaskId[parentId] = next;
            }
            this.refreshSubtaskProgressForParents([parentId]);
            this.loadProjectActivity();
            finish(true);
          } else {
            this.errorMessage = 'Failed to update subtask.';
            finish(false);
          }
        },
        error: () => {
          this.errorMessage = 'Failed to update subtask.';
          finish(false);
        },
      });
      return;
    }
    this.taskService.createSubtask(parentId, body).subscribe({
      next: (created) => {
        if (created) {
          delete this.subtasksByTaskId[parentId];
          delete this.subtasksLoading[parentId];
          this.ensureSubtasksLoaded(parentId);
          this.refreshSubtaskProgressForParents([parentId]);
          this.loadProjectActivity();
          finish(true);
        } else {
          this.errorMessage = 'Failed to create subtask.';
          finish(false);
        }
      },
      error: () => {
        this.errorMessage = 'Failed to create subtask.';
        finish(false);
      },
    });
  }

  openDeleteSubtask(parentTaskId: number, st: Subtask): void {
    this.subtaskToDelete = { parentTaskId, subtask: st };
  }

  closeDeleteSubtaskModal(): void {
    if (!this.deletingSubtask) this.subtaskToDelete = null;
  }

  doDeleteSubtask(): void {
    if (!this.subtaskToDelete?.subtask.id) return;
    const { parentTaskId, subtask } = this.subtaskToDelete;
    this.deletingSubtask = true;
    this.taskService.deleteSubtask(subtask.id).subscribe({
      next: (ok) => {
        this.deletingSubtask = false;
        this.subtaskToDelete = null;
        if (ok) {
          const cur = this.subtasksByTaskId[parentTaskId] ?? [];
          this.subtasksByTaskId[parentTaskId] = cur.filter((x) => x.id !== subtask.id);
          this.refreshSubtaskProgressForParents([parentTaskId]);
          this.loadProjectActivity();
        } else {
          this.errorMessage = 'Failed to delete subtask.';
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.deletingSubtask = false;
        this.errorMessage = 'Failed to delete subtask.';
        this.cdr.markForCheck();
      },
    });
  }

  openAdd(): void {
    const firstProject = this.projectsForAdd[0]?.id ?? null;
    this.addForm.patchValue({
      projectId: firstProject,
      title: '',
      description: '',
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: null,
    });
    this.addModalOpen = true;
  }

  closeAdd(): void {
    if (!this.adding) this.addModalOpen = false;
  }

  saveAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    const projectId = Number(v.projectId);
    const allowed = this.projectsForAdd.some((p) => p.id === projectId);
    if (!allowed) {
      this.errorMessage = 'You can only add tasks to projects you are associated with.';
      this.cdr.detectChanges();
      return;
    }
    const userId = this.auth.getUserId();
    const request: TaskRequest = {
      projectId,
      title: v.title.trim(),
      description: v.description?.trim() || null,
      status: (v.status as TaskStatus) || 'TODO',
      priority: (v.priority as TaskPriority) || 'MEDIUM',
      assigneeId: userId,
      dueDate: v.dueDate ? v.dueDate : null,
    };
    this.adding = true;
    this.taskService.createTask(request).subscribe({
      next: () => {
        this.adding = false;
        this.addModalOpen = false;
        this.requestTaskReload();
        this.loadProjectActivity();
        this.cdr.detectChanges();
      },
      error: () => {
        this.adding = false;
        this.errorMessage = 'Failed to create task.';
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(t: Task): void {
    this.editingTask = t;
    this.editForm.patchValue({
      title: t.title ?? '',
      description: t.description ?? '',
      status: t.status ?? 'TODO',
      priority: t.priority ?? 'MEDIUM',
      dueDate: t.dueDate ? t.dueDate.toString().slice(0, 10) : null,
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingTask = null;
  }

  saveEdit(): void {
    if (!this.editingTask?.id || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const request: TaskRequest = {
      projectId: this.editingTask.projectId,
      title: v.title.trim(),
      description: v.description?.trim() || null,
      status: (v.status as TaskStatus) || this.editingTask.status,
      priority: (v.priority as TaskPriority) || this.editingTask.priority,
      assigneeId: this.editingTask.assigneeId,
      dueDate: v.dueDate ? v.dueDate : null,
    };
    this.saving = true;
    this.taskService.updateTask(this.editingTask.id, request).subscribe({
      next: () => {
        this.saving = false;
        this.editingTask = null;
        this.requestTaskReload();
        this.loadProjectActivity();
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update task.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(t: Task): void {
    this.taskToDelete = t;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.taskToDelete = null;
  }

  doDelete(): void {
    if (!this.taskToDelete?.id) return;
    this.deleting = true;
    this.taskService.deleteTask(this.taskToDelete.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.taskToDelete = null;
        if (ok) {
          this.requestTaskReload();
          this.loadProjectActivity();
        } else this.errorMessage = 'Failed to delete task.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete task.';
        this.cdr.detectChanges();
      },
    });
  }

  get isFreelancer(): boolean {
    return this.auth.isFreelancer();
  }

  suggestDescriptionFromAi(): void {
    const userId = this.auth.getUserId();
    const projectId = this.addForm.get('projectId')?.value as number | null;
    const title = (this.addForm.get('title')?.value as string | null)?.trim();
    if (userId == null || projectId == null || !title) {
      this.errorMessage = 'Select a project and enter a title first.';
      this.cdr.detectChanges();
      return;
    }
    const allowed = this.projectsForAdd.some((p) => p.id === projectId);
    if (!allowed) {
      this.errorMessage = 'You can only use AI on projects you are associated with.';
      this.cdr.detectChanges();
      return;
    }
    this.errorMessage = '';
    this.suggestDescriptionLoading = true;
    this.cdr.detectChanges();
    this.taskService.suggestTaskDescription({ projectId, freelancerId: userId, title }).subscribe({
      next: (res) => {
        this.suggestDescriptionLoading = false;
        this.addForm.patchValue({ description: res.description ?? '' });
        this.cdr.detectChanges();
      },
      error: (err: { error?: { message?: string } }) => {
        this.suggestDescriptionLoading = false;
        this.errorMessage = err?.error?.message ?? 'AI suggestion failed.';
        this.cdr.detectChanges();
      },
    });
  }

  openAiProjectTasksWizard(): void {
    if (this.projectsForAdd.length === 0) return;
    this.aiWizardMode = 'project-tasks';
    this.aiSubtaskParent = null;
    this.aiProjectSelectedId = null;
    this.aiProposalRows = [];
    this.aiWizardError = '';
    this.aiWizardOpen = true;
    this.startAiLiveStatusPoll();
    this.cdr.detectChanges();
  }

  openAiSubtasksWizard(parent: Task): void {
    const userId = this.auth.getUserId();
    if (userId == null || parent.id == null) return;
    this.aiWizardMode = 'subtasks';
    this.aiSubtaskParent = parent;
    this.aiProjectSelectedId = parent.projectId;
    this.aiProposalRows = [];
    this.aiWizardError = '';
    this.aiWizardOpen = true;
    this.startAiLiveStatusPoll();
    this.cdr.detectChanges();
  }

  closeAiWizard(): void {
    if (this.acceptingAi) return;
    this.stopAiLiveStatusPoll();
    this.aiWizardOpen = false;
    this.aiWizardMode = null;
    this.aiSubtaskParent = null;
    this.aiProposalRows = [];
    this.aiWizardError = '';
    this.cdr.detectChanges();
  }

  declineAiProposals(): void {
    this.closeAiWizard();
  }

  regenerateAiProposals(): void {
    const userId = this.auth.getUserId();
    if (userId == null || this.aiWizardMode == null) return;
    this.aiWizardLoading = true;
    this.aiWizardError = '';
    this.cdr.detectChanges();
    if (this.aiWizardMode === 'project-tasks') {
      const pid = this.aiProjectSelectedId;
      if (pid == null) {
        this.aiWizardLoading = false;
        this.aiWizardError = 'Select a project.';
        this.cdr.detectChanges();
        return;
      }
      this.taskService.proposeProjectTasks({ projectId: pid, freelancerId: userId }).subscribe({
        next: (rows) => {
          this.aiWizardLoading = false;
          this.aiProposalRows = (rows ?? []).map((r) => ({
            title: r.title ?? '',
            description: r.description ?? '',
            suggestedPriority: r.suggestedPriority ?? 'MEDIUM',
            suggestedDueDate: this.normalizeAiDueDate(r.suggestedDueDate) ?? '',
          }));
          this.cdr.detectChanges();
        },
        error: (err: { error?: { message?: string } }) => {
          this.aiWizardLoading = false;
          this.aiWizardError = err?.error?.message ?? 'AI generation failed.';
          this.cdr.detectChanges();
        },
      });
      return;
    }
    if (this.aiWizardMode === 'subtasks' && this.aiSubtaskParent?.id != null) {
      this.taskService
        .proposeSubtasks({ taskId: this.aiSubtaskParent.id, freelancerId: userId })
        .subscribe({
          next: (rows) => {
            this.aiWizardLoading = false;
            this.aiProposalRows = (rows ?? []).map((r) => ({
              title: r.title ?? '',
              description: r.description ?? '',
              suggestedPriority: r.suggestedPriority ?? 'MEDIUM',
              suggestedDueDate: this.normalizeAiDueDate(r.suggestedDueDate) ?? '',
            }));
            this.cdr.detectChanges();
          },
          error: (err: { error?: { message?: string } }) => {
            this.aiWizardLoading = false;
            this.aiWizardError = err?.error?.message ?? 'AI generation failed.';
            this.cdr.detectChanges();
          },
        });
    }
  }

  acceptAiProposals(): void {
    const userId = this.auth.getUserId();
    if (userId == null || this.aiWizardMode == null) return;
    const rows = this.aiProposalRows.filter((r) => (r.title ?? '').trim().length > 0);
    if (rows.length === 0) {
      this.aiWizardError = 'No tasks to create.';
      this.cdr.detectChanges();
      return;
    }
    this.acceptingAi = true;
    this.aiWizardError = '';
    this.cdr.detectChanges();
    let i = 0;
    const runNext = (): void => {
      if (i >= rows.length) {
        this.acceptingAi = false;
        const wasSubtasks = this.aiWizardMode === 'subtasks';
        const subParentId = this.aiSubtaskParent?.id ?? null;
        if (wasSubtasks && subParentId != null) {
          this.pendingRevealSubtasksForTaskId = subParentId;
        }
        this.closeAiWizard();
        this.requestTaskReload();
        this.loadProjectActivity();
        if (wasSubtasks) {
          this.editingTask = null;
        }
        this.cdr.detectChanges();
        return;
      }
      const row = rows[i++];
      const due = (row.suggestedDueDate ?? '').trim();
      if (this.aiWizardMode === 'subtasks' && this.aiSubtaskParent?.id != null) {
        const subReq: SubtaskRequest = {
          title: row.title.trim(),
          description: row.description?.trim() || null,
          status: 'TODO',
          priority: row.suggestedPriority ?? 'MEDIUM',
          assigneeId: userId,
          dueDate: due.length >= 8 ? due : null,
        };
        this.taskService.createSubtask(this.aiSubtaskParent.id, subReq).subscribe({
          next: () => runNext(),
          error: (err: { error?: { message?: string } }) => {
            this.acceptingAi = false;
            this.aiWizardError = err?.error?.message ?? 'Failed to create a subtask.';
            this.cdr.detectChanges();
          },
        });
        return;
      }
      const projectId = this.aiProjectSelectedId as number;
      const request: TaskRequest = {
        projectId,
        title: row.title.trim(),
        description: row.description?.trim() || null,
        status: 'TODO',
        priority: row.suggestedPriority ?? 'MEDIUM',
        assigneeId: userId,
        dueDate: due.length >= 8 ? due : null,
      };
      this.taskService.createTask(request).subscribe({
        next: () => runNext(),
        error: (err: { error?: { message?: string } }) => {
          this.acceptingAi = false;
          this.aiWizardError = err?.error?.message ?? 'Failed to create a task.';
          this.cdr.detectChanges();
        },
      });
    };
    runNext();
  }

  /** User clicks Generate (project tasks) or Generate (subtasks) — not called on modal open. */
  generateAiProposalsClick(): void {
    this.regenerateAiProposals();
  }

  /** yyyy-MM-dd for <input type="date">; supports Spring array-style LocalDate JSON. */
  private normalizeAiDueDate(raw: string | string[] | null | undefined): string | null {
    if (raw == null) return null;
    const s = Array.isArray(raw) ? raw.join('-') : String(raw);
    const m = s.trim().match(/^(\d{4})-(\d{2})-(\d{2})/);
    return m ? `${m[1]}-${m[2]}-${m[3]}` : null;
  }
}
