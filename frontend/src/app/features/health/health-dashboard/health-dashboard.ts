import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {interval, startWith, Subscription, switchMap} from 'rxjs';
import {HealthService, ServiceHealth} from '../../../core/services/health.service';

@Component({
  selector: 'app-health-dashboard',
  imports: [CommonModule],
  templateUrl: './health-dashboard.html',
  styleUrls: ['./health-dashboard.scss']
})
export class HealthDashboardComponent implements OnInit, OnDestroy {
  private healthService = inject(HealthService);

  services = signal<ServiceHealth[]>([]);
  lastUpdated = signal<Date>(new Date());
  loading = signal(true);

  private subscription?: Subscription;

  ngOnInit() {
    this.subscription = interval(10000).pipe(
      startWith(0),
      switchMap(() => this.healthService.getAllHealth())
    ).subscribe(services => {
      this.services.set(services);
      this.lastUpdated.set(new Date());
      this.loading.set(false);
    });
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe();
  }

  allUp(): boolean {
    return this.services().every(s => s.status === 'UP');
  }
}
