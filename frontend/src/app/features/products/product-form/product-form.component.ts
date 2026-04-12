import {ChangeDetectionStrategy, Component, inject, input, OnInit, output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ProductStore} from '../../../core/store/product.store';
import {Product, ProductRequest} from '../../../core/models/product.model';

@Component({
  selector: 'app-product-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: 'product-form.component.html',
  styleUrl: 'product-form.component.scss'
})
export class ProductFormComponent implements OnInit {
  product = input<Product | null>(null);
  saved = output<void>();
  cancelled = output<void>();

  readonly store = inject(ProductStore);
  private readonly fb = inject(FormBuilder);

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    price: [0, [Validators.required, Validators.min(0.01)]],
    stock: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    const p = this.product();
    if (p) {
      this.form.patchValue({
        name: p.name,
        description: p.description,
        price: p.price,
        stock: p.stock
      });
    }
  }

  save(): void {
    if (this.form.invalid) return;
    const value = this.form.value as ProductRequest;
    const p = this.product();
    if (p) {
      this.store.updateProduct({id: p.id, request: value});
    } else {
      this.store.createProduct(value);
    }
  }
}
