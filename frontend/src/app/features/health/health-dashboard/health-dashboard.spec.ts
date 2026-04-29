import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HealthDashboardComponent } from './health-dashboard';
import { HealthService, ServiceHealth } from '../../../core/services/health.service';
import { of } from 'rxjs';

describe('HealthDashboardComponent', () => {
  let component: HealthDashboardComponent;
  let fixture: ComponentFixture<HealthDashboardComponent>;
  let getAllHealthSpy: ReturnType<typeof vi.fn>;

  const allUp: ServiceHealth[] = [
    {
      name: 'API Gateway',
      status: 'UP',
      uptime: '2h 5m',
      memoryUsed: '256 MB',
      memoryMax: '512 MB',
    },
    { name: 'Order Service', status: 'UP' },
    { name: 'Product Service', status: 'UP' },
    { name: 'Notification Service', status: 'UP' },
  ];

  beforeEach(async () => {
    getAllHealthSpy = vi.fn().mockReturnValue(of(allUp));

    await TestBed.configureTestingModule({
      imports: [HealthDashboardComponent],
      providers: [{ provide: HealthService, useValue: { getAllHealth: getAllHealthSpy } }],
    }).compileComponents();

    fixture = TestBed.createComponent(HealthDashboardComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => fixture.destroy());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('populates services signal after detectChanges', () => {
    fixture.detectChanges();
    expect(getAllHealthSpy).toHaveBeenCalled();
    expect(component.services()).toEqual(allUp);
  });

  it('sets loading to false after receiving data', () => {
    fixture.detectChanges();
    expect(component.loading()).toBe(false);
  });

  it('updates lastUpdated after receiving data', () => {
    fixture.detectChanges();
    expect(component.lastUpdated()).toBeInstanceOf(Date);
  });

  it('allUp returns true when every service is UP', () => {
    fixture.detectChanges();
    expect(component.allUp()).toBe(true);
  });

  it('allUp returns false when any service is not UP', () => {
    getAllHealthSpy.mockReturnValue(
      of([
        { name: 'API Gateway', status: 'UP' },
        { name: 'Order Service', status: 'DOWN' },
      ] satisfies ServiceHealth[])
    );
    fixture.detectChanges();
    expect(component.allUp()).toBe(false);
  });

  it('allUp returns false when a service is UNKNOWN', () => {
    getAllHealthSpy.mockReturnValue(
      of([{ name: 'API Gateway', status: 'UNKNOWN' }] satisfies ServiceHealth[])
    );
    fixture.detectChanges();
    expect(component.allUp()).toBe(false);
  });

  it('unsubscribes on destroy without throwing', () => {
    fixture.detectChanges();
    expect(() => fixture.destroy()).not.toThrow();
  });
});
