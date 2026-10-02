import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError, applyFieldErrors, controlError, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { CustomerService } from '../../core/customer.service';
import { guessRegion, REGIONS } from '../../core/regions';
import { pastDate, todayIso } from '../../core/validators';

/**
 * Google gives us name + email only. Local sign-up requires date of birth, country and phone,
 * so Google users are asked for them once, here.
 */
@Component({
  selector: 'app-complete-profile',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './complete-profile.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompleteProfile {
  protected readonly auth = inject(AuthService);
  private readonly customerService = inject(CustomerService);
  private readonly router = inject(Router);

  protected readonly regions = REGIONS;
  protected readonly today = todayIso();
  protected readonly controlError = controlError;
  protected readonly submitting = signal(false);
  protected readonly apiError = signal<ApiError | null>(null);

  private readonly user = this.auth.user();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: [this.user?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    dateOfBirth: [this.user?.dateOfBirth ?? '', [Validators.required, pastDate()]],
    region: [this.user?.region ?? guessRegion(), Validators.required],
    phoneNumber: [this.user?.phoneNumber ?? '', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.apiError.set(null);

    this.customerService.updateProfile(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/products'),
      error: (e) => {
        const apiError = toApiError(e);
        if (apiError.code === 'PHONE_ALREADY_EXISTS') apiError.fieldErrors['phoneNumber'] = apiError.message;
        applyFieldErrors(this.form, apiError);
        this.apiError.set(apiError);
        this.submitting.set(false);
      },
    });
  }
}
