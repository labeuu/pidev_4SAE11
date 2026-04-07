import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ProjectTasks } from './project-tasks';
import { TaskService, PageResponse, Task, TaskStatsExtendedDto } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { of } from 'rxjs';
import { Card } from '../../../shared/components/card/card';

describe('ProjectTasks', () => {
  let component: ProjectTasks;
  let fixture: ComponentFixture<ProjectTasks>;
  let taskService: jasmine.SpyObj<TaskService>;

  const emptyPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0 };

  const minimalExtended: TaskStatsExtendedDto = {
    totalTasks: 3,
    doneCount: 1,
    inProgressCount: 0,
    inReviewCount: 1,
    todoCount: 1,
    cancelledCount: 0,
    overdueCount: 0,
    completionPercentage: 33,
    unassignedCount: 0,
    priorityBreakdown: [
      { priority: 'LOW', count: 1 },
      { priority: 'MEDIUM', count: 1 },
      { priority: 'HIGH', count: 1 },
      { priority: 'URGENT', count: 0 },
    ],
  };

  beforeEach(async () => {
    const taskSpy = jasmine.createSpyObj<TaskService>('TaskService', [
      'getFilteredTasks',
      'getExtendedStatsByProject',
      'getOverdueTasks',
      'listSubtasks',
    ]);
    taskSpy.getFilteredTasks.and.returnValue(of(emptyPage as PageResponse<Task>));
    taskSpy.getExtendedStatsByProject.and.returnValue(of(minimalExtended));
    taskSpy.getOverdueTasks.and.returnValue(of([]));
    taskSpy.listSubtasks.and.returnValue(of([]));

    const authStub = {
      getUserId: () => 1,
      getUserRole: () => 'CLIENT',
      isClient: () => true,
    };

    await TestBed.configureTestingModule({
      imports: [ProjectTasks, ReactiveFormsModule, Card],
      providers: [
        { provide: TaskService, useValue: taskSpy },
        { provide: AuthService, useValue: authStub },
        { provide: ProjectService, useValue: { getByClientId: () => of([{ id: 1, title: 'Proj 1' }]) } },
        {
          provide: UserService,
          useValue: {
            getAll: () => of([]),
            getById: () => of(null),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => null as string | null } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectTasks);
    component = fixture.componentInstance;
    taskService = TestBed.inject(TaskService) as jasmine.SpyObj<TaskService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load extended stats and paginated tasks when a project is selected on init', () => {
    fixture.detectChanges();
    expect(taskService.getExtendedStatsByProject).toHaveBeenCalledWith(1);
    expect(taskService.getFilteredTasks).toHaveBeenCalled();
    const args = taskService.getFilteredTasks.calls.mostRecent().args[0];
    expect(args.projectId).toBe(1);
    expect(args.page).toBe(0);
  });

  it('applyStatusDrilldown should request filtered tasks with status', () => {
    fixture.detectChanges();
    taskService.getFilteredTasks.calls.reset();
    component.applyStatusDrilldown('TODO');
    expect(taskService.getFilteredTasks).toHaveBeenCalled();
    const params = taskService.getFilteredTasks.calls.mostRecent().args[0];
    expect(params.status).toBe('TODO');
    expect(params.projectId).toBe(1);
  });

  it('applyOverdueDrilldown should load overdue list for project', () => {
    fixture.detectChanges();
    taskService.getOverdueTasks.calls.reset();
    component.applyOverdueDrilldown();
    expect(taskService.getOverdueTasks).toHaveBeenCalledWith(1, null);
  });
});
