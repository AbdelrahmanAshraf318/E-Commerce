import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, Observable, of, switchMap, tap } from 'rxjs';
import { API_URL, GOOGLE_LOGIN_URL } from '../config';
import { AuthResponse, CustomerProfile, LoginRequest, RegisterRequest } from '../models';

const TOKEN_KEY = 'smartcart.accessToken';

export type LogoutReason = 'expired' | 'deactivated' | 'deleted';

/**
 * Single source of truth for "who is signed in".
 *
 * Trade-off: the JWT is kept in localStorage so a refresh does not sign the user out. That makes it readable
 * by any script on the page (XSS). The stronger option is an HttpOnly cookie set by the backend, which also
 * requires CSRF protection - worth doing before production.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly token = signal<string | null>(readValidToken());
  private expiryTimer?: ReturnType<typeof setTimeout>;

  readonly user = signal<CustomerProfile | null>(null);
  readonly isAuthenticated = computed(() => this.token() !== null);
  readonly needsProfileCompletion = computed(() => this.user()?.profileComplete === false);

  readonly googleLoginUrl = GOOGLE_LOGIN_URL;

  constructor() {
    this.scheduleExpiry(this.token());
  }

  /** Read by the interceptor; returns null once the token has expired. */
  accessToken(): string | null {
    const token = this.token();
    if (token && isExpired(token)) {
      this.clearSession();
      return null;
    }
    return token;
  }

  login(request: LoginRequest): Observable<CustomerProfile> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/login`, request).pipe(switchMap((r) => this.startSession(r.accessToken)));
  }

  register(request: RegisterRequest): Observable<CustomerProfile> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/register`, request).pipe(switchMap((r) => this.startSession(r.accessToken)));
  }

  reactivate(request: LoginRequest): Observable<CustomerProfile> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/reactivate`, request).pipe(switchMap((r) => this.startSession(r.accessToken)));
  }

  /** Called by /oauth2/callback with the token the backend put in the URL fragment. */
  completeOAuth2Login(accessToken: string): Observable<CustomerProfile> {
    return this.startSession(accessToken);
  }

  /** Runs once at startup: if a token survived the refresh, load the profile behind it. */
  restoreSession(): Observable<unknown> {
    if (!this.accessToken()) {
      return of(null);
    }
    return this.refreshProfile().pipe(catchError(() => of(this.clearSession())));
  }

  refreshProfile(): Observable<CustomerProfile> {
    return this.http.get<CustomerProfile>(`${API_URL}/users/me`).pipe(tap((profile) => this.user.set(profile)));
  }

  logout(reason?: LogoutReason): void {
    this.clearSession();
    this.router.navigate(['/login'], { queryParams: reason ? { reason } : {} });
  }

  /** The API rejected our token (expired, or account deactivated/deleted elsewhere): forget it without navigating. */
  dropRejectedSession(): void {
    this.clearSession();
  }

  private startSession(accessToken: string): Observable<CustomerProfile> {
    if (isExpired(accessToken)) {
      throw new Error('Received an expired token');
    }
    storage()?.setItem(TOKEN_KEY, accessToken);
    this.token.set(accessToken);
    this.scheduleExpiry(accessToken);
    return this.refreshProfile();
  }

  private clearSession(): null {
    storage()?.removeItem(TOKEN_KEY);
    clearTimeout(this.expiryTimer);
    this.token.set(null);
    this.user.set(null);
    return null;
  }

  private scheduleExpiry(token: string | null): void {
    clearTimeout(this.expiryTimer);
    const exp = token ? expiresAt(token) : null;
    if (exp) {
      // setTimeout overflows above ~24.8 days; tokens here live an hour.
      this.expiryTimer = setTimeout(() => this.logout('expired'), Math.min(exp - Date.now(), 2 ** 31 - 1));
    }
  }
}

function storage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null; // private mode / storage blocked
  }
}

function readValidToken(): string | null {
  const token = storage()?.getItem(TOKEN_KEY) ?? null;
  if (token && isExpired(token)) {
    storage()?.removeItem(TOKEN_KEY);
    return null;
  }
  return token;
}

/** Reads "exp" for scheduling only. The signature is verified by the backend, never trusted here. */
function expiresAt(token: string): number | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const exp = JSON.parse(atob(payload)).exp;
    return typeof exp === 'number' ? exp * 1000 : null;
  } catch {
    return null;
  }
}

function isExpired(token: string): boolean {
  const exp = expiresAt(token);
  return exp === null || exp <= Date.now();
}
