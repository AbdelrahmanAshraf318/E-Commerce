import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError, applyFieldErrors, controlError, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { GoogleButton } from '../google-button/google-button';
import { guessRegion, REGIONS } from '../../core/regions';
import { matchFields, PASSWORD_HINT, passwordStrength, pastDate, todayIso } from '../../core/validators';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, GoogleButton],
  templateUrl: './register.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Register {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly regions = REGIONS;
  protected readonly today = todayIso();
  protected readonly passwordHint = PASSWORD_HINT;
  protected readonly controlError = controlError;

  protected readonly submitting = signal(false);
  protected readonly apiError = signal<ApiError | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      name: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, passwordStrength()]],
      confirmPassword: ['', Validators.required],
      dateOfBirth: ['', [Validators.required, pastDate()]],
      region: [guessRegion(), Validators.required],
      phoneNumber: ['', Validators.required],
    },
    { validators: matchFields('password', 'confirmPassword') },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.apiError.set(null);

    const { confirmPassword: _, ...request } = this.form.getRawValue();
    this.auth.register(request).subscribe({
      next: () => this.router.navigateByUrl('/products'),
      error: (e) => {
        const apiError = toApiError(e);
        // Put "email already exists" next to the email field instead of only at the bottom.
        if (apiError.code === 'EMAIL_ALREADY_EXISTS') apiError.fieldErrors['email'] = apiError.message;
        if (apiError.code === 'PHONE_ALREADY_EXISTS') apiError.fieldErrors['phoneNumber'] = apiError.message;
        applyFieldErrors(this.form, apiError);
        this.apiError.set(apiError);
        this.submitting.set(false);
      },
    });
  }
}
