import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * − [n] + control. Emits the new value; the parent decides what to do with it
 * (the cart page sends it to the server, the product page just keeps it).
 */
@Component({
  selector: 'app-quantity-stepper',
  template: `
    <div class="stepper" [class.stepper-sm]="size() === 'sm'" role="group" [attr.aria-label]="'Quantity for ' + label()">
      <button type="button" class="stepper-btn" (click)="valueChange.emit(value() - 1)"
              [disabled]="disabled() || value() <= min()" aria-label="Decrease quantity">
        <i class="bi bi-dash"></i>
      </button>
      <output class="stepper-value" [attr.aria-live]="'polite'">{{ value() }}</output>
      <button type="button" class="stepper-btn" (click)="valueChange.emit(value() + 1)"
              [disabled]="disabled() || value() >= max()" aria-label="Increase quantity">
        <i class="bi bi-plus"></i>
      </button>
    </div>
  `,
  styles: `
    .stepper {
      display: inline-flex;
      align-items: center;
      background: var(--sc-surface);
      border: 1px solid var(--sc-line);
      border-radius: 999px;
      padding: 0.25rem;
    }
    .stepper-btn {
      width: 2.5rem;
      height: 2.5rem;
      border: 0;
      border-radius: 999px;
      background: transparent;
      color: var(--sc-ink);
      font-size: 1.15rem;
      display: grid;
      place-items: center;
      transition: background-color 0.15s ease;
    }
    .stepper-btn:hover:not(:disabled) { background: var(--sc-surface-2); }
    .stepper-btn:disabled { color: var(--sc-line-strong); }
    .stepper-value {
      min-width: 2.25rem;
      text-align: center;
      font-weight: 700;
      font-variant-numeric: tabular-nums;
      color: var(--sc-ink);
    }
    .stepper-sm .stepper-btn { width: 2rem; height: 2rem; font-size: 1rem; }
    .stepper-sm .stepper-value { min-width: 1.75rem; font-size: 0.9rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuantityStepper {
  readonly value = input.required<number>();
  readonly min = input(1);
  readonly max = input(10);
  readonly disabled = input(false);
  readonly size = input<'sm' | 'md'>('md');
  readonly label = input('item');
  readonly valueChange = output<number>();
}
