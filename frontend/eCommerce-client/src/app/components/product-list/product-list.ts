import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, of, Subject, switchMap } from 'rxjs';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../core/cart.service';
import { Page, Product } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { PRODUCT_PLACEHOLDER, ProductService } from '../../services/product/product-service';

const PAGE_SIZE = 12;

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductList implements OnInit {
  protected readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // Signals, not plain fields: this app is zoneless, so mutating a field inside subscribe()
  // would never re-render the template.
  protected readonly page = signal<Page<Product> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly search = signal('');
  protected readonly addingId = signal<number | null>(null);

  private readonly requests = new Subject<number>();
  private readonly searchInput = new Subject<string>();

  ngOnInit(): void {
    this.requests
      .pipe(
        // switchMap cancels the in-flight request, so a slow old search never overwrites a newer one.
        switchMap((pageNumber) =>
          this.productService.getProducts(pageNumber, PAGE_SIZE, this.search()).pipe(
            // Handle the error per request; an error reaching subscribe() would end the stream for good.
            catchError(() => of(null)),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        this.error.set(result === null);
        if (result) this.page.set(result);
        this.loading.set(false);
      });

    this.searchInput
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(0));

    this.load(0);
  }

  protected onSearch(term: string): void {
    this.search.set(term.trim());
    this.searchInput.next(term);
  }

  protected clearSearch(): void {
    this.onSearch('');
  }

  protected goToPage(pageNumber: number): void {
    this.load(pageNumber);
    document.getElementById('catalogue')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  protected retry(): void {
    this.load(this.page()?.page ?? 0);
  }

  protected isPurchasable(product: Product): boolean {
    return product.status === 'ACTIVE' && product.stockQuantity > 0;
  }

  protected quickAdd(product: Product): void {
    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: `/products/${product.productId}` } });
      return;
    }
    this.addingId.set(product.productId);
    this.cartService.addItem(product.productId, 1).subscribe({
      next: () => {
        this.addingId.set(null);
        this.toast.success('Added to cart', product.productName, { label: 'View cart', url: '/cart' });
      },
      error: (e) => {
        this.addingId.set(null);
        this.toast.error("Couldn't add to cart", toApiError(e).message);
      },
    });
  }

  protected useFallbackImage(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img.src !== PRODUCT_PLACEHOLDER) {
      img.src = PRODUCT_PLACEHOLDER;
    }
  }

  private load(pageNumber: number): void {
    this.loading.set(true);
    this.requests.next(pageNumber);
  }
}
