import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProductList } from './product-list';

describe('ProductList', () => {
  let fixture: ComponentFixture<ProductList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductList);
    http = TestBed.inject(HttpTestingController);
  });

  it('renders products returned by the API', async () => {
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/products')).flush({
      content: [
        { productId: 1, productName: 'Mechanical Keyboard', productDescription: 'Clicky', price: 89.5, currency: 'USD',
          stockQuantity: 3, status: 'ACTIVE', categoryName: 'Electronics', brandName: 'Globex' },
      ],
      page: 0, size: 12, totalElements: 1, totalPages: 1,
    });
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Mechanical Keyboard');
  });

  it('shows an error state when the request fails', async () => {
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/products')).flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain("couldn't load products");
  });
});
