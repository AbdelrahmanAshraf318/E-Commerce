import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { CustomerService } from '../../core/customer.service';
import { CustomerOrder, OrderStatus } from '../../core/models';

interface StatusStyle {
  label: string;
  pill: 'pill-success' | 'pill-warning' | 'pill-danger' | 'pill-neutral';
}

const STATUS: Record<OrderStatus, StatusStyle> = {
  SAVED_TO_CART: { label: 'Saved', pill: 'pill-neutral' },
  PENDING_PAYMENT: { label: 'Awaiting payment', pill: 'pill-warning' },
  CONFIRMED: { label: 'Confirmed', pill: 'pill-success' },
  IN_PROGRESS: { label: 'In progress', pill: 'pill-warning' },
  ON_HOLD: { label: 'On hold', pill: 'pill-warning' },
  CANCELED: { label: 'Canceled', pill: 'pill-danger' },
  COMPLETED: { label: 'Completed', pill: 'pill-success' },
  REQUEST_REFUND: { label: 'Refund requested', pill: 'pill-danger' },
};

const UNKNOWN_STATUS: StatusStyle = { label: 'Unknown', pill: 'pill-neutral' };

/** GET /api/v1/users/me/orders - the signed-in customer's order history. */
@Component({
  selector: 'app-orders',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './orders.html',
  styleUrl: './orders.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Orders {
  private readonly customerService = inject(CustomerService);

  protected readonly orders = signal<CustomerOrder[] | null>(null);
  protected readonly loadError = signal<string | null>(null);
  /** Orders whose item list is expanded. */
  protected readonly expanded = signal<ReadonlySet<number>>(new Set());

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loadError.set(null);
    this.customerService.getOrders().subscribe({
      next: (orders) => this.orders.set(orders),
      error: (e) => this.loadError.set(toApiError(e).message),
    });
  }

  protected status(status: OrderStatus | null): StatusStyle {
    return (status && STATUS[status]) || UNKNOWN_STATUS;
  }

  protected itemCount(order: CustomerOrder): number {
    return order.orderItems.reduce((sum, item) => sum + item.quantity, 0);
  }

  protected toggle(orderId: number): void {
    this.expanded.update((ids) => {
      const next = new Set(ids);
      if (!next.delete(orderId)) next.add(orderId);
      return next;
    });
  }
}
