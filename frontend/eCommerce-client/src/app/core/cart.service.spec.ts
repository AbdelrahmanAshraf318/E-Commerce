import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { CartService } from './cart.service';
import { API_URL } from './config';
import { Cart, CustomerProfile } from './models';

const user = { userId: 'u-1', name: 'Test' } as CustomerProfile;
const cart: Cart = {
  items: [{ productId: 1, productName: 'Keyboard', unitPrice: 10, currency: 'USD', quantity: 2, lineTotal: 20,
            stockQuantity: 5, maxQuantity: 5, available: true }],
  totalQuantity: 2, subtotal: 20, currency: 'USD', hasUnavailableItems: false,
};

describe('CartService', () => {
  let service: CartService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CartService);
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the cart when a user signs in and forgets it when they sign out', () => {
    auth.user.set(user);
    TestBed.tick();
    http.expectOne(`${API_URL}/cart`).flush(cart);
    expect(service.itemCount()).toBe(2);
    expect(service.quantityOf(1)).toBe(2);

    auth.user.set(null);
    TestBed.tick();
    expect(service.cart()).toBeNull();
    expect(service.itemCount()).toBe(0);
  });

  it('does not re-fetch when the same user\'s profile object is replaced', () => {
    auth.user.set(user);
    TestBed.tick();
    http.expectOne(`${API_URL}/cart`).flush(cart);

    auth.user.set({ ...user, name: 'Renamed' });
    TestBed.tick();
    http.expectNone(`${API_URL}/cart`);
  });

  it('takes the server response as the new state after adding', () => {
    service.addItem(1, 2).subscribe();
    const req = http.expectOne(`${API_URL}/cart/items`);
    expect(req.request.body).toEqual({ productId: 1, quantity: 2 });
    req.flush(cart);
    expect(service.cart()).toEqual(cart);
  });
});
