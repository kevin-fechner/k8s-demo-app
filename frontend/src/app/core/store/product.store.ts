import {patchState, signalStore, withComputed, withHooks, withMethods, withState} from '@ngrx/signals';
import {computed, inject} from '@angular/core';
import {rxMethod} from '@ngrx/signals/rxjs-interop';
import {tapResponse} from '@ngrx/operators';
import {pipe, switchMap, tap} from 'rxjs';
import {ProductService} from '../services/product.service';
import {Product, ProductRequest} from '../models/product.model';

interface ProductState {
  products: Product[];
  selectedProduct: Product | null;
  loading: boolean;
  error: string | null;
  showForm: boolean;
}

const initialState: ProductState = {
  products: [],
  selectedProduct: null,
  loading: false,
  error: null,
  showForm: false
};

export const ProductStore = signalStore(
  {providedIn: 'root'},

  withState(initialState),

  withComputed(({products}) => ({
    totalProducts: computed(() => products().length),
    inStockProducts: computed(() =>
      products().filter(p => p.stock > 0).length
    ),
    outOfStockProducts: computed(() =>
      products().filter(p => p.stock === 0).length
    )
  })),

  withMethods((store, productService = inject(ProductService)) => ({

    loadProducts: rxMethod<void>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap(() =>
          productService.getAll().pipe(
            tapResponse({
              next: (products) => patchState(store, {
                products,
                loading: false
              }),
              error: () => patchState(store, {
                error: 'Failed to load products. Is the API gateway running?',
                loading: false
              })
            })
          )
        )
      )
    ),

    createProduct: rxMethod<ProductRequest>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap((request) =>
          productService.create(request).pipe(
            tapResponse({
              next: (product) => patchState(store, (state) => ({
                products: [...state.products, product],
                loading: false,
                showForm: false,
                selectedProduct: null
              })),
              error: () => patchState(store, {
                error: 'Failed to create product.',
                loading: false
              })
            })
          )
        )
      )
    ),

    updateProduct: rxMethod<{ id: number; request: ProductRequest }>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap(({id, request}) =>
          productService.update(id, request).pipe(
            tapResponse({
              next: (updated) => patchState(store, (state) => ({
                products: state.products.map(p =>
                  p.id === updated.id ? updated : p
                ),
                loading: false,
                showForm: false,
                selectedProduct: null
              })),
              error: () => patchState(store, {
                error: 'Failed to update product.',
                loading: false
              })
            })
          )
        )
      )
    ),

    deleteProduct: rxMethod<number>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap((id) =>
          productService.delete(id).pipe(
            tapResponse({
              next: () => patchState(store, (state) => ({
                products: state.products.filter(p => p.id !== id),
                loading: false
              })),
              error: () => patchState(store, {
                error: 'Failed to delete product.',
                loading: false
              })
            })
          )
        )
      )
    ),

    openForm(product?: Product): void {
      patchState(store, {
        selectedProduct: product ?? null,
        showForm: true
      });
    },

    closeForm(): void {
      patchState(store, {
        showForm: false,
        selectedProduct: null
      });
    },

    clearError(): void {
      patchState(store, {error: null});
    }
  })),

  withHooks({
    onInit(store) {
      store.loadProducts();
    }
  })
);
