import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {CommonModule, CurrencyPipe} from '@angular/common';
import {ProductStore} from '../../../core/store/product.store';
import {Product} from '../../../core/models/product.model';
import {ProductFormComponent} from '../product-form/product-form.component';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-product-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, CurrencyPipe, ProductFormComponent],
  templateUrl: 'product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit {
  readonly store = inject(ProductStore);
  private readonly route = inject(ActivatedRoute);

  ngOnInit() {
    // Use resolved data on first load (works with SSR)
    const resolvedPage = this.route.snapshot.data['productsPage'];
    if (resolvedPage?.data?.length > 0) {
      this.store.setInitialProducts(resolvedPage);
    } else {
      // Fallback to store load (client-side)
      this.store.loadProducts();
    }
    this.store.loadStockNumbers();
  }

  confirmDelete(product: Product): void {
    if (!confirm(`Delete "${product.name}"?`)) return;
    this.store.deleteProduct(product.id);
  }


}
