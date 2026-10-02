import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_URL } from '../config';
import { currentRouteRequiresAuth } from './auth.guards';
import { AuthService } from './auth.service';

/**
 * Adds the bearer token to calls to OUR API only - never leak it to third-party URLs.
 * A 401 on an authenticated call means the token is no longer accepted (expired, account deactivated or
 * deleted elsewhere): the session is dropped, and the user is sent to /login only if the current page
 * requires sign-in. A stale token must not kick anyone off public pages like the catalogue.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = request.url.startsWith(API_URL) ? auth.accessToken() : null;
  const authorized = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (token && error instanceof HttpErrorResponse && error.status === 401) {
        auth.dropRejectedSession();
        if (currentRouteRequiresAuth(router)) {
          router.navigate(['/login'], { queryParams: { reason: 'expired', returnUrl: router.url } });
        }
      }
      return throwError(() => error);
    }),
  );
};
