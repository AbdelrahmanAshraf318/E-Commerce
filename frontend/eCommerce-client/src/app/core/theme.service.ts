import { computed, effect, Injectable, signal } from '@angular/core';

export type ThemePreference = 'light' | 'dark' | 'system';
export type Theme = 'light' | 'dark';

/** Must match the inline script in index.html, which applies the theme before Angular boots. */
const STORAGE_KEY = 'smartcart.theme';

/**
 * Light / dark / follow-the-OS. The whole palette is CSS variables (styles.css), so switching is a single
 * attribute on <html>: data-bs-theme="dark" - which also flips Bootstrap's own components.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  // matchMedia is missing in some environments (jsdom in unit tests, very old embedded browsers).
  private readonly media: MediaQueryList | null = window.matchMedia?.('(prefers-color-scheme: dark)') ?? null;
  private readonly systemPrefersDark = signal(this.media?.matches ?? false);

  readonly preference = signal<ThemePreference>(readPreference());
  readonly theme = computed<Theme>(() => {
    const preference = this.preference();
    return preference === 'system' ? (this.systemPrefersDark() ? 'dark' : 'light') : preference;
  });

  constructor() {
    // Follow OS changes live while the preference is "system".
    this.media?.addEventListener('change', (e) => this.systemPrefersDark.set(e.matches));
    effect(() => document.documentElement.setAttribute('data-bs-theme', this.theme()));
  }

  setPreference(preference: ThemePreference): void {
    const root = document.documentElement;
    // Animate only user-initiated switches (never the first paint).
    root.classList.add('theme-transition');
    this.preference.set(preference);
    try {
      if (preference === 'system') localStorage.removeItem(STORAGE_KEY);
      else localStorage.setItem(STORAGE_KEY, preference);
    } catch {
      // Storage blocked (private mode): the choice still applies for this visit.
    }
    setTimeout(() => root.classList.remove('theme-transition'), 350);
  }
}

function readPreference(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'light' || stored === 'dark' ? stored : 'system';
  } catch {
    return 'system';
  }
}
