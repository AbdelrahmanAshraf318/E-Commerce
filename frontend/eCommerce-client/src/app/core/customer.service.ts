import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { API_URL } from './config';
import { ActivityEntry, ChangePasswordRequest, CustomerOrder, CustomerProfile, Page, UpdateProfileRequest } from './models';

/** /api/v1/users/me/** - the signed-in customer's own account. */
@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${API_URL}/users/me`;

  updateProfile(request: UpdateProfileRequest): Observable<CustomerProfile> {
    return this.http.patch<CustomerProfile>(this.baseUrl, request).pipe(tap((profile) => this.auth.user.set(profile)));
  }

  /** Re-fetches the profile afterwards so `hasPassword` flips for Google users setting a first password. */
  changePassword(request: ChangePasswordRequest): Observable<CustomerProfile> {
    return this.http
      .post<void>(`${this.baseUrl}/password`, request)
      .pipe(switchMap(() => this.auth.refreshProfile()));
  }

  deactivate(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/deactivate`, null);
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(this.baseUrl);
  }

  /** Newest first. */
  getOrders(): Observable<CustomerOrder[]> {
    return this.http.get<CustomerOrder[]>(`${this.baseUrl}/orders`);
  }

  /** The account's audit log, newest first. `page` is zero-based. */
  getActivity(page = 0, size = 20): Observable<Page<ActivityEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ActivityEntry>>(`${this.baseUrl}/activity`, { params });
  }
}
