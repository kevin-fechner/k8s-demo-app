import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, OrderFilter, OrderRequest, UpdateStatusRequest } from '../models/order.model';
import { CursorPage } from '../models/pagination.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/orders`;

  getPage(cursor?: string | null, size = 10, filter?: OrderFilter): Observable<CursorPage<Order>> {
    let params = new HttpParams().set('size', size);
    if (cursor) params = params.set('cursor', cursor);
    if (filter?.status) params = params.set('status', filter.status);
    if (filter?.customerEmail) params = params.set('customerEmail', filter.customerEmail);
    if (filter?.fromDate) params = params.set('fromDate', filter.fromDate);
    if (filter?.toDate) params = params.set('toDate', filter.toDate);
    return this.http.get<CursorPage<Order>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  create(request: OrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, request);
  }

  updateStatus(id: number, request: UpdateStatusRequest): Observable<Order> {
    return this.http.patch<Order>(`${this.apiUrl}/${id}/status`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
