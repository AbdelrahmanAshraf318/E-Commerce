import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-toasts',
  imports: [RouterLink],
  template: `
    <div class="toast-stack" aria-live="polite" aria-atomic="false">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="sc-toast" [class]="'sc-toast-' + toast.kind" role="status">
          <i class="bi toast-icon"
             [class.bi-check-circle-fill]="toast.kind === 'success'"
             [class.bi-exclamation-circle-fill]="toast.kind === 'error'"
             [class.bi-info-circle-fill]="toast.kind === 'info'"></i>
          <div class="flex-grow-1 min-w-0">
            <div class="fw-semibold">{{ toast.title }}</div>
            @if (toast.message) {
              <div class="small toast-message">{{ toast.message }}</div>
            }
            @if (toast.link; as link) {
              <a class="small fw-semibold" [routerLink]="link.url" (click)="toastService.dismiss(toast.id)">{{ link.label }} →</a>
            }
          </div>
          <button type="button" class="btn-close btn-close-sm" aria-label="Dismiss" (click)="toastService.dismiss(toast.id)"></button>
        </div>
      }
    </div>
  `,
  styles: `
    .toast-stack {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      z-index: 1090;
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
      width: min(22rem, calc(100vw - 2rem));
    }
    .sc-toast {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.9rem 1rem;
      background: rgba(15, 23, 42, 0.94);
      color: #f8fafc;
      border-radius: 1rem;
      box-shadow: 0 20px 40px -12px rgba(15, 23, 42, 0.45);
      backdrop-filter: blur(8px);
      animation: sc-fade-up 0.3s cubic-bezier(0.2, 0.8, 0.2, 1) both;
    }
    .sc-toast a { color: #a5b4fc; text-decoration: none; }
    .toast-message { color: #cbd5e1; }
    .toast-icon { font-size: 1.15rem; line-height: 1.4; }
    .sc-toast-success .toast-icon { color: #34d399; }
    .sc-toast-error .toast-icon { color: #f87171; }
    .sc-toast-info .toast-icon { color: #93c5fd; }
    .btn-close { filter: invert(1); opacity: 0.6; }
    .min-w-0 { min-width: 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Toasts {
  protected readonly toastService = inject(ToastService);
}
