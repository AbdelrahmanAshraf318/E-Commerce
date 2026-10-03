package com.example.eCommerce.security.oauth2;

import com.example.eCommerce.exception.BusinessException;
import com.example.eCommerce.security.SecurityProperties;
import com.example.eCommerce.user.enums.AuthProvider;
import com.example.eCommerce.user.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 * Runs after Google has authenticated the user.
 * Finds-or-creates the Customer, issues OUR JWT and sends the browser back to the SPA.
 * <p>
 * The token goes in the URL <b>fragment</b> (#token=...): browsers never send fragments to servers,
 * so it does not end up in access logs or Referer headers.
 * <p>
 * By the time this runs, Spring Security's {@code OAuth2LoginAuthenticationFilter} has already done the hard part
 * of the Authorization Code flow: checked the {@code state} parameter (CSRF protection for the login itself),
 * exchanged the one-time {@code code} for tokens directly with Google (server to server, using the client secret),
 * validated Google's ID token (OpenID Connect) and loaded the user's profile.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler
{
    private final AuthService authService;
    private final SecurityProperties securityProperties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    /**
     * Converts a successful Google login into a SmartCart session.
     * <ol>
     *   <li>Read the provider ("google" -> {@link AuthProvider#GOOGLE}) and the user's verified profile:
     *       {@code email}, {@code name} and {@code email_verified}.</li>
     *   <li>{@link AuthService#loginWithOAuth2} finds the customer by email or creates one, refuses unverified
     *       emails (account-takeover protection) and locked accounts, reactivates deactivated ones, and returns our JWT.</li>
     *   <li>Redirect to the SPA: {@code /oauth2/callback#token=...} on success,
     *       {@code /oauth2/callback?error=CODE} on failure. The error code is safe to put in a query string; the token is not.</li>
     *   <li>Always clean up: clear the SecurityContext and invalidate the short-lived HTTP session that carried the
     *       OAuth2 {@code state} across the Google redirect - after this, the API is stateless again.</li>
     * </ol>
     * Google's own access token is not kept: we only needed it to learn who the user is.
     *
     * @param request        the callback request from Google (/login/oauth2/code/google)
     * @param response       used to send the redirect
     * @param authentication an {@link OAuth2AuthenticationToken} holding Google's view of the user
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException
    {
        String redirectUri = securityProperties.oauth2().authorizedRedirectUri();
        String target;

        try
        {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauthUser = oauthToken.getPrincipal();
            AuthProvider provider = AuthProvider.valueOf(
                    oauthToken.getAuthorizedClientRegistrationId().toUpperCase(Locale.ROOT));

            String accessToken = authService.loginWithOAuth2(
                    provider,
                    oauthUser.getAttribute("email"),
                    oauthUser.getAttribute("name"),
                    Boolean.TRUE.equals(oauthUser.getAttribute("email_verified")));

            target = UriComponentsBuilder.fromUriString(redirectUri).fragment("token=" + accessToken).toUriString();
        }
        catch (BusinessException e)
        {
            target = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", e.getErrorCode().getCode()).toUriString();
        }
        catch (RuntimeException e)
        {
            log.error("OAuth2 login could not be completed", e);
            target = UriComponentsBuilder.fromUriString(redirectUri).queryParam("error", "OAUTH2_FAILED").toUriString();
        }
        finally
        {
            // The HTTP session only existed to carry the OAuth2 "state" across the Google redirect.
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null)
                request.getSession(false).invalidate();
        }

        redirectStrategy.sendRedirect(request, response, target);
    }
}
