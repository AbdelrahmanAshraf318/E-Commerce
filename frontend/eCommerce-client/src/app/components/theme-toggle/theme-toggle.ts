import { ChangeDetectionStrategy, Component, ElementRef, inject, signal } from '@angular/core';
import { ThemePreference, ThemeService } from '../../core/theme.service';

const OPTIONS: { value: ThemePreference; label: string; icon: string }[] = [
  { value: 'light', label: 'Light', icon: 'bi-sun' },
  { value: 'dark', label: 'Dark', icon: 'bi-moon-stars' },
  { value: 'system', label: 'System', icon: 'bi-circle-half' },
];

@Component({
  selector: 'app-theme-toggle',
  template: `
    <div class="theme-toggle">
      <button type="button" class="nav-icon-btn" (click)="open.set(!open())" [attr.aria-expanded]="open()"
              aria-haspopup="menu" [attr.aria-label]="'Theme: ' + themeService.preference()">
        <i class="bi" [class.bi-moon-stars]="themeService.theme() === 'dark'" [class.bi-sun]="themeService.theme() === 'light'"></i>
      </button>
      @if (open()) {
        <div class="theme-menu" role="menu">
          @for (option of options; track option.value) {
            <button type="button" role="menuitemradio" class="theme-option"
                    [attr.aria-checked]="themeService.preference() === option.value"
                    [class.is-active]="themeService.preference() === option.value" (click)="choose(option.value)">
              <i class="bi {{ option.icon }}"></i>
              <span>{{ option.label }}</span>
              @if (themeService.preference() === option.value) {
                <i class="bi bi-check2 ms-auto"></i>
              }
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .theme-toggle { position: relative; }
    .nav-icon-btn {
      width: 2.6rem; height: 2.6rem; border: 0; border-radius: 999px;
      display: grid; place-items: center; font-size: 1.15rem;
      color: var(--sc-ink); background: transparent; transition: background-color 0.15s ease;
    }
    .nav-icon-btn:hover, .nav-icon-btn[aria-expanded='true'] { background: var(--sc-surface-2); }
    .theme-menu {
      position: absolute; right: 0; top: calc(100% + 0.5rem); z-index: 1050; min-width: 10rem; padding: 0.35rem;
      background: var(--sc-surface); border: 1px solid var(--sc-line); border-radius: 0.9rem; box-shadow: var(--sc-shadow-lg);
      animation: sc-fade-up 0.18s ease both;
    }
    .theme-option {
      width: 100%; display: flex; align-items: center; gap: 0.6rem; padding: 0.55rem 0.7rem;
      border: 0; border-radius: 0.6rem; background: transparent; color: var(--sc-text); font-size: 0.9rem; font-weight: 600;
    }
    .theme-option:hover { background: var(--sc-surface-2); color: var(--sc-ink); }
    .theme-option.is-active { color: var(--sc-primary-hover); }
  `,
  host: { '(document:click)': 'onDocumentClick($event)', '(document:keydown.escape)': 'open.set(false)' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeToggle {
  protected readonly themeService = inject(ThemeService);
  private readonly host = inject(ElementRef<HTMLElement>);
  protected readonly options = OPTIONS;
  protected readonly open = signal(false);

  protected choose(preference: ThemePreference): void {
    this.themeService.setPreference(preference);
    this.open.set(false);
  }

  protected onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }
}
