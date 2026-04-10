import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

const TASK_API = `${environment.apiGatewayUrl}/task/api`;

/** Task entity (matches backend Task). */
export interface Task {
  id: number;
  projectId: number;
  contractId: number | null;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number | null;
  dueDate: string | null;
  orderIndex: number;
  createdBy: number | null;
  /**
   * When true (overdue / due-soon merged rows), {@link id} refers to a subtask; use subtask APIs.
   */
  subtask?: boolean | null;
  /** Root task id when {@link subtask} is true (synthetic API rows). */
  parentTaskId?: number | null;
  createdAt: string;
  updatedAt: string;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

/** Task comment (matches backend TaskComment). */
export interface TaskComment {
  id: number;
  taskId: number;
  userId: number;
  message: string;
  createdAt: string;
}

/** Request body for create/update task. */
export interface TaskRequest {
  projectId: number;
  contractId?: number | null;
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: number | null;
  dueDate?: string | null;
  orderIndex?: number | null;
  createdBy?: number | null;
}

/** Subtask row (Task MS `subtask` table). */
export interface Subtask {
  id: number;
  parentTaskId?: number | null;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number | null;
  dueDate: string | null;
  orderIndex: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SubtaskRequest {
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: number | null;
  dueDate?: string | null;
  orderIndex?: number | null;
}

/** Request body for create/update task comment. */
export interface TaskCommentRequest {
  taskId: number;
  userId: number;
  message: string;
}

/** Filter params for paginated task list. */
export interface TaskFilterParams {
  page?: number;
  size?: number;
  sort?: string;
  projectId?: number | null;
  contractId?: number | null;
  assigneeId?: number | null;
  status?: TaskStatus | null;
  priority?: TaskPriority | null;
  search?: string | null;
  dueDateFrom?: string | null;
  dueDateTo?: string | null;
  /** When true and status omitted, API excludes DONE and CANCELLED (open root tasks). */
  openTasksOnly?: boolean | null;
}

/** Spring Data Page response. */
export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

/** Task stats (project or freelancer). */
export interface TaskStatsDto {
  totalTasks: number;
  doneCount: number;
  inProgressCount: number;
  overdueCount: number;
  completionPercentage: number;
}

/** Count per priority (extended stats). */
export interface TaskPriorityCountDto {
  priority: TaskPriority;
  count: number;
}

/** Extended task stats: root tasks + subtasks, status split and priority breakdown. */
export interface TaskStatsExtendedDto {
  totalTasks: number;
  doneCount: number;
  inProgressCount: number;
  inReviewCount: number;
  todoCount: number;
  cancelledCount: number;
  overdueCount: number;
  completionPercentage: number;
  unassignedCount: number;
  createdInRangeCount?: number;
  completedInRangeCount?: number;
  priorityBreakdown?: TaskPriorityCountDto[];
  /** Projects where you have at least one assigned task/subtask (from task service). */
  projectIdsWithAssignedWork?: number[];
}

/** Kanban board: tasks grouped by status. */
export interface TaskBoardDto {
  projectId: number;
  columns?: Record<string, Task[]>;
}

/** Health payload for Task microservice. */
export interface TaskHealthDatabase {
  status: string;
  taskCount?: number;
  error?: string;
}

export interface TaskHealth {
  service: string;
  status: string;
  timestamp: string;
  database?: TaskHealthDatabase;
}

/** AI-suggested task or subtask (Task MS). */
export interface AiProposedTask {
  title: string;
  description: string | null;
  suggestedPriority: TaskPriority;
  /** yyyy-MM-dd from backend; optional */
  suggestedDueDate?: string | null;
}

export interface TaskAiSuggestDescriptionRequest {
  projectId: number;
  freelancerId: number;
  title: string;
}

export interface TaskAiSuggestDescriptionResponse {
  description: string;
}

export interface TaskAiProjectContextRequest {
  projectId: number;
  freelancerId: number;
}

export interface TaskAiSubtasksRequest {
  taskId: number;
  freelancerId: number;
}

/** POST `/task/api/tasks/ai/workload-coach` — narrative coaching from assignee workload snapshot. */
export interface TaskAiWorkloadCoachRequest {
  freelancerId: number;
  /** Due-soon window in days; backend default if omitted. */
  horizonDays?: number | null;
}

export interface TaskAiWorkloadCoachResponse {
  summaryMarkdown?: string | null;
  highlights?: string[] | null;
}

/** POST `/task/api/tasks/ai/definition-of-done` — checklist before review. */
export interface TaskAiDefinitionOfDoneRequest {
  taskId: number;
  freelancerId: number;
}

export interface TaskAiDefinitionOfDoneCriterionDto {
  text?: string | null;
  mustHave: boolean;
}

export interface TaskAiDefinitionOfDoneResponse {
  criteria?: TaskAiDefinitionOfDoneCriterionDto[] | null;
  assumptions?: string[] | null;
}

/** POST `/task/api/tasks/ai/ask-tasks` — RAG-lite Q&A over open tasks. */
export interface TaskAiAskTasksRequest {
  freelancerId: number;
  question: string;
}

export interface TaskAiAskTasksResponse {
  answerMarkdown?: string | null;
  /** Root task ids the model cited as relevant. */
  citedTaskIds?: number[] | null;
}

/** POST `/task/api/tasks/ai/client-status-brief` — client-only; merges tasks + Planning when available. */
export interface TaskAiClientBriefRequest {
  projectId: number;
  clientUserId: number;
  reportFrom?: string | null;
  reportTo?: string | null;
}

export interface TaskAiClientBriefResponse {
  briefMarkdown?: string | null;
  planningDataWarning?: string | null;
}

/** Batch subtask completion for root tasks (Task MS). */
export interface SubtaskProgressRow {
  parentTaskId: number;
  total: number;
  completed: number;
}

/** Per-project recency for assignee dashboard ordering. */
export interface ProjectActivity {
  projectId: number;
  lastActivityAt: string | null;
  openTaskCount: number;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  constructor(private http: HttpClient) {}

