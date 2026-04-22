import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Calendar } from './calendar';
import { PlanningService } from '../../../core/services/planning.service';
import { MeetingService } from '../../../core/services/meeting.service';
import { AuthService } from '../../../core/services/auth.service';
import { of } from 'rxjs';

/**
 * Jasmine unit tests for Calendar component. Verifies that the component loads events via PlanningService
 * on init and displays the month. Mocks PlanningService and AuthService.
 */
describe('Calendar', () => {
  let component: Calendar;
  let fixture: ComponentFixture<Calendar>;
  let planningService: jasmine.SpyObj<PlanningService>;

  beforeEach(async () => {
    const planningSpy = jasmine.createSpyObj('PlanningService', ['getCalendarEvents']);
    planningSpy.getCalendarEvents.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Calendar],
      providers: [
        { provide: PlanningService, useValue: planningSpy },
        { provide: MeetingService, useValue: { getMyMeetings: () => of([]) } },
        { provide: AuthService, useValue: { getUserId: () => 1, getUserRole: () => 'FREELANCER' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Calendar);
    component = fixture.componentInstance;
    planningService = TestBed.inject(PlanningService) as jasmine.SpyObj<PlanningService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call getCalendarEvents on init', () => {
    fixture.detectChanges();
    expect(planningService.getCalendarEvents).toHaveBeenCalled();
  });

  it('should have weekdayLabels', () => {
    expect(component.weekdayLabels.length).toBe(7);
    expect(component.weekdayLabels[0]).toBe('Sun');
  });

  it('monthTitle should return formatted month and year', () => {
    component.currentMonth = new Date(2025, 0, 1); // Jan 2025
    expect(component.monthTitle).toContain('January');
    expect(component.monthTitle).toContain('2025');
  });
});
