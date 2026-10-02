import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiError, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../core/cart.service';
import { Product } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { PRODUCT_PLACEHOLDER, ProductService } from '../../services/product/product-service';
import { QuantityStepper } from '../quantity-stepper/quantity-stepper';

/** Mirrors CartItem.MAX_QUANTITY on the backend; the server still enforces it. */
const MAX_PER_LINE = 10;

@Component({
  selector: 'app-product-detail',
  imports: [CurrencyPipe, RouterLink, QuantityStepper],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductDetail {
  protected readonly productService = inject(ProductService);
  protected readonly cartService = inject(CartService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  /** Bound from the :productId route param (withComponentInputBinding). */
  readonly productId = input.required({ transform: numberAttribute });

  protected readonly product = signal<Product | null>(null);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly quantity = signal(1);
  protected readonly adding = signal(false);
  protected readonly cartError = signal<string | null>(null);

  protected readonly inCart = computed(() => {
    const p = this.product();
    return p ? this.cartService.quantityOf(p.productId) : 0;
  });

  protected readonly purchasable = computed(() => {
    const p = this.product();
    return !!p && p.status === 'ACTIVE' && p.stockQuantity > 0;
  });

  /** What can still be added: stock and the per-line limit both count what is already in the cart. */
  protected readonly maxAddable = computed(() => {
    const p = this.product();
    return p ? Math.max(0, Math.min(p.stockQuantity, MAX_PER_LINE) - this.inCart()) : 0;
  });

  constructor() {
    effect((onCleanup) => {
      const id = this.productId();
      this.product.set(null);
      this.error.set(null);
      this.quantity.set(1);
      this.cartError.set(null);
      const sub = this.productService.getProduct(id).subscribe({
        next: (product) => this.product.set(product),
        error: (e) => this.error.set(toApiError(e)),
      });
      onCleanup(() => sub.unsubscribe());
    });
  }

  protected setQuantity(value: number): void {
    this.quantity.set(Math.min(Math.max(1, value), Math.max(1, this.maxAddable())));
  }

  protected addToCart(): void {
    const p = this.product();
    if (!p) return;

    if (!this.auth.isAuthenticated()) {
      // Come back to this product after signing in.
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    this.adding.set(true);
    this.cartError.set(null);

    this.cartService.addItem(p.productId, this.quantity()).subscribe({
      next: () => {
        this.adding.set(false);
        this.toast.success('Added to cart', `${this.quantity()} × ${p.productName}`, { label: 'View cart', url: '/cart' });
        this.quantity.set(1);
      },
      error: (e) => {
        this.adding.set(false);
        this.cartError.set(toApiError(e).message);
        // Stock may have changed since the page loaded - refresh what we show.
        this.productService.getProduct(p.productId).subscribe({ next: (fresh) => this.product.set(fresh), error: () => undefined });
      },
    });
  }

  protected useFallbackImage(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img.src !== PRODUCT_PLACEHOLDER) {
      img.src = PRODUCT_PLACEHOLDER;
    }
  }
}
