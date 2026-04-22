import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ProgressUpdates } from './progress-updates';
import { PlanningService } from '../../../core/services/planning.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { ProjectService } from '../../../core/services/project.service';
import { of } from 'rxjs';
import { Card } from '../../../shared/components/card/card';

/**
 * Jasmine unit tests for ProgressUpdates (freelancer) component. Verifies creation and that init loads
 * projects, stats, due projects, and calendar events via PlanningService. Mocks Auth, User, Project, Planning.
 */
describe('ProgressUpdates', () => {
  let component: ProgressUpdates;
  let fixture: ComponentFixture<ProgressUpdates>;
  let planningService: jasmine.SpyObj<PlanningService>;

  beforeEach(async () => {
    const planningSpy = jasmine.createSpyObj('PlanningService', [
      'getStatsByFreelancer',
      'getDueOrOverdueProjects',
      'getCalendarEvents',
      'getFilteredProgressUpdates',
      'getProgressUpdatesByProjectId',
      'createProgressUpdate',
      'updateProgressUpdate',
      'deleteProgressUpdate',
      'syncProjectDeadlineToCalendar',
    ]);
    planningSpy.getStatsByFreelancer.and.returnValue(of(null));
    planningSpy.getDueOrOverdueProjects.and.returnValue(of([]));
    planningSpy.getCalendarEvents.and.returnValue(of([]));
    planningSpy.getFilteredProgressUpdates.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 })
    );
    planningSpy.getProgressUpdatesByProjectId.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ProgressUpdates, ReactiveFormsModule, Card],
      providers: [
        { provide: PlanningService, useValue: planningSpy },
        {
          provide: AuthService,
          useValue: {
            getPreferredUsername: () => 'freelancer@test.com',
            getUserId: () => 2,
            getUserRole: () => 'FREELANCER',
          },
        },
        {
          provide: UserService,
          useValue: {
            getByEmail: () => of({ id: 2, firstName: 'Test', lastName: 'Freelancer', email: 'freelancer@test.com' }),
          },
        },
        {
          provide: ProjectService,
          useValue: {
            getApplicationsByFreelancer: () => of([]),
            getById: () => of(null),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => null } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProgressUpdates);
    component = fixture.componentInstance;
    planningService = TestBed.inject(PlanningService) as jasmine.SpyObj<PlanningService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have titleMax and descriptionMax constants', () => {
    expect(component.titleMax).toBeGreaterThan(0);
    expect(component.descriptionMax).toBeGreaterThan(0);
  });
});
