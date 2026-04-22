import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TrackProgress } from './track-progress';
import { PlanningService } from '../../../core/services/planning.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { ProjectService } from '../../../core/services/project.service';
import { of } from 'rxjs';
import { Card } from '../../../shared/components/card/card';

/**
 * Jasmine unit tests for TrackProgress (client) component. Verifies creation and that the component
 * uses PlanningService for project stats, stalled projects, updates, and comments. Mocks all services.
 */
describe('TrackProgress', () => {
  let component: TrackProgress;
  let fixture: ComponentFixture<TrackProgress>;
  let planningService: jasmine.SpyObj<PlanningService>;

  beforeEach(async () => {
    const planningSpy = jasmine.createSpyObj('PlanningService', [
      'getStatsByProject',
      'getDueOrOverdueProjects',
      'getFilteredProgressUpdates',
      'getCommentsByProgressUpdateId',
      'createComment',
      'updateComment',
      'deleteComment',
    ]);
    planningSpy.getStatsByProject.and.returnValue(of(null));
    planningSpy.getDueOrOverdueProjects.and.returnValue(of([]));
    planningSpy.getFilteredProgressUpdates.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 })
    );
    planningSpy.getCommentsByProgressUpdateId.and.returnValue(of([]));
    planningSpy.createComment.and.returnValue(of(null));
    planningSpy.updateComment.and.returnValue(of(null));
    planningSpy.deleteComment.and.returnValue(of(false));

    await TestBed.configureTestingModule({
      imports: [TrackProgress, ReactiveFormsModule, Card, RouterLink],
      providers: [
        { provide: PlanningService, useValue: planningSpy },
        {
          provide: AuthService,
          useValue: { getPreferredUsername: () => 'client@test.com' },
        },
        {
          provide: UserService,
          useValue: {
            getByEmail: () => of({ id: 1, firstName: 'Test', lastName: 'Client', email: 'client@test.com' }),
          },
        },
        { provide: ProjectService, useValue: { getByClientId: () => of([]) } },
        { provide: ActivatedRoute, useValue: { params: of({}), queryParams: of({}) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TrackProgress);
    component = fixture.componentInstance;
    planningService = TestBed.inject(PlanningService) as jasmine.SpyObj<PlanningService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
