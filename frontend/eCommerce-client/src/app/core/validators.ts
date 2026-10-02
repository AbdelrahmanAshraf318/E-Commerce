import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Same rule as the backend's PasswordPolicy.REGEX - the server is still the source of truth. */
export const PASSWORD_PATTERN = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()\-\[\]{}:;',?/*~$^+=<>]).{8,20}$/;

export const PASSWORD_HINT =
  '8-20 characters with an uppercase letter, a lowercase letter, a digit and a special character.';

export function passwordStrength(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    !control.value || PASSWORD_PATTERN.test(control.value) ? null : { password: PASSWORD_HINT };
}

/** Group-level validator: `confirmKey` must equal `key`. The error is put on the confirm control. */
export function matchFields(key: string, confirmKey: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const confirm = group.get(confirmKey);
    if (!confirm) return null;
    const mismatch = !!confirm.value && group.get(key)?.value !== confirm.value;
    const { mismatch: _, ...others } = confirm.errors ?? {};
    confirm.setErrors(mismatch ? { ...others, mismatch: 'Passwords do not match.' } : Object.keys(others).length ? others : null);
    return null;
  };
}

export function pastDate(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    return new Date(control.value) < new Date() ? null : { pastDate: 'Date of birth must be in the past.' };
  };
}

/** Today as yyyy-MM-dd, for <input type="date" [max]>. */
export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}
