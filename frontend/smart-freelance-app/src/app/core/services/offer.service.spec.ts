import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OfferService } from './offer.service';
import { environment } from '../../../environments/environment';

const OFFER_API = `${environment.apiGatewayUrl}/offer/api/offers`;
const APPLICATION_API = `${environment.apiGatewayUrl}/offer/api/applications`;

describe('OfferService', () => {
  let service: OfferService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OfferService],
    });
    service = TestBed.inject(OfferService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getActiveOffers should GET with page and size', () => {
    service.getActiveOffers(1, 5).subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === OFFER_API && r.params.get('page') === '1' && r.params.get('size') === '5'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 5, number: 1, first: false, last: true });
  });

  it('getOfferById should GET single offer', () => {
    service.getOfferById(42).subscribe();
    const req = httpMock.expectOne(`${OFFER_API}/42`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 42 } as any);
  });

  it('getOfferById should return null on HTTP error', (done) => {
    service.getOfferById(99).subscribe((v) => {
      expect(v).toBeNull();
      done();
    });
    const req = httpMock.expectOne(`${OFFER_API}/99`);
    req.flush('Not found', { status: 404, statusText: 'Not Found' });
  });

  it('getFeaturedOffers should GET featured endpoint', () => {
    service.getFeaturedOffers().subscribe();
    const req = httpMock.expectOne(`${OFFER_API}/featured`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('searchOffers should POST filter body', () => {
    const filter = { page: 0, size: 10, keyword: 'java' };
    service.searchOffers(filter).subscribe();
    const req = httpMock.expectOne(`${OFFER_API}/search`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(filter);
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0, first: true, last: true });
  });

  it('applyToOffer should POST application', () => {
    const body = { offerId: 1, clientId: 2, message: 'Hi', proposedBudget: 100 };
    service.applyToOffer(body).subscribe();
    const req = httpMock.expectOne(APPLICATION_API);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 1, ...body, status: 'PENDING', appliedAt: '2025-01-01' } as any);
  });

  it('getFreelancerDashboardStats should GET dashboard path', () => {
    service.getFreelancerDashboardStats(7).subscribe();
    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiGatewayUrl}/offer/api/dashboard/freelancer/7`
    );
    expect(req.request.method).toBe('GET');
    req.flush({ activeOffers: 0, pendingApplications: 0 } as any);
  });
});
