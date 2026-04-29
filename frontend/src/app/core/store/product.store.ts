import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { inject, PLATFORM_ID } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { tapResponse } from '@ngrx/operators';
import { pipe, switchMap, tap } from 'rxjs';
import { ProductService } from '../services/product.service';
import { Product, ProductRequest } from '../models/product.model';
import { StockNumbers } from '../models/stock-numbers.model';
import { CursorPage } from '../models/pagination.model';
import { isPlatformBrowser } from '@angular/common';

interface ProductState {
  products: Product[];
  cursor: string | null;
  hasMore: boolean;
  selectedProduct: Product | null;
  loading: boolean;
  error: string | null;
  showForm: boolean;
  stockNumbers: StockNumbers;
}

const initialState: ProductState = {
  products: [],
  cursor: null,
  hasMore: false,
  selectedProduct: null,
  loading: false,
  error: null,
  showForm: false,
  stockNumbers: { total: 0, inStock: 0, outOfStock: 0 },
};

export const ProductStore = signalStore(
  { providedIn: 'root' },

  withState(initialState),

  withMethods((store, productService = inject(ProductService)) => ({
    loadStockNumbers: rxMethod<void>(
      pipe(
        switchMap(() =>
          productService.getStockNumbers().pipe(
            tapResponse({
              next: (stockNumbers) => patchState(store, { stockNumbers }),
              error: () =>
                patchState(store, {
                  error: 'Failed to load stock numbers. Is the API gateway running?',
                }),
            })
          )
        )
      )
    ),

    loadProducts: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(() =>
          productService.getPage().pipe(
            tapResponse({
              next: (page) =>
                patchState(store, {
                  products: page.data,
                  cursor: page.nextCursor ?? null,
                  hasMore: page.hasMore,
                  loading: false,
                }),
              error: () =>
                patchState(store, {
                  error: 'Failed to load products. Is the API gateway running?',
                  loading: false,
                }),
            })
          )
        )
      )
    ),

    loadMore: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(() =>
          productService.getPage(store.cursor()).pipe(
            tapResponse({
              next: (page) =>
                patchState(store, (state) => ({
                  products: [...state.products, ...page.data],
                  cursor: page.nextCursor ?? null,
                  hasMore: page.hasMore,
                  loading: false,
                })),
              error: () =>
                patchState(store, {
                  error: 'Failed to load more products.',
                  loading: false,
                }),
            })
          )
        )
      )
    ),

    setInitialProducts(page: CursorPage<Product>): void {
      patchState(store, {
        products: page.data,
        cursor: page.nextCursor ?? null,
        hasMore: page.hasMore,
        loading: false,
      });
    },

    setInitialStockNumbers(stock: StockNumbers): void {
      patchState(store, {
        stockNumbers: stock,
      });
    },
  })),

  withMethods((store, productService = inject(ProductService)) => ({
    createProduct: rxMethod<ProductRequest>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap((request) =>
          productService.create(request).pipe(
            tapResponse({
              next: (product) => {
                patchState(store, (state) => ({
                  products: [...state.products, product],
                  loading: false,
                  showForm: false,
                  selectedProduct: null,
                }));
                store.loadStockNumbers();
              },
              error: () =>
                patchState(store, {
                  error: 'Failed to create product.',
                  loading: false,
                }),
            })
          )
        )
      )
    ),

    updateProduct: rxMethod<{ id: number; request: ProductRequest }>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(({ id, request }) =>
          productService.update(id, request).pipe(
            tapResponse({
              next: (updated) => {
                const products = store.products().map((p) => (p.id === updated.id ? updated : p));
                patchState(store, {
                  products,
                  loading: false,
                  showForm: false,
                  selectedProduct: null,
                });
                store.loadStockNumbers();
              },
              error: () =>
                patchState(store, {
                  error: 'Failed to update product.',
                  loading: false,
                }),
            })
          )
        )
      )
    ),

    deleteProduct: rxMethod<number>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap((id) =>
          productService.delete(id).pipe(
            tapResponse({
              next: () => {
                const products = store.products().filter((p) => p.id !== id);
                patchState(store, { products, loading: false });
                store.loadStockNumbers();
              },
              error: () =>
                patchState(store, {
                  error: 'Failed to delete product.',
                  loading: false,
                }),
            })
          )
        )
      )
    ),

    openForm(product?: Product): void {
      patchState(store, {
        selectedProduct: product ?? null,
        showForm: true,
      });
    },

    closeForm(): void {
      patchState(store, {
        showForm: false,
        selectedProduct: null,
      });
    },

    clearError(): void {
      patchState(store, { error: null });
    },
  })),

  withHooks({
    onInit(store) {
      const platformId = inject(PLATFORM_ID);
      if (isPlatformBrowser(platformId)) {
        store.loadProducts();
        store.loadStockNumbers();
      }
    },
  })
);
