import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, takeUntil } from 'rxjs';
import { TaskService, Task, TaskRequest, TaskStatus, TaskPriority, TaskFilterParams, PageResponse } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { Card } from '../../../shared/components/card/card';

interface ProjectOption {
  id: number;
  title: string;
}

@Component({
  selector: 'app-my-tasks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './my-tasks.html',
  styleUrl: './my-tasks.scss',
})
export class MyTasks implements OnInit, OnDestroy {
  tasks: Task[] = [];
  loading = true;
  errorMessage = '';
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  projectIdToTitle: Record<number, string> = {};
  private searchSubject$ = new Subject<string>();
  private destroy$ = new Subject<void>();
  projectsFromApi: ProjectOption[] = [];
  updatingStatus = false;

  addModalOpen = false;
  adding = false;
  addForm: FormGroup;
  editingTask: Task | null = null;
  saving = false;
  editForm: FormGroup;
  taskToDelete: Task | null = null;
  deleting = false;
  filterForm: FormGroup;

  constructor(
    private taskService: TaskService,
    public auth: AuthService,
    private projectService: ProjectService,
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
    this.filterForm = this.fb.group({ search: [''] });
  }

  get projectsForAdd(): ProjectOption[] {
    const fromTasks = [...new Set(this.tasks.map((t) => t.projectId).filter((id): id is number => id != null))]
      .sort((a, b) => a - b)
      .map((id) => ({ id, title: this.getProjectTitle(id) }));
    if (fromTasks.length > 0) return fromTasks;
    return this.projectsFromApi;
  }

  ngOnInit(): void {
    this.setupSearchDebounce();
    this.loadTasks();
    this.projectService.getAllProjects().pipe(catchError(() => of([]))).subscribe((projects) => {
      (projects ?? []).forEach((p) => {
        if (p.id != null) this.projectIdToTitle[p.id] = p.title?.trim() || `Project ${p.id}`;
      });
      this.cdr.detectChanges();
    });
    const userId = this.auth.getUserId();
    if (userId != null) {
      this.projectService.getByFreelancerId(userId).pipe(catchError(() => of([]))).subscribe((projects) => {
      const list = Array.isArray(projects) ? projects : [];
      this.projectsFromApi = list
        .filter((p): p is Project & { id: number } => p.id != null)
        .map((p) => ({ id: p.id, title: p.title?.trim() || `Project ${p.id}` }));
      this.cdr.detectChanges();
    });
    }
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

  loadTasks(): void {
    const userId = this.auth.getUserId();
    if (userId == null) {
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }
    this.loading = true;
    const params: TaskFilterParams = {
      page: this.page,
      size: this.size,
      sort: 'createdAt,desc',
      assigneeId: userId,
      search: this.filterForm.get('search')?.value?.trim() || null,
    };
    this.taskService.getFilteredTasks(params).subscribe({
      next: (p: PageResponse<Task>) => {
        this.tasks = p.content ?? [];
        this.totalElements = p.totalElements ?? 0;
        this.totalPages = p.totalPages ?? 0;
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

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadTasks();
  }

  getProjectTitle(id: number): string {
    return this.projectIdToTitle[id] ?? `Project ${id}`;
  }

  formatDueDate(d: string | null): string {
    return d ? new Date(d).toLocaleDateString() : '—';
  }

  updateStatusFromSelect(t: Task, value: string): void {
    if (!value || value.trim() === '') return;
    this.updateStatus(t, value as TaskStatus);
  }

  updateStatus(t: Task, status: TaskStatus): void {
    if (!t.id || this.updatingStatus) return;
    this.updatingStatus = true;
    this.taskService.patchStatus(t.id, status).subscribe({
      next: () => {
        this.updatingStatus = false;
        this.loadTasks();
        this.cdr.detectChanges();
      },
      error: () => {
        this.updatingStatus = false;
        this.errorMessage = 'Failed to update status.';
        this.cdr.detectChanges();
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
    const userId = this.auth.getUserId();
    const v = this.addForm.getRawValue();
    const request: TaskRequest = {
      projectId: Number(v.projectId),
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
        this.loadTasks();
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
        this.loadTasks();
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
        if (ok) this.loadTasks();
        else this.errorMessage = 'Failed to delete task.';
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
}