  getFilteredTasks(params: TaskFilterParams): Observable<PageResponse<Task>> {
    const query = new URLSearchParams();
    if (params.page != null) query.set('page', String(params.page));
    if (params.size != null) query.set('size', String(params.size));
    if (params.sort?.trim()) query.set('sort', params.sort.trim());
    if (params.projectId != null) query.set('projectId', String(params.projectId));
    if (params.contractId != null) query.set('contractId', String(params.contractId));
    if (params.assigneeId != null) query.set('assigneeId', String(params.assigneeId));
    if (params.status) query.set('status', params.status);
    if (params.priority) query.set('priority', params.priority);
    if (params.search?.trim()) query.set('search', params.search.trim());
    if (params.dueDateFrom?.trim()) query.set('dueDateFrom', params.dueDateFrom.trim());
    if (params.dueDateTo?.trim()) query.set('dueDateTo', params.dueDateTo.trim());
    if (params.openTasksOnly === true) query.set('openTasksOnly', 'true');
    const qs = query.toString();
    const url = qs ? `${TASK_API}/tasks?${qs}` : `${TASK_API}/tasks`;
    return this.http.get<PageResponse<Task>>(url).pipe(
      map((p) => ({
        content: p?.content ?? [],
        totalElements: p?.totalElements ?? 0,
        totalPages: p?.totalPages ?? 0,
        size: p?.size ?? 20,
        number: p?.number ?? 0,
      })),
      catchError((err) => throwError(() => err))
    );
  }

  getTaskById(id: number): Observable<Task | null> {
    return this.http.get<Task>(`${TASK_API}/tasks/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getTasksByProjectId(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${TASK_API}/tasks/project/${projectId}`).pipe(
      catchError(() => of([]))
    );
  }

