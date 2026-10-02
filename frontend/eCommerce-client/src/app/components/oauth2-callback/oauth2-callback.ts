import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Landing page after Google sign-in. The backend redirects to either
 *   /oauth2/callback#token=<jwt>       on success, or
 *   /oauth2/callback?error=<CODE>      on failure.
 */
@Component({
  selector: 'app-oauth2-callback',
  template: `
    <div class="d-flex flex-column align-items-center justify-content-center py-5 text-muted">
      <div class="spinner-border text-primary mb-3" role="status"></div>
      Signing you in…
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OAuth2Callback implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  ngOnInit(): void {
    const snapshot = this.route.snapshot;
    const token = new URLSearchParams(snapshot.fragment ?? '').get('token');
    const error = snapshot.queryParamMap.get('error');

    // Remove the token from the address bar and from history straight away.
    history.replaceState(null, '', '/oauth2/callback');

    if (!token) {
      this.failWith(error ?? 'OAUTH2_FAILED');
      return;
    }

    this.auth.completeOAuth2Login(token).subscribe({
      next: (profile) =>
        this.router.navigateByUrl(profile.profileComplete ? '/products' : '/account/complete-profile', { replaceUrl: true }),
      error: () => this.failWith('OAUTH2_FAILED'),
    });
  }

  private failWith(error: string): void {
    this.router.navigate(['/login'], { queryParams: { error }, replaceUrl: true });
  }
}
