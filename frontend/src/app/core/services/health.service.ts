import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable, catchError, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiUrlService } from './api-url.service';

export interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  uptime?: string;
  memoryUsed?: string;
  memoryMax?: string;
  diskSpace?: string;
  kafka?: string;
  db?: string;
  details?: Record<string, any>;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrlService = inject(ApiUrlService);

  private get apiUrl(): string {
    return `${this.apiUrlService.baseUrl}`;
  }

  private readonly services = [
    { name: 'API Gateway',           path: 'gateway' },
    { name: 'Order Service',         path: 'order-service' },
    { name: 'Product Service',       path: 'product-service' },
    { name: 'Notification Service',  path: 'notification-service' },
  ];

  getAllHealth(): Observable<ServiceHealth[]> {
    const requests = this.services.map(svc =>
      forkJoin({
        health:  this.fetchHealth(svc.path),
        metrics: this.fetchMetrics(svc.path),
      }).pipe(
        map(({ health, metrics }) => this.mapToServiceHealth(svc.name, health, metrics)),
        catchError(() => of(this.unknownService(svc.name)))
      )
    );
    return forkJoin(requests);
  }

  private fetchHealth(path: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/${path}/actuator/health`)
      .pipe(catchError(() => of({ status: 'DOWN' })));
  }

  private fetchMetrics(path: string): Observable<any> {
    const base = `${this.apiUrl}/api/${path}/actuator/metrics`;
    return forkJoin({
      memUsed: this.http.get(`${base}/jvm.memory.used`).pipe(catchError(() => of(null))),
      memMax:  this.http.get(`${base}/jvm.memory.max`).pipe(catchError(() => of(null))),
      uptime:  this.http.get(`${base}/process.uptime`).pipe(catchError(() => of(null))),
    });
  }

  private mapToServiceHealth(name: string, health: any, metrics: any): ServiceHealth {
    const memUsedBytes = metrics?.memUsed?.measurements?.[0]?.value;
    const memMaxBytes = metrics?.memMax?.measurements?.[0]?.value;
    const uptimeSeconds = metrics?.uptime?.measurements?.[0]?.value;

    return {
      name,
      status: health?.status === 'UP' ? 'UP' : 'DOWN',
      uptime: uptimeSeconds ? this.formatUptime(uptimeSeconds) : undefined,
      memoryUsed: memUsedBytes ? this.formatBytes(memUsedBytes) : undefined,
      memoryMax: memMaxBytes ? this.formatBytes(memMaxBytes) : undefined,
      kafka: health?.components?.kafka?.status,
      db: health?.components?.db?.status ??
          health?.components?.['notifications-db']?.status,
      details: health?.components,
    };
  }

  private formatUptime(seconds: number): string {
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (d > 0) return `${d}d ${h}h`;
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
  }

  private formatBytes(bytes: number): string {
    const mb = bytes / (1024 * 1024);
    return mb > 1024
      ? `${(mb / 1024).toFixed(1)} GB`
      : `${Math.round(mb)} MB`;
  }

  private unknownService(name: string): ServiceHealth {
    return { name, status: 'UNKNOWN' };
  }
}
