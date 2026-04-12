import {patchState, signalStore, withComputed, withHooks, withMethods, withState} from '@ngrx/signals';
import {computed, inject} from '@angular/core';
import {rxMethod} from '@ngrx/signals/rxjs-interop';
import {tapResponse} from '@ngrx/operators';
import {pipe, switchMap, tap} from 'rxjs';
import {OrderService} from '../services/order.service';
import {Order, OrderRequest, UpdateStatusRequest} from '../models/order.model';

interface OrderState {
  orders: Order[];
  loading: boolean;
  error: string | null;
  showForm: boolean;
}

const initialState: OrderState = {
  orders: [],
  loading: false,
  error: null,
  showForm: false
};

export const OrderStore = signalStore(
  {providedIn: 'root'},

  withState(initialState),

  withComputed(({orders}) => ({
    totalOrders: computed(() => orders().length),
    pendingOrders: computed(() =>
      orders().filter(o => o.status === 'PENDING').length
    ),
    totalRevenue: computed(() =>
      orders()
        .filter(o => o.status !== 'CANCELLED')
        .reduce((sum, o) => sum + o.totalAmount, 0)
    )
  })),

  withMethods((store, orderService = inject(OrderService)) => ({

    loadOrders: rxMethod<void>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap(() =>
          orderService.getAll().pipe(
            tapResponse({
              next: (orders) => patchState(store, {
                orders,
                loading: false
              }),
              error: () => patchState(store, {
                error: 'Failed to load orders. Is the API gateway running?',
                loading: false
              })
            })
          )
        )
      )
    ),

    createOrder: rxMethod<OrderRequest>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap((request) =>
          orderService.create(request).pipe(
            tapResponse({
              next: (order) => patchState(store, (state) => ({
                orders: [...state.orders, order],
                loading: false,
                showForm: false
              })),
              error: () => patchState(store, {
                error: 'Failed to create order.',
                loading: false
              })
            })
          )
        )
      )
    ),

    updateStatus: rxMethod<{ id: number; request: UpdateStatusRequest }>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap(({id, request}) =>
          orderService.updateStatus(id, request).pipe(
            tapResponse({
              next: (updated) => patchState(store, (state) => ({
                orders: state.orders.map(o =>
                  o.id === updated.id ? updated : o
                ),
                loading: false
              })),
              error: () => patchState(store, {
                error: 'Failed to update order status.',
                loading: false
              })
            })
          )
        )
      )
    ),

    deleteOrder: rxMethod<number>(
      pipe(
        tap(() => patchState(store, {loading: true, error: null})),
        switchMap((id) =>
          orderService.delete(id).pipe(
            tapResponse({
              next: () => patchState(store, (state) => ({
                orders: state.orders.filter(o => o.id !== id),
                loading: false
              })),
              error: () => patchState(store, {
                error: 'Failed to delete order.',
                loading: false
              })
            })
          )
        )
      )
    ),

    openForm(): void {
      patchState(store, {showForm: true});
    },

    closeForm(): void {
      patchState(store, {showForm: false});
    },

    clearError(): void {
      patchState(store, {error: null});
    }
  })),

  withHooks({
    onInit(store) {
      store.loadOrders();
    }
  })
);
