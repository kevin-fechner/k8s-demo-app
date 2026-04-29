import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { ApiUrlService } from './api-url.service';

type GlobalWithProcess = typeof globalThis & { process?: { env: Record<string, string | undefined> } };

describe('ApiUrlService', () => {
  describe('in browser', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [{ provide: PLATFORM_ID, useValue: 'browser' }],
      });
    });

    it('returns empty string as baseUrl', () => {
      expect(TestBed.inject(ApiUrlService).baseUrl).toBe('');
    });
  });

  describe('on server', () => {
    const originalProcess = (globalThis as GlobalWithProcess).process;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [{ provide: PLATFORM_ID, useValue: 'server' }],
      });
    });

    afterEach(() => {
      (globalThis as GlobalWithProcess).process = originalProcess;
    });

    it('returns the API_URL env var when set', () => {
      (globalThis as GlobalWithProcess).process = { env: { API_URL: 'http://custom-api:8080' } };
      expect(TestBed.inject(ApiUrlService).baseUrl).toBe('http://custom-api:8080');
    });

    it('returns the default cluster URL when API_URL is not set', () => {
      (globalThis as GlobalWithProcess).process = { env: {} };
      expect(TestBed.inject(ApiUrlService).baseUrl).toBe(
        'http://api-gateway.demo-app.svc.cluster.local:8080'
      );
    });

    it('returns the default cluster URL when process is absent', () => {
      (globalThis as GlobalWithProcess).process = undefined;
      expect(TestBed.inject(ApiUrlService).baseUrl).toBe(
        'http://api-gateway.demo-app.svc.cluster.local:8080'
      );
    });
  });
});