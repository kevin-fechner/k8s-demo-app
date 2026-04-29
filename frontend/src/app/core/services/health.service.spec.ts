import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HealthService } from './health.service';

const PATHS = ['gateway', 'order-service', 'product-service', 'notification-service'];

function flushService(
  httpMock: HttpTestingController,
  path: string,
  healthBody: object,
  memUsed: number,
  memMax: number,
  uptime: number
) {
  httpMock.expectOne(`/api/${path}/actuator/health`).flush(healthBody);
  httpMock
    .expectOne(`/api/${path}/actuator/metrics/jvm.memory.used`)
    .flush({ measurements: [{ statistic: 'VALUE', value: memUsed }] });
  httpMock
    .expectOne(`/api/${path}/actuator/metrics/jvm.memory.max`)
    .flush({ measurements: [{ statistic: 'VALUE', value: memMax }] });
  httpMock
    .expectOne(`/api/${path}/actuator/metrics/process.uptime`)
    .flush({ measurements: [{ statistic: 'VALUE', value: uptime }] });
}

describe('HealthService', () => {
  let service: HealthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HealthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllHealth emits one result per service', () => {
    let result: ReturnType<HealthService['getAllHealth']> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (result = r));

    for (const path of PATHS) {
      flushService(httpMock, path, { status: 'UP' }, 256 * 1024 * 1024, 512 * 1024 * 1024, 3600);
    }

    expect(result!.length).toBe(4);
  });

  it('maps UP status and components correctly', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    const health = {
      status: 'UP',
      components: { kafka: { status: 'UP' }, db: { status: 'UP' } },
    };
    for (const path of PATHS) {
      flushService(httpMock, path, health, 268435456, 536870912, 3661);
    }

    const gw = results![0];
    expect(gw.name).toBe('API Gateway');
    expect(gw.status).toBe('UP');
    expect(gw.kafka).toBe('UP');
    expect(gw.db).toBe('UP');
    expect(gw.memoryUsed).toBe('256 MB');
    expect(gw.memoryMax).toBe('512 MB');
  });

  it('formats uptime in minutes when under one hour', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    for (const path of PATHS) {
      flushService(httpMock, path, { status: 'UP' }, 0, 0, 42 * 60);
    }

    expect(results![0].uptime).toBe('42m');
  });

  it('formats uptime in hours and minutes when under one day', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    for (const path of PATHS) {
      flushService(httpMock, path, { status: 'UP' }, 0, 0, 2 * 3600 + 30 * 60);
    }

    expect(results![0].uptime).toBe('2h 30m');
  });

  it('formats uptime in days and hours when one day or more', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    for (const path of PATHS) {
      flushService(httpMock, path, { status: 'UP' }, 0, 0, 3 * 86400 + 5 * 3600);
    }

    expect(results![0].uptime).toBe('3d 5h');
  });

  it('formats memory in GB when over 1 GB', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    const twoGb = 2 * 1024 * 1024 * 1024;
    for (const path of PATHS) {
      flushService(httpMock, path, { status: 'UP' }, twoGb, twoGb, 0);
    }

    expect(results![0].memoryUsed).toBe('2.0 GB');
  });

  it('falls back to DOWN status when health request returns a non-UP status', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    for (const path of PATHS) {
      flushService(httpMock, path, { status: 'DOWN' }, 0, 0, 0);
    }

    expect(results![0].status).toBe('DOWN');
  });

  it('returns DOWN when individual HTTP requests fail (inner catchError handles them)', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    // All gateway requests fail — fetchHealth catchError returns { status: 'DOWN' },
    // metric catchErrors return null, so the outer forkJoin still completes.
    httpMock.expectOne(`/api/gateway/actuator/health`).error(new ProgressEvent('error'));
    httpMock
      .expectOne(`/api/gateway/actuator/metrics/jvm.memory.used`)
      .error(new ProgressEvent('error'));
    httpMock
      .expectOne(`/api/gateway/actuator/metrics/jvm.memory.max`)
      .error(new ProgressEvent('error'));
    httpMock
      .expectOne(`/api/gateway/actuator/metrics/process.uptime`)
      .error(new ProgressEvent('error'));

    for (const path of PATHS.slice(1)) {
      flushService(httpMock, path, { status: 'UP' }, 0, 0, 0);
    }

    expect(results![0].status).toBe('DOWN');
    expect(results![0].name).toBe('API Gateway');
  });

  it('reads notifications-db component for the db field', () => {
    let results: ReturnType<typeof service.getAllHealth> extends import('rxjs').Observable<infer T>
      ? T
      : never;
    service.getAllHealth().subscribe((r) => (results = r));

    const health = { status: 'UP', components: { 'notifications-db': { status: 'UP' } } };
    for (const path of PATHS) {
      flushService(httpMock, path, health, 0, 0, 0);
    }

    expect(results![0].db).toBe('UP');
  });
});
