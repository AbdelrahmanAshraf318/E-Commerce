package com.example.eCommerce.security;

import com.example.eCommerce.user.entity.Customer;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Authenticates requests carrying "Authorization: Bearer &lt;jwt&gt;".
 * <p>
 * An invalid/expired token never fails the request here: the request just stays anonymous and
 * the security rules decide (public endpoints still work, protected ones get a 401).
 * <p>
 * Not a @Component on purpose - Spring Boot would also register it as a plain servlet filter
 * and it would run twice. It is created in {@link com.example.eCommerce.config.SecurityConfig}.
 * <p>
 * Where it runs: inside Spring Security's filter chain, before {@code UsernamePasswordAuthenticationFilter}
 * and well before {@code AuthorizationFilter}, which is the filter that finally decides "allowed or 401/403".
 * Extending {@link OncePerRequestFilter} guarantees it runs once per request, even when the request is
 * forwarded internally (e.g. to /error).
 */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter
{
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomerUserDetailsService userDetailsService;

    /**
     * Called by the servlet container for every request that reaches the security filter chain.
     * <ol>
     *   <li>No "Authorization: Bearer ..." header? Do nothing - the request continues as anonymous.</li>
     *   <li>Already authenticated earlier in the chain? Do nothing - never overwrite an existing authentication.</li>
     *   <li>Verify the token and read the user id ({@link JwtService#extractUserId}).</li>
     *   <li>Load the user from the database and make sure the account is still enabled and not locked.</li>
     *   <li>If everything checks out, store an {@code Authentication} in the {@code SecurityContext};
     *       controllers then receive it as the {@code Authentication} parameter.</li>
     * </ol>
     * In every case the request is passed on with {@code filterChain.doFilter} - this filter only <b>identifies</b>
     * the user; {@code SecurityConfig}'s rules decide what that user may access.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response (not written here)
     * @param filterChain the rest of the filters, ending in the controller
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (Objects.nonNull(authHeader) && authHeader.startsWith(BEARER_PREFIX)
                && Objects.isNull(SecurityContextHolder.getContext().getAuthentication()))
        {
            try
            {
                UUID userId = jwtService.extractUserId(authHeader.substring(BEARER_PREFIX.length()));

                // Re-checked on every request so a deactivated/deleted/locked account loses access
                // immediately instead of when its token expires.
                userDetailsService.findActiveUserById(userId).ifPresent(customer -> authenticate(customer, request));
            }
            catch (JwtException | IllegalArgumentException e)
            {
                log.debug("Rejected bearer token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Marks the current request as authenticated for {@code customer}.
     * <p>
     * {@link UsernamePasswordAuthenticationToken#authenticated} creates a token whose
     * {@code isAuthenticated()} is true. Its three parts:
     * <ul>
     *   <li>principal - the {@link Customer} itself, which is why {@code AuthenticationUtil.getUserId} can cast to it;</li>
     *   <li>credentials - {@code null}: the password is not needed (and must not linger in memory) after authentication;</li>
     *   <li>authorities - the roles loaded from the database, used by rules such as {@code hasRole("ADMIN")}.</li>
     * </ul>
     * The details (client IP, etc.) are attached for auditing. The {@code SecurityContext} is thread-bound and
     * cleared by Spring Security when the request finishes, so it never leaks into another request.
     *
     * @param customer the verified, active customer
     * @param request  the current request
     */
    private void authenticate(Customer customer, HttpServletRequest request)
    {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(customer, null, customer.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
