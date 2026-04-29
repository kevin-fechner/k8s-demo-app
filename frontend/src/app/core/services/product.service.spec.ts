import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { Product, ProductRequest } from '../models/product.model';
import { CursorPage } from '../models/pagination.model';
import { StockNumbers } from '../models/stock-numbers.model';

const mockProduct: Product = {
  id: 1,
  name: 'Widget',
  description: 'A widget',
  price: 9.99,
  stock: 100,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
};

const emptyPage: CursorPage<Product> = { data: [], nextCursor: null, hasMore: false, size: 10 };

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getPage', () => {
    it('sends GET /api/products with default size=10', () => {
      service.getPage().subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/products');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(emptyPage);
    });

    it('includes cursor param when provided', () => {
      service.getPage('cursor-abc').subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/products');
      expect(req.request.params.get('cursor')).toBe('cursor-abc');
      req.flush(emptyPage);
    });

    it('includes filter params when provided', () => {
      service
        .getPage(null, 5, { name: 'widget', inStock: true, minPrice: 1, maxPrice: 50 })
        .subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/products');
      expect(req.request.params.get('size')).toBe('5');
      expect(req.request.params.get('name')).toBe('widget');
      expect(req.request.params.get('inStock')).toBe('true');
      expect(req.request.params.get('minPrice')).toBe('1');
      expect(req.request.params.get('maxPrice')).toBe('50');
      req.flush(emptyPage);
    });

    it('omits optional params when not provided', () => {
      service.getPage().subscribe();
      const req = httpMock.expectOne((r) => r.url === '/api/products');
      expect(req.request.params.has('cursor')).toBe(false);
      expect(req.request.params.has('name')).toBe(false);
      req.flush(emptyPage);
    });
  });

  it('getById sends GET /api/products/:id', () => {
    let result: Product | undefined;
    service.getById(1).subscribe((p) => (result = p));
    const req = httpMock.expectOne('/api/products/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockProduct);
    expect(result).toEqual(mockProduct);
  });

  it('getStockNumbers sends GET /api/products/stock-numbers', () => {
    const stock: StockNumbers = { total: 10, inStock: 8, outOfStock: 2 };
    let result: StockNumbers | undefined;
    service.getStockNumbers().subscribe((s) => (result = s));
    const req = httpMock.expectOne('/api/products/stock-numbers');
    expect(req.request.method).toBe('GET');
    req.flush(stock);
    expect(result).toEqual(stock);
  });

  it('create sends POST /api/products with the request body', () => {
    const request: ProductRequest = {
      name: 'Widget',
      description: 'A widget',
      price: 9.99,
      stock: 100,
    };
    let result: Product | undefined;
    service.create(request).subscribe((p) => (result = p));
    const req = httpMock.expectOne('/api/products');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockProduct);
    expect(result).toEqual(mockProduct);
  });

  it('update sends PUT /api/products/:id with the request body', () => {
    const request: ProductRequest = {
      name: 'Updated',
      description: 'Updated desc',
      price: 19.99,
      stock: 50,
    };
    let result: Product | undefined;
    service.update(1, request).subscribe((p) => (result = p));
    const req = httpMock.expectOne('/api/products/1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ ...mockProduct, ...request });
    expect(result!.name).toBe('Updated');
  });

  it('delete sends DELETE /api/products/:id', () => {
    service.delete(1).subscribe();
    const req = httpMock.expectOne('/api/products/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
