import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { MyTasks } from './my-tasks';
import { Subtask, Task, TaskService } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectApplicationService } from '../../../core/services/project-application.service';
import { AiModelStatusService } from '../../../core/services/aimodel-status.service';
import { of } from 'rxjs';
import { Card } from '../../../shared/components/card/card';

/**
 * Jasmine unit tests for MyTasks (freelancer) component.
 */
describe('MyTasks', () => {
  let component: MyTasks;
  let fixture: ComponentFixture<MyTasks>;
  let taskService: jasmine.SpyObj<TaskService>;

  beforeEach(async () => {
    const taskSpy = jasmine.createSpyObj('TaskService', [
      'getFilteredTasks',
      'getSubtaskProgress',
      'getProjectActivity',
      'getOverdueTasks',
      'getExtendedStatsByFreelancer',
      'patchStatus',
      'createTask',
      'updateTask',
      'deleteTask',
      'listSubtasks',
      'createSubtask',
      'updateSubtask',
      'patchSubtaskStatus',
      'deleteSubtask',
      'workloadCoach',
      'askMyTasks',
      'definitionOfDone',
    ]);
    taskSpy.getFilteredTasks.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 })
    );
    taskSpy.getSubtaskProgress.and.returnValue(of({}));
    taskSpy.getProjectActivity.and.returnValue(of([]));
    taskSpy.getOverdueTasks.and.returnValue(of([]));
    taskSpy.getExtendedStatsByFreelancer.and.returnValue(of(null));
    taskSpy.workloadCoach.and.returnValue(of({ summaryMarkdown: '', highlights: [] }));
    taskSpy.askMyTasks.and.returnValue(of({ answerMarkdown: '', citedTaskIds: [] }));
    taskSpy.definitionOfDone.and.returnValue(of({ criteria: [], assumptions: [] }));

    const authStub = {
      getUserId: () => 1,
      isFreelancer: () => true,
    };

    const projectStub = {
      getAllProjects: () => of([]),
    };

    const applicationStub = {
      getApplicationsByFreelance: () => of([]),
    };

    const aiStub = {
      getLiveStatus: jasmine.createSpy('getLiveStatus').and.returnValue(
        of({ snapshot: null as never, reachabilityError: false })
      ),
    };

    await TestBed.configureTestingModule({
      imports: [MyTasks, ReactiveFormsModule, FormsModule, Card],
      providers: [
        provideRouter([]),
        { provide: TaskService, useValue: taskSpy },
        { provide: AuthService, useValue: authStub },
        { provide: ProjectService, useValue: projectStub },
        { provide: ProjectApplicationService, useValue: applicationStub },
        { provide: AiModelStatusService, useValue: aiStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyTasks);
    component = fixture.componentInstance;
    taskService = TestBed.inject(TaskService) as jasmine.SpyObj<TaskService>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('defaults dashboard tab to tasks', () => {
    expect(component.dashboardTab).toBe('tasks');
  });

  it('setDashboardTab switches active panel', () => {
    component.setDashboardTab('statistics');
    expect(component.dashboardTab).toBe('statistics');
    component.setDashboardTab('ai');
    expect(component.dashboardTab).toBe('ai');
    fixture.detectChanges();
    const statsPanel = fixture.nativeElement.querySelector('#my-tasks-panel-statistics');
    const aiPanel = fixture.nativeElement.querySelector('#my-tasks-panel-ai');
    expect(statsPanel.classList.contains('my-tasks-panel--active')).toBe(false);
    expect(aiPanel.classList.contains('my-tasks-panel--active')).toBe(true);
  });

  it('clicking Statistics tab activates statistics panel', () => {
    fixture.detectChanges();
    const statsTab = fixture.nativeElement.querySelector('#my-tasks-tab-statistics') as HTMLButtonElement;
    statsTab?.click();
    fixture.detectChanges();
    const statsPanel = fixture.nativeElement.querySelector('#my-tasks-panel-statistics');
    expect(component.dashboardTab).toBe('statistics');
    expect(statsPanel.classList.contains('my-tasks-panel--active')).toBe(true);
  });

  it('should call getFilteredTasks on init', () => {
    fixture.detectChanges();
    expect(taskService.getFilteredTasks).toHaveBeenCalled();
  });

  it('should call getProjectActivity on init when user id is set', () => {
    fixture.detectChanges();
    expect(taskService.getProjectActivity).toHaveBeenCalledWith(1);
  });

  it('subtaskProgressPercent should return 0 when no progress', () => {
    expect(component.subtaskProgressPercent(99)).toBe(0);
  });

  it('subtaskProgressPercent should round completed over total', () => {
    component.subtaskProgressByTaskId[5] = { total: 3, completed: 1 };
    expect(component.subtaskProgressPercent(5)).toBe(33);
  });

  it('formatProjectActivityHint should be empty when no activity row', () => {
    expect(component.formatProjectActivityHint(1)).toBe('');
  });

  it('formatProjectActivityHint should format last activity and open count', () => {
    component.projectActivityByProjectId[2] = {
      projectId: 2,
      lastActivityAt: '2026-04-01T10:00:00',
      openTaskCount: 4,
    };
    const hint = component.formatProjectActivityHint(2);
    expect(hint).toContain('open task');
    expect(hint).toContain('4');
  });

  it('openAddSubtask should set modal parent and clear editing', () => {
    const parent = { id: 42, title: 'Root', projectId: 1 } as Task;
    component.openAddSubtask(parent);
    expect(component.subtaskModalParent).toBe(parent);
    expect(component.subtaskModalEditing).toBeNull();
    expect(component.subtaskForm.get('title')?.value).toBe('');
  });

  it('openEditSubtask should patch form from subtask', () => {
    const parent = { id: 7, title: 'Root', projectId: 1 } as Task;
    const st: Subtask = {
      id: 99,
      parentTaskId: 7,
      projectId: 1,
      title: 'Sub',
      description: 'd',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      assigneeId: 1,
      dueDate: '2026-05-15',
      orderIndex: 0,
    };
    component.openEditSubtask(parent, st);
    expect(component.subtaskModalEditing).toBe(st);
    expect(component.subtaskForm.get('title')?.value).toBe('Sub');
    expect(component.subtaskForm.get('status')?.value).toBe('IN_PROGRESS');
  });

  it('filterFingerprint should include openTasksOnly', () => {
    component.filterOpenTasksOnly = true;
    const fp = JSON.parse((component as unknown as { filterFingerprint(): string }).filterFingerprint());
    expect(fp.openTasksOnly).toBe(true);
  });

  it('applyOverdueDrilldown should switch to overdue mode and load overdue tasks', () => {
    taskService.getOverdueTasks.calls.reset();
    component.applyOverdueDrilldown();
    expect(component.listViewMode).toBe('overdue');
    expect(component.statsFocusKey).toBe('overdue');
    expect(taskService.getOverdueTasks).toHaveBeenCalledWith(null, 1);
  });

  it('applyActiveDrilldown should request openTasksOnly on paginated list', () => {
    taskService.getFilteredTasks.calls.reset();
    component.applyActiveDrilldown();
    expect(component.listViewMode).toBe('paginated');
    expect(component.filterOpenTasksOnly).toBe(true);
    expect(component.statsFocusKey).toBe('active');
    expect(taskService.getFilteredTasks).toHaveBeenCalled();
    const args = taskService.getFilteredTasks.calls.mostRecent().args[0];
    expect(args.openTasksOnly).toBe(true);
    expect(args.status).toBeNull();
  });

  it('applyStatusDrilldown should set status filter and clear openTasksOnly', () => {
    component.filterOpenTasksOnly = true;
    taskService.getFilteredTasks.calls.reset();
    component.applyStatusDrilldown('DONE');
    expect(component.filterOpenTasksOnly).toBe(false);
    expect(component.statsFocusKey).toBe('status_DONE');
    expect(taskService.getFilteredTasks.calls.mostRecent().args[0]).toEqual(
      jasmine.objectContaining({ status: 'DONE', openTasksOnly: null })
    );
  });

  it('resetStatsListView should exit overdue drilldown and clear stat focus', () => {
    component.applyOverdueDrilldown();
    component.resetStatsListView();
    expect(component.listViewMode).toBe('paginated');
    expect(component.statsFocusKey).toBeNull();
    expect(component.filterOpenTasksOnly).toBe(false);
  });

  it('showStatsDrilldownBanner should be true when stat drilldown is active', () => {
    expect(component.showStatsDrilldownBanner).toBe(false);
    component.applyPriorityDrilldown('HIGH');
    expect(component.showStatsDrilldownBanner).toBe(true);
    expect(component.statsDrilldownBannerMessage).toContain('priority');
  });

  it('isOverdueDueDate should be true for past date on open task', () => {
    expect(component.isOverdueDueDate('2020-06-01', 'TODO')).toBe(true);
  });

  it('isOverdueDueDate should be false for completed tasks', () => {
    expect(component.isOverdueDueDate('2020-06-01', 'DONE')).toBe(false);
  });

  it('dueDateDisplayLine should mention overdue when past due', () => {
    expect(component.dueDateDisplayLine('2020-06-01', 'IN_PROGRESS')).toContain('overdue');
  });

  it('canEditOrDeleteTask should be false for DONE and CANCELLED', () => {
    expect(
      component.canEditOrDeleteTask({
        id: 1,
        projectId: 1,
        contractId: null,
        title: 't',
        description: null,
        status: 'DONE',
        priority: 'LOW',
        assigneeId: 1,
        dueDate: null,
        orderIndex: 0,
        createdBy: null,
        createdAt: '',
        updatedAt: '',
      })
    ).toBe(false);
    expect(
      component.canEditOrDeleteTask({
        id: 1,
        projectId: 1,
        contractId: null,
        title: 't',
        description: null,
        status: 'CANCELLED',
        priority: 'LOW',
        assigneeId: 1,
        dueDate: null,
        orderIndex: 0,
        createdBy: null,
        createdAt: '',
        updatedAt: '',
      })
    ).toBe(false);
    expect(
      component.canEditOrDeleteTask({
        id: 1,
        projectId: 1,
        contractId: null,
        title: 't',
        description: null,
        status: 'TODO',
        priority: 'LOW',
        assigneeId: 1,
        dueDate: null,
        orderIndex: 0,
        createdBy: null,
        createdAt: '',
        updatedAt: '',
      })
    ).toBe(true);
  });
});
