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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler
{
    private final AuthService authService;
    private final SecurityProperties securityProperties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

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
