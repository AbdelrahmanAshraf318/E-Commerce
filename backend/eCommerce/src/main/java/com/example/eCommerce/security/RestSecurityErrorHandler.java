package com.example.eCommerce.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Security exceptions are thrown inside filters, before any controller runs, so @RestControllerAdvice
 * never sees them. Forwarding them to the HandlerExceptionResolver gives 401/403 responses the same
 * JSON shape (ProblemDetail) as every other API error - instead of a redirect to the OAuth2 login page.
 * <p>
 * The difference between the two cases:
 * <ul>
 *   <li><b>401 Unauthorized</b> = "I don't know who you are" (no token, invalid or expired token) -> {@link #commence}.</li>
 *   <li><b>403 Forbidden</b> = "I know who you are, but you may not do this" (e.g. a customer calling an admin
 *       endpoint) -> {@link #handle}.</li>
 * </ul>
 */
@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler
{
    private final HandlerExceptionResolver resolver;

    /**
     * Spring MVC defines several {@link HandlerExceptionResolver} beans; the one named "handlerExceptionResolver"
     * is the composite that includes our {@code @RestControllerAdvice} (GlobalExceptionHandler).
     *
     * @param resolver Spring MVC's main exception resolver
     */
    public RestSecurityErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
    {
        this.resolver = resolver;
    }

    /**
     * Called by {@code ExceptionTranslationFilter} when an <b>anonymous</b> request hits a protected endpoint.
     * Without this, {@code oauth2Login()} would answer with a 302 redirect to Google's login page - useless for a
     * JavaScript client, which needs a 401 to know it should send the user to the sign-in screen.
     *
     * @param request       the rejected request
     * @param response      where the 401 ProblemDetail is written
     * @param authException why authentication is required
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
    {
        resolver.resolveException(request, response, null, authException);
    }

    /**
     * Called by {@code ExceptionTranslationFilter} when an <b>authenticated</b> user lacks the required role/permission.
     *
     * @param request               the rejected request
     * @param response              where the 403 ProblemDetail is written
     * @param accessDeniedException why access was denied
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
    {
        resolver.resolveException(request, response, null, accessDeniedException);
    }
}
