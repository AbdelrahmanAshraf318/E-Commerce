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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler
{
    private final SecurityProperties securityProperties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

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
