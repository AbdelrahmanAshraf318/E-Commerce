import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, applyFieldErrors, controlError, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { CustomerService } from '../../core/customer.service';
import { REGIONS } from '../../core/regions';
import { matchFields, PASSWORD_HINT, passwordStrength, pastDate, todayIso } from '../../core/validators';

type DangerAction = 'deactivate' | 'delete';

/** Covers every /api/v1/users/me endpoint: GET, PATCH, POST /password, PATCH /deactivate, DELETE. */
@Component({
  selector: 'app-account',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './account.html',
  styleUrl: './account.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Account {
  protected readonly auth = inject(AuthService);
  private readonly customerService = inject(CustomerService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly regions = REGIONS;
  protected readonly today = todayIso();
  protected readonly passwordHint = PASSWORD_HINT;
  protected readonly controlError = controlError;
  protected readonly user = this.auth.user;
  protected readonly regionName = computed(
    () => REGIONS.find((r) => r.code === this.user()?.region)?.name ?? this.user()?.region,
  );

  // ---------- Profile ----------
  protected readonly editing = signal(false);
  protected readonly profileSaving = signal(false);
  protected readonly profileError = signal<ApiError | null>(null);
  protected readonly profileSaved = signal(false);

  protected readonly profileForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    dateOfBirth: ['', pastDate()],
    region: [''],
    phoneNumber: [''],
  });

  // ---------- Password ----------
  protected readonly passwordSaving = signal(false);
  protected readonly passwordError = signal<ApiError | null>(null);
  protected readonly passwordSaved = signal(false);

  protected readonly passwordForm = this.fb.group(
    {
      currentPassword: [''],
      newPassword: ['', [Validators.required, passwordStrength()]],
      confirmedNewPassword: ['', Validators.required],
    },
    { validators: matchFields('newPassword', 'confirmedNewPassword') },
  );

  // ---------- Danger zone ----------
  protected readonly confirming = signal<DangerAction | null>(null);
  protected readonly dangerBusy = signal(false);
  protected readonly dangerError = signal<ApiError | null>(null);
  protected readonly deleteConfirmation = signal('');

  protected readonly initials = computed(() => {
    const parts = (this.user()?.name ?? '').trim().split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase() || '?';
  });

  constructor() {
    // Re-fetch so the page never shows a stale profile (e.g. changed in another tab).
    this.auth.refreshProfile().subscribe({ error: () => undefined });
  }

  protected startEditing(): void {
    const u = this.user();
    this.profileForm.reset({
      name: u?.name ?? '',
      dateOfBirth: u?.dateOfBirth ?? '',
      region: u?.region ?? '',
      phoneNumber: u?.phoneNumber ?? '',
    });
    this.profileError.set(null);
    this.profileSaved.set(false);
    this.editing.set(true);
  }

  protected saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    const value = this.profileForm.getRawValue();
    this.profileSaving.set(true);
    this.profileError.set(null);

    // Empty strings mean "leave unchanged" on the backend, so send null instead.
    this.customerService
      .updateProfile({
        name: value.name,
        dateOfBirth: value.dateOfBirth || null,
        region: value.region || null,
        phoneNumber: value.phoneNumber || null,
      })
      .subscribe({
        next: () => {
          this.profileSaving.set(false);
          this.editing.set(false);
          this.profileSaved.set(true);
        },
        error: (e) => {
          const apiError = toApiError(e);
          if (apiError.code === 'PHONE_ALREADY_EXISTS') apiError.fieldErrors['phoneNumber'] = apiError.message;
          applyFieldErrors(this.profileForm, apiError);
          this.profileError.set(apiError);
          this.profileSaving.set(false);
        },
      });
  }

  protected savePassword(): void {
    const needsCurrent = this.user()?.hasPassword ?? true;
    const current = this.passwordForm.controls.currentPassword;
    if (needsCurrent && !current.value) {
      current.setErrors({ required: true });
    }
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.passwordSaving.set(true);
    this.passwordError.set(null);
    this.passwordSaved.set(false);

    const value = this.passwordForm.getRawValue();
    this.customerService
      .changePassword({ ...value, currentPassword: needsCurrent ? value.currentPassword : null })
      .subscribe({
        next: () => {
          this.passwordForm.reset();
          this.passwordSaving.set(false);
          this.passwordSaved.set(true);
        },
        error: (e) => {
          const apiError = toApiError(e);
          if (apiError.code === 'CURRENT_PASSWORD_INCORRECT' || apiError.code === 'CURRENT_PASSWORD_REQUIRED') {
            apiError.fieldErrors['currentPassword'] = apiError.message;
          }
          applyFieldErrors(this.passwordForm, apiError);
          this.passwordError.set(apiError);
          this.passwordSaving.set(false);
        },
      });
  }

  protected confirm(action: DangerAction | null): void {
    this.confirming.set(action);
    this.deleteConfirmation.set('');
    this.dangerError.set(null);
  }

  protected runDangerAction(): void {
    const action = this.confirming();
    if (!action) return;
    this.dangerBusy.set(true);

    const request = action === 'delete' ? this.customerService.deleteAccount() : this.customerService.deactivate();
    request.subscribe({
      // The token stops working on the next request anyway (the backend re-checks the account), so end the session now.
      next: () => this.auth.logout(action === 'delete' ? 'deleted' : 'deactivated'),
      error: (e) => {
        this.dangerError.set(toApiError(e));
        this.dangerBusy.set(false);
      },
    });
  }
}
