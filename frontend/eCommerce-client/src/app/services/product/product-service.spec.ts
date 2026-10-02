import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../core/config';
import { ProductService } from './product-service';

describe('ProductService', () => {
  let service: ProductService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProductService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests a page of products with a trimmed search term', () => {
    service.getProducts(2, 12, '  keyboard ').subscribe();
    const req = http.expectOne((r) => r.url === `${API_URL}/products`);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('search')).toBe('keyboard');
    req.flush({ content: [], page: 2, size: 12, totalElements: 0, totalPages: 0 });
  });
});
