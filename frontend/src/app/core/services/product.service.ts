import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductFilter, ProductRequest } from '../models/product.model';
import { CursorPage } from '../models/pagination.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/products`;

  getPage(cursor?: string | null, size = 10, filter?: ProductFilter): Observable<CursorPage<Product>> {
    let params = new HttpParams().set('size', size);
    if (cursor) params = params.set('cursor', cursor);
    if (filter?.name) params = params.set('name', filter.name);
    if (filter?.minPrice != null) params = params.set('minPrice', filter.minPrice);
    if (filter?.maxPrice != null) params = params.set('maxPrice', filter.maxPrice);
    if (filter?.inStock != null) params = params.set('inStock', filter.inStock);
    return this.http.get<CursorPage<Product>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, request);
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
