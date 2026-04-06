import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TaskService } from './task.service';

describe('TaskService', () => {
  let service: TaskService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TaskService],
    });
    service = TestBed.inject(TaskService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getFilteredTasks should call correct URL', () => {
    service.getFilteredTasks({ page: 0, size: 10 }).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks') && r.url.includes('page=0'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('getFilteredTasks should append openTasksOnly=true when requested', () => {
    service.getFilteredTasks({ page: 0, size: 10, openTasksOnly: true }).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('openTasksOnly=true'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('getTaskById should call correct URL', () => {
    service.getTaskById(1).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks/1'));
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('getTasksByProjectId should call correct URL', () => {
    service.getTasksByProjectId(5).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks/project/5'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('createTask should POST', () => {
    service.createTask({ projectId: 1, title: 'Test' }).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks'));
    expect(req.request.method).toBe('POST');
  });

  it('getSubtaskProgress with no taskIds should not HTTP and return empty map', (done) => {
    service.getSubtaskProgress(5, []).subscribe((m) => {
      expect(m).toEqual({});
      done();
    });
  });

  it('getSubtaskProgress should GET with taskIds query and map rows by parentTaskId', () => {
    let result: Record<number, { total: number; completed: number }> | undefined;
    service.getSubtaskProgress(3, [10, 11]).subscribe((m) => (result = m));

    const req = httpMock.expectOne(
      (r) =>
        r.url.includes('/task/api/tasks/assignee/3/subtask-progress') &&
        r.params.get('taskIds') === '10,11'
    );
    expect(req.request.method).toBe('GET');
    req.flush([
      { parentTaskId: 10, total: 4, completed: 1 },
      { parentTaskId: 11, total: 0, completed: 0 },
    ]);

    expect(result).toEqual({
      10: { total: 4, completed: 1 },
      11: { total: 0, completed: 0 },
    });
  });

  it('getProjectActivity should GET assignee project-activity', () => {
    service.getProjectActivity(8).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks/assignee/8/project-activity'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getExtendedStatsByFreelancer should GET extended stats URL with optional from/to', () => {
    service.getExtendedStatsByFreelancer(12, { from: '2026-01-01', to: '2026-01-31' }).subscribe();
    const req = httpMock.expectOne(
      (r) =>
        r.url.includes('/task/api/tasks/stats/extended/freelancer/12') &&
        r.url.includes('from=2026-01-01') &&
        r.url.includes('to=2026-01-31')
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      totalTasks: 0,
      doneCount: 0,
      inProgressCount: 0,
      inReviewCount: 0,
      todoCount: 0,
      cancelledCount: 0,
      overdueCount: 0,
      completionPercentage: 0,
      unassignedCount: 0,
      priorityBreakdown: [],
    });
  });

  it('getExtendedStatsByFreelancer without options should GET base URL without query string', () => {
    service.getExtendedStatsByFreelancer(3).subscribe();
    const req = httpMock.expectOne(
      (r) => r.url.includes('/task/api/tasks/stats/extended/freelancer/3') && !r.url.includes('?')
    );
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('getExtendedStatsByProject should GET extended stats URL for project', () => {
    service.getExtendedStatsByProject(42).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks/stats/extended/project/42'));
    expect(req.request.method).toBe('GET');
    req.flush({ totalTasks: 0, doneCount: 0, priorityBreakdown: [] });
  });

  it('downloadWorkReportPdf should GET weekly.pdf with rolling window params', () => {
    service.downloadWorkReportPdf({ freelancerId: 5, lastDays: 7, periodEnd: '2026-04-10' }).subscribe();
    const req = httpMock.expectOne(
      (r) =>
        r.url.includes('/task/api/tasks/reports/weekly.pdf') &&
        r.url.includes('freelancerId=5') &&
        r.url.includes('lastDays=7') &&
        r.url.includes('periodEnd=2026-04-10')
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
