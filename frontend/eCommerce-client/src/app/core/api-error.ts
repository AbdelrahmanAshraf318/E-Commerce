import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';

/**
 * Normalized form of the backend's ProblemDetail:
 * { status, title, detail, code, errors?: { field: message } }
 */
export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors: Record<string, string>;
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return {
        status: 0,
        code: 'NETWORK_ERROR',
        message: 'Cannot reach the server. Is the backend running?',
        fieldErrors: {},
      };
    }
    const body = (error.error ?? {}) as Partial<{ code: string; detail: string; errors: Record<string, string> }>;
    return {
      status: error.status,
      code: body.code ?? `HTTP_${error.status}`,
      message: body.detail ?? error.message,
      fieldErrors: body.errors ?? {},
    };
  }
  return { status: -1, code: 'UNKNOWN', message: 'Something went wrong.', fieldErrors: {} };
}

/**
 * Puts server-side validation messages on the matching form controls.
 * Returns true if at least one field error was applied.
 */
export function applyFieldErrors(form: FormGroup, apiError: ApiError): boolean {
  let applied = false;
  for (const [field, message] of Object.entries(apiError.fieldErrors)) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}

const MESSAGES: Record<string, (args: any) => string> = {
  required: () => 'This field is required.',
  email: () => 'Enter a valid email address.',
  minlength: (e) => `Must be at least ${e.requiredLength} characters.`,
  maxlength: (e) => `Must be at most ${e.requiredLength} characters.`,
  pattern: () => 'Invalid format.',
  server: (message) => message,
};

/** First human-readable error for a control, but only once the user has interacted with it. */
export function controlError(control: AbstractControl | null, overrides: Record<string, string> = {}): string | null {
  if (!control || !control.errors || !(control.touched || control.dirty)) {
    return null;
  }
  const [key, value] = Object.entries(control.errors)[0];
  return overrides[key] ?? MESSAGES[key]?.(value) ?? (typeof value === 'string' ? value : 'Invalid value.');
}
