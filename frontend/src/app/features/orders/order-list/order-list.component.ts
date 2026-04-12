import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {CurrencyPipe} from '@angular/common';
import {OrderStore} from '../../../core/store/order.store';
import {Order, OrderStatus} from '../../../core/models/order.model';
import {OrderFormComponent} from '../order-form/order-form.component';

@Component({
  selector: 'app-order-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, OrderFormComponent],
  templateUrl: './order-list.component.html',
  styleUrl: 'order-list.component.scss'
})
export class OrderListComponent {
  readonly store = inject(OrderStore);
  readonly statuses: OrderStatus[] = [
    'PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'
  ];

  updateStatus(id: number, event: Event): void {
    const status = (event.target as HTMLSelectElement).value as OrderStatus;
    if (!status) return;
    this.store.updateStatus({id, request: {status}});
  }

  confirmDelete(order: Order): void {
    if (!confirm(`Delete order #${order.id}?`)) return;
    this.store.deleteOrder(order.id);
  }
}
