import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { CustomerService } from '../../core/customer.service';
import { ActivityEntry, AuditAction } from '../../core/models';

interface ActionStyle {
  label: string;
  icon: string;
  tone: 'primary' | 'success' | 'warning' | 'danger';
}

const ACTIONS: Record<AuditAction, ActionStyle> = {
  ACCOUNT_REGISTERED: { label: 'Account created', icon: 'bi-person-plus', tone: 'success' },
  SIGNED_IN: { label: 'Signed in', icon: 'bi-box-arrow-in-right', tone: 'primary' },
  PROFILE_UPDATED: { label: 'Profile updated', icon: 'bi-person-gear', tone: 'primary' },
  PASSWORD_CHANGED: { label: 'Password changed', icon: 'bi-shield-lock', tone: 'warning' },
  ACCOUNT_DEACTIVATED: { label: 'Account deactivated', icon: 'bi-pause-circle', tone: 'danger' },
  ACCOUNT_REACTIVATED: { label: 'Account reactivated', icon: 'bi-play-circle', tone: 'success' },
  ACCOUNT_DELETED: { label: 'Account deleted', icon: 'bi-trash3', tone: 'danger' },
  CART_ITEM_ADDED: { label: 'Added to cart', icon: 'bi-bag-plus', tone: 'primary' },
  CART_ITEM_UPDATED: { label: 'Cart quantity changed', icon: 'bi-bag', tone: 'primary' },
  CART_ITEM_REMOVED: { label: 'Removed from cart', icon: 'bi-bag-dash', tone: 'warning' },
  CART_CLEARED: { label: 'Cart emptied', icon: 'bi-bag-x', tone: 'warning' },
};

const UNKNOWN_ACTION: ActionStyle = { label: 'Activity', icon: 'bi-dot', tone: 'primary' };

const PAGE_SIZE = 20;

interface DayGroup {
  day: string; // yyyy-MM-dd, from the server's local date-time
  entries: ActivityEntry[];
}

/** GET /api/v1/users/me/activity - the account's audit log, loaded a page at a time. */
@Component({
  selector: 'app-activity',
  imports: [DatePipe, RouterLink],
  templateUrl: './activity.html',
  styleUrl: './activity.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Activity {
  private readonly customerService = inject(CustomerService);
  private readonly auth = inject(AuthService);

  protected readonly entries = signal<ActivityEntry[] | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly loading = signal(false);
  protected readonly loadError = signal<string | null>(null);
  private nextPage = 0;
  private totalPages = 0;

  protected readonly hasMore = computed(() => (this.entries()?.length ?? 0) < this.totalElements());

  /** Entries arrive newest first, so consecutive grouping keeps the days in order. */
  protected readonly days = computed<DayGroup[]>(() => {
    const groups: DayGroup[] = [];
    for (const entry of this.entries() ?? []) {
      const day = entry.occurredAt.slice(0, 10);
      const last = groups.at(-1);
      if (last?.day === day) last.entries.push(entry);
      else groups.push({ day, entries: [entry] });
    }
    return groups;
  });

  constructor() {
    this.loadMore();
  }

  protected loadMore(): void {
    if (this.loading() || (this.entries() !== null && this.nextPage >= this.totalPages)) return;

    this.loading.set(true);
    this.loadError.set(null);
    this.customerService.getActivity(this.nextPage, PAGE_SIZE).subscribe({
      next: (page) => {
        this.entries.update((current) => [...(current ?? []), ...page.content]);
        this.totalElements.set(page.totalElements);
        this.totalPages = page.totalPages;
        this.nextPage = page.page + 1;
        this.loading.set(false);
      },
      error: (e) => {
        this.loadError.set(toApiError(e).message);
        this.loading.set(false);
      },
    });
  }

  protected style(action: AuditAction): ActionStyle {
    return ACTIONS[action] ?? UNKNOWN_ACTION;
  }

  /** "You", "SmartCart" for system-recorded events, or the email of whoever acted on the account. */
  protected actorLabel(actor: string): string {
    if (actor === 'system') return 'SmartCart';
    return actor.toLowerCase() === this.auth.user()?.email.toLowerCase() ? 'You' : actor;
  }
}
