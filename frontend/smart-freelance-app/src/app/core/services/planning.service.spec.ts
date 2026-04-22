import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PlanningService } from './planning.service';
import { environment } from '../../../environments/environment';

const BASE = `${environment.apiGatewayUrl}/planning/api`;

/**
 * Jasmine unit tests for PlanningService. Verifies that each method sends the correct HTTP request
 * and maps the response (or error) as documented. Uses HttpClientTestingModule to avoid real HTTP.
 */
describe('PlanningService', () => {
  let service: PlanningService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PlanningService],
    });
    service = TestBed.inject(PlanningService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAllProgressUpdates', () => {
    it('should GET progress-updates and return content array', (done) => {
      service.getAllProgressUpdates().subscribe((list) => {
        expect(Array.isArray(list)).toBe(true);
        expect(list.length).toBe(1);
        expect(list[0].id).toBe(1);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates`);
      expect(req.request.method).toBe('GET');
      req.flush({ content: [{ id: 1, title: 'T', progressPercentage: 50 }], totalElements: 1 });
    });

    it('should return empty array on error', (done) => {
      service.getAllProgressUpdates().subscribe((list) => {
        expect(list).toEqual([]);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates`);
      req.flush('error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getFilteredProgressUpdates', () => {
    it('should GET with query params and return PageResponse', (done) => {
      service.getFilteredProgressUpdates({ page: 0, size: 10, projectId: 1 }).subscribe((page) => {
        expect(page.content).toBeDefined();
        expect(page.totalElements).toBe(0);
        done();
      });
      const req = httpMock.expectOne((r) => r.url.startsWith(`${BASE}/progress-updates`) && r.url.includes('projectId=1'));
      expect(req.request.method).toBe('GET');
      req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    });
  });

  describe('getDashboardStats', () => {
    it('should GET stats/dashboard and return DTO', (done) => {
      service.getDashboardStats().subscribe((stats) => {
        expect(stats).toBeTruthy();
        expect(stats!.totalUpdates).toBe(5);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates/stats/dashboard`);
      req.flush({ totalUpdates: 5, totalComments: 2, averageProgressPercentage: 60, distinctProjectCount: 3, distinctFreelancerCount: 2 });
    });

    it('should return null on error', (done) => {
      service.getDashboardStats().subscribe((stats) => {
        expect(stats).toBeNull();
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates/stats/dashboard`);
      req.flush('error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getProgressUpdateById', () => {
    it('should GET by id and return update', (done) => {
      service.getProgressUpdateById(1).subscribe((u) => {
        expect(u).toBeTruthy();
        expect(u!.id).toBe(1);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates/1`);
      req.flush({ id: 1, title: 'T', progressPercentage: 50 });
    });

    it('should return null on error', (done) => {
      service.getProgressUpdateById(999).subscribe((u) => {
        expect(u).toBeNull();
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates/999`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('createProgressUpdate', () => {
    it('should POST and return created update', (done) => {
      const body = { projectId: 1, contractId: null, freelancerId: 10, title: 'New', progressPercentage: 25 };
      service.createProgressUpdate(body).subscribe({
        next: (u) => {
          expect(u.id).toBe(1);
          expect(u.title).toBe('New');
          done();
        },
        error: () => done.fail(),
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, title: 'New', progressPercentage: 25 });
    });
  });

  describe('updateProgressUpdate', () => {
    it('should PUT and return updated update', (done) => {
      const body = { projectId: 1, contractId: null, freelancerId: 10, title: 'Updated', progressPercentage: 60 };
      service.updateProgressUpdate(1, body).subscribe((u) => {
        expect(u).toBeTruthy();
        expect(u!.title).toBe('Updated');
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates/1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ id: 1, title: 'Updated', progressPercentage: 60 });
    });
  });

  describe('deleteProgressUpdate', () => {
    it('should DELETE and return true on success', (done) => {
      service.deleteProgressUpdate(1).subscribe((ok) => {
        expect(ok).toBe(true);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('should return false on error', (done) => {
      service.deleteProgressUpdate(1).subscribe((ok) => {
        expect(ok).toBe(false);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-updates/1`);
      req.flush('error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getCalendarEvents', () => {
    it('should GET calendar/events and return array', (done) => {
      service.getCalendarEvents({ timeMin: '2025-01-01', timeMax: '2025-12-31' }).subscribe((events) => {
        expect(Array.isArray(events)).toBe(true);
        expect(events.length).toBe(0);
        done();
      });
      const req = httpMock.expectOne((r) => r.url.startsWith(`${BASE}/calendar/events`));
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getAllComments', () => {
    it('should GET progress-comments and return array', (done) => {
      service.getAllComments().subscribe((comments) => {
        expect(Array.isArray(comments)).toBe(true);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-comments`);
      req.flush([]);
    });
  });

  describe('createComment', () => {
    it('should POST to progress-comments', (done) => {
      service.createComment({ progressUpdateId: 1, userId: 5, message: 'Hi' }).subscribe((c) => {
        expect(c).toBeTruthy();
        expect(c!.message).toBe('Hi');
        done();
      });
      const req = httpMock.expectOne(`${BASE}/progress-comments`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1, userId: 5, message: 'Hi' });
    });
  });

  describe('getPlanningHealth', () => {
    it('should GET planning/health and return health object', (done) => {
      service.getPlanningHealth().subscribe((h) => {
        expect(h).toBeTruthy();
        expect(h!.service).toBe('planning');
        done();
      });
      const req = httpMock.expectOne(`${BASE}/planning/health`);
      req.flush({ service: 'planning', status: 'UP', timestamp: new Date().toISOString() });
    });
  });

  describe('GitHub', () => {
    it('isGitHubEnabled should GET github/enabled', (done) => {
      service.isGitHubEnabled().subscribe((enabled) => {
        expect(enabled).toBe(true);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/github/enabled`);
      req.flush(true);
    });

    it('getGitHubBranches should GET repos/owner/repo/branches', (done) => {
      service.getGitHubBranches('o', 'r').subscribe((branches) => {
        expect(Array.isArray(branches)).toBe(true);
        done();
      });
      const req = httpMock.expectOne(`${BASE}/github/repos/o/r/branches`);
      req.flush([]);
    });
  });
});
