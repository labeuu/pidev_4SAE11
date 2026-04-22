import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { TaskManagement } from './task-management';
import {
  TaskService,
  Task,
  TaskHealth,
  TaskStatsDto,
  PageResponse,
} from '../../../core/services/task.service';
import { UserService } from '../../../core/services/user.service';
import { ProjectService } from '../../../core/services/project.service';
import { of } from 'rxjs';
import { Card } from '../../../shared/components/card/card';

/**
 * Jasmine unit tests for TaskManagement (admin) component. Verifies creation, load of health/dashboard/tasks,
 * and that filters use TaskService. Mocks TaskService, UserService, ProjectService.
 */
describe('TaskManagement', () => {
  let component: TaskManagement;
  let fixture: ComponentFixture<TaskManagement>;
  let taskService: jasmine.SpyObj<TaskService>;

  beforeEach(async () => {
    const taskSpy = jasmine.createSpyObj('TaskService', [
      'getTaskHealth',
      'getStatsDashboard',
      'getFilteredTasks',
      'createTask',
      'updateTask',
      'deleteTask',
    ]);
    taskSpy.getTaskHealth.and.returnValue(
      of({ service: 'task', status: 'UP', dbCount: 0, timestamp: '' } as TaskHealth)
    );
    taskSpy.getStatsDashboard.and.returnValue(
      of({
        totalTasks: 0,
        doneCount: 0,
        inProgressCount: 0,
        overdueCount: 0,
        completionPercentage: 0,
      } as TaskStatsDto)
    );
    taskSpy.getFilteredTasks.and.returnValue(
      of({ content: [] as Task[], totalElements: 0, totalPages: 0, size: 10, number: 0 } as PageResponse<Task>)
    );

    await TestBed.configureTestingModule({
      imports: [TaskManagement, ReactiveFormsModule, Card],
      providers: [
        { provide: TaskService, useValue: taskSpy },
        { provide: UserService, useValue: { getAll: () => of([]) } },
        { provide: ProjectService, useValue: { getAllProjects: () => of([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskManagement);
    component = fixture.componentInstance;
    taskService = TestBed.inject(TaskService) as jasmine.SpyObj<TaskService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call getTaskHealth and getStatsDashboard on init', () => {
    fixture.detectChanges();
    expect(taskService.getTaskHealth).toHaveBeenCalled();
    expect(taskService.getStatsDashboard).toHaveBeenCalled();
  });

  it('should call getFilteredTasks on init', () => {
    fixture.detectChanges();
    expect(taskService.getFilteredTasks).toHaveBeenCalled();
  });
});
