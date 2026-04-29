import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { Order, OrderRequest, UpdateStatusRequest } from '../models/order.model';
import { CursorPage } from '../models/pagination.model';

const mockOrder: Order = {
  id: 1,
  customerName: 'Alice',
  customerEmail: 'alice@example.com',
  status: 'PENDING',
  totalAmount: 99.99,
  notes: '',
  items: [],
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
};

const emptyPage: CursorPage<Order> = { data: [], nextCursor: null, hasMore: false, size: 10 };

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getPage', () => {
    it('sends GET /api/orders with default size=10', () => {
      service.getPage().subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/orders');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(emptyPage);
    });

    it('includes cursor param when provided', () => {
      service.getPage('cursor-xyz').subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/orders');
      expect(req.request.params.get('cursor')).toBe('cursor-xyz');
      req.flush(emptyPage);
    });

    it('includes filter params when provided', () => {
      service
        .getPage(null, 5, {
          status: 'PENDING',
          customerEmail: 'a@b.com',
          fromDate: '2024-01-01',
          toDate: '2024-12-31',
        })
        .subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/orders');
      expect(req.request.params.get('size')).toBe('5');
      expect(req.request.params.get('status')).toBe('PENDING');
      expect(req.request.params.get('customerEmail')).toBe('a@b.com');
      expect(req.request.params.get('fromDate')).toBe('2024-01-01');
      expect(req.request.params.get('toDate')).toBe('2024-12-31');
      req.flush(emptyPage);
    });

    it('omits optional params when not provided', () => {
      service.getPage().subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/orders');
      expect(req.request.params.has('cursor')).toBe(false);
      expect(req.request.params.has('status')).toBe(false);
      req.flush(emptyPage);
    });
  });

  it('getById sends GET /api/orders/:id', () => {
    let result: Order | undefined;
    service.getById(1).subscribe((o) => (result = o));
    const req = httpMock.expectOne('/api/orders/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockOrder);
    expect(result).toEqual(mockOrder);
  });

  it('create sends POST /api/orders with the request body', () => {
    const request: OrderRequest = {
      customerName: 'Alice',
      customerEmail: 'alice@example.com',
      items: [{ productId: 1, quantity: 2 }],
    };
    let result: Order | undefined;
    service.create(request).subscribe((o) => (result = o));
    const req = httpMock.expectOne('/api/orders');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockOrder);
    expect(result).toEqual(mockOrder);
  });

  it('updateStatus sends PATCH /api/orders/:id/status', () => {
    const request: UpdateStatusRequest = { status: 'CONFIRMED' };
    let result: Order | undefined;
    service.updateStatus(1, request).subscribe((o) => (result = o));
    const req = httpMock.expectOne('/api/orders/1/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(request);
    req.flush({ ...mockOrder, status: 'CONFIRMED' });
    expect(result!.status).toBe('CONFIRMED');
  });

  it('delete sends DELETE /api/orders/:id', () => {
    service.delete(1).subscribe();
    const req = httpMock.expectOne('/api/orders/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});