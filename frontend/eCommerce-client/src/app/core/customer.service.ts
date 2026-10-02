import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { API_URL } from './config';
import { ChangePasswordRequest, CustomerProfile, UpdateProfileRequest } from './models';

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
}
