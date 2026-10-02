import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { toApiError } from '../../core/api-error';
import { CartService } from '../../core/cart.service';
import { Cart, CartItem } from '../../core/models';
import { PRODUCT_PLACEHOLDER, ProductService } from '../../services/product/product-service';
import { QuantityStepper } from '../quantity-stepper/quantity-stepper';

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, RouterLink, QuantityStepper],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartPage {
  protected readonly cartService = inject(CartService);
  protected readonly productService = inject(ProductService);

  protected readonly cart = this.cartService.cart;
  protected readonly loadError = signal<string | null>(null);

  /** productId currently being changed, so only that line shows a spinner / disables its controls. */
  protected readonly busyProductId = signal<number | null>(null);
  protected readonly lineErrors = signal<Record<number, string>>({});
  protected readonly clearing = signal(false);
  protected readonly confirmClear = signal(false);

  protected readonly currency = computed(() => this.cart()?.currency ?? 'USD');

  constructor() {
    // Always show fresh stock/prices when the page opens - they may have changed since the cart was loaded.
    this.cartService.refresh().subscribe({ error: (e) => this.loadError.set(toApiError(e).message) });
  }

  protected changeQuantity(item: CartItem, quantity: number): void {
    // If stock dropped below the current quantity (5 in cart, 2 left), "−" jumps straight to the highest
    // valid value instead of stepping to 4 and being rejected by the server.
    const target = quantity < item.quantity ? Math.min(quantity, item.maxQuantity) : quantity;
    if (target >= 1 && target !== item.quantity) {
      this.runForLine(item.productId, this.cartService.updateQuantity(item.productId, target));
    }
  }

  protected remove(item: CartItem): void {
    this.runForLine(item.productId, this.cartService.removeItem(item.productId));
  }

  protected clear(): void {
    this.clearing.set(true);
    this.cartService.clear().subscribe({
      next: () => {
        this.clearing.set(false);
        this.confirmClear.set(false);
      },
      error: (e) => {
        this.clearing.set(false);
        this.loadError.set(toApiError(e).message);
      },
    });
  }

  protected unavailableReason(item: CartItem): string {
    if (item.stockQuantity === 0 || item.maxQuantity === 0) return 'This item is no longer available. Remove it to continue.';
    return `Only ${item.stockQuantity} left - lower the quantity to continue.`;
  }

  protected useFallbackImage(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img.src !== PRODUCT_PLACEHOLDER) {
      img.src = PRODUCT_PLACEHOLDER;
    }
  }

  private runForLine(productId: number, request: Observable<Cart>): void {
    this.busyProductId.set(productId);
    this.setLineError(productId, null);
    request.subscribe({
      next: () => this.busyProductId.set(null),
      error: (e) => {
        this.busyProductId.set(null);
        this.setLineError(productId, toApiError(e).message);
        // The server's view (stock, availability) is the truth - re-sync after any rejection.
        this.cartService.refresh().subscribe({ error: () => undefined });
      },
    });
  }

  private setLineError(productId: number, message: string | null): void {
    this.lineErrors.update((errors) => {
      const { [productId]: _, ...rest } = errors;
      return message ? { ...rest, [productId]: message } : rest;
    });
  }
}
