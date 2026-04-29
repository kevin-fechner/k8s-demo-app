import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: '',
    renderMode: RenderMode.Server,
  },
  {
    path: 'products', // products list
    renderMode: RenderMode.Server, // SSR - fresh stock data
  },
  {
    path: 'orders', // orders list
    renderMode: RenderMode.Client, // CSR - user specific
  },
  {
    path: 'health', // health dashboard
    renderMode: RenderMode.Client, // CSR - real-time data
  },
  {
    path: '**', // fallback
    renderMode: RenderMode.Server,
  },
];
