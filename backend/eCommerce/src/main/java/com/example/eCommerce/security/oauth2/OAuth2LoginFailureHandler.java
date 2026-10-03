package com.example.eCommerce.security.oauth2;

import com.example.eCommerce.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * User cancelled on Google's consent screen, state mismatch, etc. Send them back to the SPA with an error code.
 * <p>
 * Without this handler Spring Security would redirect to its own HTML "/login?error" page,
 * which does not exist in an API-only backend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler
{
    private final SecurityProperties securityProperties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    /**
     * Called when the Google sign-in fails <b>before</b> we know who the user is. Typical causes:
     * <ul>
     *   <li>the user clicked "Cancel" on Google's consent screen ({@code error=access_denied});</li>
     *   <li>the {@code state} value does not match the one stored in the session - a possible forged login (CSRF);</li>
     *   <li>exchanging the code with Google failed (wrong client secret, expired code, network error).</li>
     * </ul>
     * The details are logged server-side only; the browser gets a generic {@code OAUTH2_FAILED} code, so nothing
     * internal leaks to the client. The handshake session is discarded either way.
     *
     * @param request   the failed callback request
     * @param response  used to send the redirect
     * @param exception what went wrong
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException
    {
        log.warn("OAuth2 login failed: {}", exception.getMessage());

        if (request.getSession(false) != null)
            request.getSession(false).invalidate();

        String target = UriComponentsBuilder.fromUriString(securityProperties.oauth2().authorizedRedirectUri())
                .queryParam("error", "OAUTH2_FAILED")
                .toUriString();
        redirectStrategy.sendRedirect(request, response, target);
    }
}
