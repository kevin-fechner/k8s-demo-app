import {ChangeDetectionStrategy, Component, inject, OnInit, output} from '@angular/core';
import {CurrencyPipe} from '@angular/common';
import {FormArray, FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {OrderStore} from '../../../core/store/order.store';
import {ProductStore} from '../../../core/store/product.store';

@Component({
  selector: 'app-order-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, ReactiveFormsModule],
  templateUrl: 'order-form.component.html',
  styleUrl: 'order-form.component.scss'
})
export class OrderFormComponent implements OnInit {
  saved = output<void>();
  cancelled = output<void>();

  readonly orderStore = inject(OrderStore);
  readonly productStore = inject(ProductStore);
  private readonly fb = inject(FormBuilder);

  form = this.fb.group({
    customerName: ['', [Validators.required, Validators.maxLength(255)]],
    customerEmail: ['', [Validators.required, Validators.email]],
    notes: [''],
    items: this.fb.array([this.createItemGroup()])
  });

  get itemsArray(): FormArray {
    return this.form.get('items') as FormArray;
  }

  ngOnInit(): void {
    // Products already loaded by ProductStore's onInit hook
    // but load anyway if store is empty
    if (this.productStore.products().length === 0) {
      this.productStore.loadProducts();
    }
  }

  createItemGroup() {
    return this.fb.group({
      productId: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]]
    });
  }

  addItem(): void {
    this.itemsArray.push(this.createItemGroup());
  }

  removeItem(i: number): void {
    this.itemsArray.removeAt(i);
  }

  save(): void {
    if (this.form.invalid) return;
    const {customerName, customerEmail, notes, items} = this.form.value;
    this.orderStore.createOrder({
      customerName: customerName!,
      customerEmail: customerEmail!,
      notes: notes || undefined,
      items: items!.map(i => ({
        productId: Number(i.productId),
        quantity: Number(i.quantity)
      }))
    });
  }
}
