import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ApiError, applyFieldErrors, controlError, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { GoogleButton } from '../google-button/google-button';
import { CustomerProfile } from '../../core/models';

/** Messages for ?reason= (set by logout) and ?error= (set by the Google callback). */
const NOTICES: Record<string, { type: 'info' | 'warning' | 'danger'; text: string }> = {
  expired: { type: 'info', text: 'Your session ended. Please sign in again.' },
  deactivated: { type: 'info', text: 'Your account is deactivated. Sign in and choose “Reactivate” whenever you want to come back.' },
  deleted: { type: 'info', text: 'Your account and its data were deleted.' },
  OAUTH2_FAILED: { type: 'danger', text: 'Google sign-in was cancelled or failed. Please try again.' },
  OAUTH2_EMAIL_NOT_VERIFIED: { type: 'danger', text: 'Your Google email address is not verified, so we cannot sign you in with it.' },
  ACCOUNT_LOCKED: { type: 'danger', text: 'This account is locked. Please contact support.' },
};

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, GoogleButton],
  templateUrl: './login.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly returnUrl = input<string>();
  readonly reason = input<string>();
  readonly error = input<string>();

  protected readonly notice = computed(() => NOTICES[this.error() ?? this.reason() ?? '']);
  protected readonly submitting = signal(false);
  protected readonly apiError = signal<ApiError | null>(null);
  protected readonly controlError = controlError;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected submit(): void {
    this.send((credentials) => this.auth.login(credentials));
  }

  /** Shown after the backend answers ACCOUNT_DEACTIVATED - which it only does when the password was correct. */
  protected reactivate(): void {
    this.send((credentials) => this.auth.reactivate(credentials));
  }

  private send(call: (credentials: { email: string; password: string }) => Observable<CustomerProfile>): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.apiError.set(null);

    call(this.form.getRawValue()).subscribe({
      next: (profile) => {
        const target = profile.profileComplete ? safeReturnUrl(this.returnUrl()) : '/account/complete-profile';
        this.router.navigateByUrl(target);
      },
      error: (e) => {
        const apiError = toApiError(e);
        applyFieldErrors(this.form, apiError);
        this.apiError.set(apiError);
        this.submitting.set(false);
      },
    });
  }
}

/** Only allow in-app paths, so ?returnUrl=https://evil.example cannot be used as an open redirect. */
export function safeReturnUrl(url: string | undefined): string {
  return url && url.startsWith('/') && !url.startsWith('//') ? url : '/products';
}
