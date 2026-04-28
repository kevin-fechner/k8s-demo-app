import {ApplicationConfig, inject, PLATFORM_ID, provideZonelessChangeDetection} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import {isPlatformBrowser} from '@angular/common';
import {IS_BROWSER} from './tokens/platform.token';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    provideClientHydration(),
    {
      provide: IS_BROWSER,
      useFactory: () => isPlatformBrowser(inject(PLATFORM_ID))
    }
  ],
};
