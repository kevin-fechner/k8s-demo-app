import {Routes} from '@angular/router';
import {ProductListComponent} from './features/products/product-list/product-list.component';
import {productsResolver} from './core/resolvers/products.resolver';

export const routes: Routes = [
  {path: '', redirectTo: 'products', pathMatch: 'full'},
  {
    path: 'products',
    component: ProductListComponent,
    resolve: {
      productsPage: productsResolver
    }
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./features/orders/order-list/order-list.component')
        .then(m => m.OrderListComponent)
  },
  {
    path: 'health',
    loadComponent: () =>
      import('./features/health/health-dashboard/health-dashboard')
        .then(m => m.HealthDashboardComponent)
  },
  {path: '**', redirectTo: 'products'}
];
