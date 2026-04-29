import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class ApiUrlService {
  private readonly platformId = inject(PLATFORM_ID);

  get baseUrl(): string {
    if (isPlatformBrowser(this.platformId)) {
      return '';
    }
    interface NodeProcess {
      env: Record<string, string | undefined>;
    }
    const nodeProcess = (globalThis as typeof globalThis & { process?: NodeProcess }).process;
    return nodeProcess?.env['API_URL'] ?? 'http://api-gateway.demo-app.svc.cluster.local:8080';
  }
}
