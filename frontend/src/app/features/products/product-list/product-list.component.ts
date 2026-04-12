import {Component, OnInit, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { ProductStore } from '../../../core/store/product.store';
import { Product } from '../../../core/models/product.model';
import { ProductFormComponent } from '../product-form/product-form.component';

@Component({
  selector: 'app-product-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, CurrencyPipe, ProductFormComponent],
  templateUrl: 'product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent {
  readonly store = inject(ProductStore);

  confirmDelete(product: Product): void {
    if (!confirm(`Delete "${product.name}"?`)) return;
    this.store.deleteProduct(product.id);
  }
}
