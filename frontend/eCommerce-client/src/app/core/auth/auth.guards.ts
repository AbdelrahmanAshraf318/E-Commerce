import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Signed-in users only; others go to /login and come back afterwards. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isAuthenticated()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Login / register make no sense when already signed in. */
export const guestGuard: CanActivateFn = () => {
  return inject(AuthService).isAuthenticated() ? inject(Router).createUrlTree(['/products']) : true;
};

/** True if the page currently shown is protected by {@link authGuard}. */
export function currentRouteRequiresAuth(router: Router): boolean {
  let route: ActivatedRouteSnapshot | null = router.routerState.snapshot.root;
  while (route) {
    if (route.routeConfig?.canActivate?.includes(authGuard)) return true;
    route = route.firstChild;
  }
  return false;
}

/** Guards only improve UX - the backend enforces every rule again. */
