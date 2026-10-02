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
 */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter
{
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomerUserDetailsService userDetailsService;

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

    private void authenticate(Customer customer, HttpServletRequest request)
    {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(customer, null, customer.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
