import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  {
    path: 'products',
    title: 'Products · SmartCart',
    loadComponent: () => import('./components/product-list/product-list').then((m) => m.ProductList),
  },
  {
    path: 'products/:productId',
    title: 'Product · SmartCart',
    loadComponent: () => import('./components/product-detail/product-detail').then((m) => m.ProductDetail),
  },
  {
    path: 'login',
    title: 'Sign in · SmartCart',
    canActivate: [guestGuard],
    loadComponent: () => import('./components/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Create account · SmartCart',
    canActivate: [guestGuard],
    loadComponent: () => import('./components/register/register').then((m) => m.Register),
  },
  {
    // Spring Security redirects here after Google sign-in (app.security.oauth2.authorized-redirect-uri).
    path: 'oauth2/callback',
    loadComponent: () => import('./components/oauth2-callback/oauth2-callback').then((m) => m.OAuth2Callback),
  },
  {
    path: 'cart',
    title: 'Your cart · SmartCart',
    canActivate: [authGuard],
    loadComponent: () => import('./components/cart/cart').then((m) => m.CartPage),
  },
  {
    path: 'account',
    title: 'My account · SmartCart',
    canActivate: [authGuard],
    loadComponent: () => import('./components/account/account').then((m) => m.Account),
  },
  {
    path: 'account/complete-profile',
    title: 'Complete your profile · SmartCart',
    canActivate: [authGuard],
    loadComponent: () => import('./components/complete-profile/complete-profile').then((m) => m.CompleteProfile),
  },
  { path: '**', redirectTo: 'products' },
];
