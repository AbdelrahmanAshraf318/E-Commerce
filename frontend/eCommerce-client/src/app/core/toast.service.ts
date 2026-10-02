import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  kind: 'success' | 'error' | 'info';
  title: string;
  message?: string;
  /** Optional in-app link shown in the toast, e.g. "View cart". */
  link?: { label: string; url: string };
}

/** Short-lived, non-blocking notifications. Rendered once by <app-toasts> in the app shell. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  show(toast: Omit<Toast, 'id'>, durationMs = 3500): void {
    const id = this.nextId++;
    // Keep at most 3 on screen.
    this.toasts.update((list) => [...list.slice(-2), { ...toast, id }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }

  success(title: string, message?: string, link?: Toast['link']): void {
    this.show({ kind: 'success', title, message, link });
  }

  error(title: string, message?: string): void {
    this.show({ kind: 'error', title, message }, 5000);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
