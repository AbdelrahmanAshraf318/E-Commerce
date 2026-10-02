import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../core/config';
import { Page, Product } from '../../core/models';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_URL}/products`;

  getProducts(page = 0, size = 12, search = ''): Observable<Page<Product>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<Page<Product>>(this.baseUrl, { params });
  }

  getProduct(productId: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${productId}`);
  }

  /** Served from the PRODUCT_IMAGE table; 404s when a product has no image (the UI falls back to a placeholder). */
  imageUrl(productId: number): string {
    return `${this.baseUrl}/${productId}/image`;
  }
}

/** Inline SVG so the fallback image never 404s itself. */
export const PRODUCT_PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#eef1f4"/>' +
      '<path d="M170 120h60v60h-60z" fill="none" stroke="#adb5bd" stroke-width="6" stroke-linejoin="round"/>' +
      '<circle cx="188" cy="140" r="7" fill="#adb5bd"/><path d="M172 176l20-22 14 14 10-10 14 18" fill="none" stroke="#adb5bd" stroke-width="6" stroke-linejoin="round"/></svg>',
  );
