import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal, untracked } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { API_URL } from './config';
import { Cart } from './models';

/**
 * The signed-in customer's cart, mirrored from the server.
 *
 * The server's response is always taken as the new state (no client-side maths), so totals, stock and
 * availability can never drift from what the backend will accept at checkout.
 *
 * Depends on AuthService - never the other way round - and reacts to sign-in/sign-out through an effect,
 * so there is no circular dependency.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${API_URL}/cart`;

  readonly cart = signal<Cart | null>(null);
  readonly itemCount = computed(() => this.cart()?.totalQuantity ?? 0);

  // A computed string only notifies when the VALUE changes - not every time the profile object is replaced
  // (e.g. after editing the profile), which would re-fetch the cart for nothing.
  private readonly userId = computed(() => this.auth.user()?.userId ?? null);

  constructor() {
    effect(() => {
      const userId = this.userId();
      // Signed in -> load; signed out (logout, expired token, deleted account) -> forget the cart.
      untracked(() => (userId ? this.refresh().subscribe({ error: () => undefined }) : this.cart.set(null)));
    });
  }

  refresh(): Observable<Cart> {
    return this.http.get<Cart>(this.baseUrl).pipe(tap((cart) => this.cart.set(cart)));
  }

  addItem(productId: number, quantity: number): Observable<Cart> {
    return this.http.post<Cart>(`${this.baseUrl}/items`, { productId, quantity }).pipe(tap((cart) => this.cart.set(cart)));
  }

  updateQuantity(productId: number, quantity: number): Observable<Cart> {
    return this.http.patch<Cart>(`${this.baseUrl}/items/${productId}`, { quantity }).pipe(tap((cart) => this.cart.set(cart)));
  }

  removeItem(productId: number): Observable<Cart> {
    return this.http.delete<Cart>(`${this.baseUrl}/items/${productId}`).pipe(tap((cart) => this.cart.set(cart)));
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.baseUrl).pipe(tap(() => this.cart.update((c) => (c ? emptyLike(c) : c))));
  }

  /** Quantity of a product already in the cart (0 if none) - lets the product page cap what can still be added. */
  quantityOf(productId: number): number {
    return this.cart()?.items.find((i) => i.productId === productId)?.quantity ?? 0;
  }
}

function emptyLike(cart: Cart): Cart {
  return { ...cart, items: [], totalQuantity: 0, subtotal: 0, currency: null, hasUnavailableItems: false };
}