  getTasksByAssigneeId(assigneeId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${TASK_API}/tasks/assignee/${assigneeId}`).pipe(
      catchError(() => of([]))
    );
  }

  getSubtaskProgress(
    assigneeId: number,
    taskIds: number[]
  ): Observable<Record<number, { total: number; completed: number }>> {
    if (taskIds.length === 0) {
      return of({});
    }
    const params = { taskIds: taskIds.join(',') };
    return this.http
      .get<SubtaskProgressRow[]>(`${TASK_API}/tasks/assignee/${assigneeId}/subtask-progress`, { params })
      .pipe(
        map((rows) => {
          const out: Record<number, { total: number; completed: number }> = {};
          for (const r of rows ?? []) {
            if (r.parentTaskId == null) continue;
            out[r.parentTaskId] = { total: Number(r.total) || 0, completed: Number(r.completed) || 0 };
          }
          return out;
        }),
        catchError(() => of({}))
      );
  }

  getProjectActivity(assigneeId: number): Observable<ProjectActivity[]> {
    return this.http.get<ProjectActivity[]>(`${TASK_API}/tasks/assignee/${assigneeId}/project-activity`).pipe(
      catchError(() => of([]))
    );
  }

  getBoardByProject(projectId: number): Observable<TaskBoardDto | null> {
    return this.http.get<TaskBoardDto>(`${TASK_API}/tasks/board/project/${projectId}`).pipe(
      catchError(() => of(null))
    );
  }

  getOverdueTasks(projectId?: number | null, assigneeId?: number | null): Observable<Task[]> {
    const query = new URLSearchParams();
    if (projectId != null) query.set('projectId', String(projectId));
    if (assigneeId != null) query.set('assigneeId', String(assigneeId));
    const qs = query.toString();
    const url = qs ? `${TASK_API}/tasks/overdue?${qs}` : `${TASK_API}/tasks/overdue`;
    return this.http.get<Task[]>(url).pipe(
      catchError(() => of([]))
    );
  }

  getStatsByProject(projectId: number): Observable<TaskStatsDto | null> {
    return this.http.get<TaskStatsDto>(`${TASK_API}/tasks/stats/project/${projectId}`).pipe(
      catchError(() => of(null))
    );
  }

  getStatsByFreelancer(freelancerId: number, from?: string | null, to?: string | null): Observable<TaskStatsDto | null> {
    const query = new URLSearchParams();
    if (from?.trim()) query.set('from', from.trim());
    if (to?.trim()) query.set('to', to.trim());
    const qs = query.toString();
    const url = qs ? `${TASK_API}/tasks/stats/freelancer/${freelancerId}?${qs}` : `${TASK_API}/tasks/stats/freelancer/${freelancerId}`;
    return this.http.get<TaskStatsDto>(url).pipe(
      catchError(() => of(null))
    );
  }

  getExtendedStatsByFreelancer(
    freelancerId: number,
    options?: { from?: string | null; to?: string | null }
  ): Observable<TaskStatsExtendedDto | null> {
    const query = new URLSearchParams();
    const from = options?.from?.trim();
    const to = options?.to?.trim();
    if (from) query.set('from', from);
    if (to) query.set('to', to);
    const qs = query.toString();
    const base = `${TASK_API}/tasks/stats/extended/freelancer/${freelancerId}`;
    const url = qs ? `${base}?${qs}` : base;
    return this.http.get<TaskStatsExtendedDto>(url).pipe(catchError(() => of(null)));
  }

  getExtendedStatsByProject(projectId: number): Observable<TaskStatsExtendedDto | null> {
    return this.http
      .get<TaskStatsExtendedDto>(`${TASK_API}/tasks/stats/extended/project/${projectId}`)
      .pipe(catchError(() => of(null)));
  }

  getStatsDashboard(): Observable<TaskStatsDto | null> {
    return this.http.get<TaskStatsDto>(`${TASK_API}/tasks/stats/dashboard`).pipe(
      catchError(() => of(null))
    );
  }

  createTask(request: TaskRequest): Observable<Task | null> {
    return this.http.post<Task>(`${TASK_API}/tasks`, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  updateTask(id: number, request: TaskRequest): Observable<Task | null> {
    return this.http.put<Task>(`${TASK_API}/tasks/${id}`, request).pipe(
      catchError(() => of(null))
    );
  }

  patchStatus(id: number, status: TaskStatus): Observable<Task | null> {
    return this.http.patch<Task>(`${TASK_API}/tasks/${id}/status`, null, { params: { status } }).pipe(
      catchError(() => of(null))
    );
  }

  patchAssignee(id: number, assigneeId: number | null): Observable<Task | null> {
    const params: Record<string, string> = {};
    if (assigneeId != null) params['assigneeId'] = String(assigneeId);
    const options = Object.keys(params).length > 0 ? { params } : {};
    return this.http.patch<Task>(`${TASK_API}/tasks/${id}/assignee`, null, options).pipe(
      catchError(() => of(null))
    );
  }

  reorderTasks(taskIds: number[]): Observable<void> {
    return this.http.post<void>(`${TASK_API}/tasks/reorder`, taskIds).pipe(
      catchError(() => of(undefined))
    );
  }

  deleteTask(id: number): Observable<boolean> {
    return this.http.delete(`${TASK_API}/tasks/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  getCommentsByTaskId(taskId: number): Observable<TaskComment[]> {
    return this.http.get<TaskComment[]>(`${TASK_API}/task-comments/task/${taskId}`).pipe(
      catchError(() => of([]))
    );
  }

  createComment(request: TaskCommentRequest): Observable<TaskComment | null> {
    return this.http.post<TaskComment>(`${TASK_API}/task-comments`, request).pipe(
      catchError(() => of(null))
    );
  }

  updateComment(id: number, request: TaskCommentRequest): Observable<TaskComment | null> {
    return this.http.put<TaskComment>(`${TASK_API}/task-comments/${id}`, request).pipe(
      catchError(() => of(null))
    );
  }

  deleteComment(id: number): Observable<boolean> {
    return this.http.delete(`${TASK_API}/task-comments/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  getTaskHealth(): Observable<TaskHealth | null> {
    return this.http.get<TaskHealth>(`${TASK_API}/task/health`).pipe(
      catchError(() => of(null))
    );
  }

  suggestTaskDescription(request: TaskAiSuggestDescriptionRequest): Observable<TaskAiSuggestDescriptionResponse> {
    return this.http
      .post<TaskAiSuggestDescriptionResponse>(`${TASK_API}/tasks/ai/suggest-description`, request)
      .pipe(catchError((err) => throwError(() => err)));
  }

  proposeProjectTasks(request: TaskAiProjectContextRequest): Observable<AiProposedTask[]> {
    return this.http
      .post<AiProposedTask[]>(`${TASK_API}/tasks/ai/propose-project-tasks`, request)
      .pipe(catchError((err) => throwError(() => err)));
  }

  proposeSubtasks(request: TaskAiSubtasksRequest): Observable<AiProposedTask[]> {
    return this.http
      .post<AiProposedTask[]>(`${TASK_API}/tasks/ai/propose-subtasks`, request)
      .pipe(catchError((err) => throwError(() => err)));
  }

  /**
   * AI workload coach: POST body `{ freelancerId, horizonDays? }`.
   * Errors propagate (gateway timeout / 502 from Task MS); callers should surface `error.error.message` when present.
   */
  workloadCoach(request: TaskAiWorkloadCoachRequest): Observable<TaskAiWorkloadCoachResponse> {
    return this.http
      .post<TaskAiWorkloadCoachResponse>(`${TASK_API}/tasks/ai/workload-coach`, request)
      .pipe(catchError((err) => throwError(() => err)));
  }

  /**
   * Structured definition-of-done for one task (assignee must match). May return 502 if model JSON is invalid.
   */
  definitionOfDone(request: TaskAiDefinitionOfDoneRequest): Observable<TaskAiDefinitionOfDoneResponse> {
    return this.http
      .post<TaskAiDefinitionOfDoneResponse>(`${TASK_API}/tasks/ai/definition-of-done`, request)
      .pipe(catchError((err) => throwError(() => err)));
  }

  /** Natural-language question over capped open-task snapshot for the freelancer. */
  askMyTasks(request: TaskAiAskTasksRequest): Observable<TaskAiAskTasksResponse> {
    return this.http
      .post<TaskAiAskTasksResponse>(`${TASK_API}/tasks/ai/ask-tasks`, request)
      .pipe(catchError((err) => throwError(() => err)));
  }

  /**
   * Stakeholder brief for project owner; server checks `clientUserId` against project client.
   * `planningDataWarning` is set when Planning MS could not be reached (task-only brief).
   */
  clientStatusBrief(request: TaskAiClientBriefRequest): Observable<TaskAiClientBriefResponse> {
    return this.http
      .post<TaskAiClientBriefResponse>(`${TASK_API}/tasks/ai/client-status-brief`, request)
      .pipe(catchError((err) => throwError(() => err)));
  }

  listSubtasks(parentTaskId: number): Observable<Subtask[]> {
    return this.http.get<Subtask[]>(`${TASK_API}/tasks/${parentTaskId}/subtasks`).pipe(catchError(() => of([])));
  }

  createSubtask(parentTaskId: number, body: SubtaskRequest): Observable<Subtask | null> {
    return this.http.post<Subtask>(`${TASK_API}/tasks/${parentTaskId}/subtasks`, body).pipe(catchError(() => of(null)));
  }

  updateSubtask(id: number, body: SubtaskRequest): Observable<Subtask | null> {
    return this.http.put<Subtask>(`${TASK_API}/subtasks/${id}`, body).pipe(catchError(() => of(null)));
  }

  patchSubtaskStatus(id: number, status: TaskStatus): Observable<Subtask | null> {
    return this.http
      .patch<Subtask>(`${TASK_API}/subtasks/${id}/status`, null, { params: { status } })
      .pipe(catchError(() => of(null)));
  }

  patchSubtaskDueDate(id: number, dueDate: string | null): Observable<Subtask | null> {
    const params: Record<string, string> = {};
    if (dueDate != null && dueDate !== '') params['dueDate'] = dueDate;
    return this.http
      .patch<Subtask>(`${TASK_API}/subtasks/${id}/due-date`, null, { params })
      .pipe(catchError(() => of(null)));
  }

  deleteSubtask(id: number): Observable<boolean> {
    return this.http.delete(`${TASK_API}/subtasks/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  /**
   * PDF work report. With lastDays + periodEnd, uses a rolling inclusive window ending that day
   * (e.g. lastDays=7 → 7 calendar days). Otherwise behaves as ISO week (weekStart).
   */
  downloadWorkReportPdf(params: {
    freelancerId: number;
    lastDays?: number;
    periodEnd?: string;
    projectId?: number | null;
    weekStart?: string | null;
  }): Observable<Blob> {
    const q = new URLSearchParams();
    q.set('freelancerId', String(params.freelancerId));
    if (params.lastDays != null) q.set('lastDays', String(params.lastDays));
    if (params.periodEnd?.trim()) q.set('periodEnd', params.periodEnd.trim());
    if (params.projectId != null) q.set('projectId', String(params.projectId));
    if (params.weekStart?.trim()) q.set('weekStart', params.weekStart.trim());
    return this.http.get(`${TASK_API}/tasks/reports/weekly.pdf?${q.toString()}`, { responseType: 'blob' });
  }

  /** Trigger browser download for a Blob (e.g. PDF). */
  saveBlobAsFile(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.rel = 'noopener';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}
