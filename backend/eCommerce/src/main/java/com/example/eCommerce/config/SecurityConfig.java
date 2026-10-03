package com.example.eCommerce.config;

import com.example.eCommerce.security.CustomerUserDetailsService;
import com.example.eCommerce.security.JwtFilter;
import com.example.eCommerce.security.JwtService;
import com.example.eCommerce.security.RestSecurityErrorHandler;
import com.example.eCommerce.security.SecurityProperties;
import com.example.eCommerce.security.oauth2.OAuth2LoginFailureHandler;
import com.example.eCommerce.security.oauth2.OAuth2LoginSuccessHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Two ways in, one token out:
 * <ul>
 *   <li>Email + password: POST /api/v1/auth/login (or /register) returns a JWT.</li>
 *   <li>Google: the browser goes to /oauth2/authorization/google; after consent Google calls
 *       /login/oauth2/code/google and {@link OAuth2LoginSuccessHandler} redirects back to the SPA with a JWT.</li>
 * </ul>
 * After that every API call is authenticated the same way, by {@link JwtFilter}.
 * <p>
 * No dependencies are injected through the constructor - beans are passed to the @Bean methods instead.
 * That keeps this class out of the dependency cycle that broke startup
 * (SecurityConfig -> CustomerService -> AuthenticationManager -> SecurityConfig).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig
{
    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/reactivate"
    };

    private static final String[] PUBLIC_CATALOG_ENDPOINTS = {
            "/api/v1/products",
            "/api/v1/products/**"
    };

    private static final String[] OAUTH2_ENDPOINTS = {
            "/oauth2/authorization/**",
            "/login/oauth2/code/**"
    };

    private static final String[] API_DOCS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * The rules every HTTP request goes through. Spring Security turns this into an ordered chain of servlet
     * filters (CORS, OAuth2 login, our JwtFilter, exception translation, authorization, ...) that runs before
     * any controller.
     * <ul>
     *   <li><b>CSRF off</b> - CSRF attacks ride on cookies the browser sends automatically. This API is authenticated
     *       only by an {@code Authorization} header that JavaScript must add explicitly, so a forged cross-site request
     *       arrives anonymous. (If tokens ever move into cookies, CSRF protection must come back on.)</li>
     *   <li><b>CORS on</b> - lets the browser call the API from the SPA's origin; see {@link #corsConfigurationSource}.</li>
     *   <li><b>formLogin / httpBasic / logout off</b> - those are for server-rendered apps; they would add HTML login
     *       pages and Basic-auth prompts we do not want. Logout in a stateless API = the client discards the token.</li>
     *   <li><b>STATELESS</b> - no HTTP session stores who you are; every request proves it again with its token.</li>
     *   <li><b>authorizeHttpRequests</b> - first matching rule wins: sign-up/login and the catalogue are public,
     *       everything else needs an authenticated user. Matching includes the HTTP method, so e.g. only GET on
     *       products is public.</li>
     *   <li><b>oauth2Login</b> - adds the "/oauth2/authorization/google" and "/login/oauth2/code/google" endpoints
     *       and plugs in our success/failure handlers.</li>
     *   <li><b>exceptionHandling</b> - turns "not logged in" into a JSON 401 and "not allowed" into a JSON 403.</li>
     *   <li><b>JwtFilter</b> - placed before {@link UsernamePasswordAuthenticationFilter}, so the request is already
     *       identified by the time authorization rules are evaluated.</li>
     * </ul>
     *
     * @return the configured filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   CustomerUserDetailsService userDetailsService,
                                                   RestSecurityErrorHandler restSecurityErrorHandler,
                                                   OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                                                   OAuth2LoginFailureHandler oAuth2LoginFailureHandler)
    {
        return http
                // Safe to disable: the API authenticates with an Authorization header, never with cookies,
                // so a cross-site form cannot make an authenticated request on the user's behalf.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // Spring Security never creates a session for authentication. The OAuth2 handshake still uses a
                // short-lived one to store the "state" parameter; the success/failure handlers invalidate it.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_CATALOG_ENDPOINTS).permitAll()
                        .requestMatchers(OAUTH2_ENDPOINTS).permitAll()
                        .requestMatchers(API_DOCS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                // Must come after oauth2Login(): otherwise unauthenticated API calls get a 302 to Google
                // instead of a 401 the SPA can react to.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restSecurityErrorHandler)
                        .accessDeniedHandler(restSecurityErrorHandler))
                .addFilterBefore(new JwtFilter(jwtService, userDetailsService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Checks email + password logins (used by AuthServiceImpl.login).
     * <p>
     * {@link ProviderManager} delegates to its providers; we have one, {@link DaoAuthenticationProvider}, which
     * loads the user through {@link CustomerUserDetailsService} and verifies the password with the BCrypt
     * {@link PasswordEncoder}. BCrypt is deliberately slow and salts every hash, so stolen hashes are expensive
     * to crack and two equal passwords never produce the same hash.
     * <p>
     * Unknown emails and wrong passwords both surface as {@code BadCredentialsException} (user-not-found is hidden
     * by default), so the login response never reveals whether an email is registered.
     *
     * @param userDetailsService looks customers up by email
     * @param passwordEncoder    the BCrypt encoder from BeansConfig
     * @return the manager used for password authentication
     */
    @Bean
    public AuthenticationManager authenticationManager(CustomerUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder)
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * CORS: which <b>other websites</b> a browser may let call this API.
     * <p>
     * Browsers block JavaScript on http://localhost:4200 from reading responses from http://localhost:8080
     * (a different origin) unless the server opts in. Before "non-simple" requests (JSON bodies, an Authorization
     * header) the browser sends a preflight {@code OPTIONS} request; Spring answers it from this configuration.
     * <ul>
     *   <li>Origins - only the configured SPA origin(s); never "*" for an authenticated API.</li>
     *   <li>Headers - Authorization (the token) and Content-Type (JSON).</li>
     *   <li>Credentials - off: we send tokens in a header, not cookies.</li>
     *   <li>Max age - the browser may cache the preflight answer for an hour, saving a round trip per request.</li>
     * </ul>
     * Only "/api/**" gets CORS: the OAuth2 endpoints are full-page navigations, not JavaScript calls.
     * Note CORS is a <b>browser</b> protection - it does not stop curl or a server from calling the API;
     * authentication does that.
     *
     * @param securityProperties provides the allowed origins
     * @return the CORS configuration used by {@code http.cors()}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties)
    {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(securityProperties.cors().allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Bearer tokens, not cookies - so no credentials are needed cross-origin.
        cors.setAllowCredentials(false);
        cors.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
