package com.example.eCommerce.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Everything under "app.security.*" in application.properties, bound into typed, immutable records.
 * <p>
 * Why a record instead of {@code @Value("${...}")} on each field:
 * <ul>
 *   <li>One place lists every security setting the app depends on.</li>
 *   <li>Spring converts the text for us: {@code file:./keys/x.pem} becomes a {@link Resource},
 *       {@code PT1H} becomes a {@link Duration}, a comma-separated list becomes a {@code List}.</li>
 *   <li>{@code @Validated} checks the constraints at startup, so a missing key path or redirect URI
 *       stops the app immediately instead of failing on the first login in production.</li>
 * </ul>
 * Registered by {@code @EnableConfigurationProperties(SecurityProperties.class)} in SecurityConfig.
 *
 * @param jwt    settings for signing and verifying our own access tokens
 * @param cors   which browser origins may call the API
 * @param oauth2 where to send the browser after a Google sign-in
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(@Valid @NotNull Jwt jwt,
                                 @Valid @NotNull Cors cors,
                                 @Valid @NotNull OAuth2 oauth2)
{
    /**
     * "app.security.jwt.*"
     *
     * @param privateKey PEM file with the RSA private key (PKCS#8). Only this app may hold it: whoever has it can mint tokens.
     * @param publicKey  PEM file with the matching RSA public key (X.509). Safe to share: it can only verify tokens.
     * @param issuer     value of the "iss" claim. Tokens issued by anyone else are rejected even if the signature checks out.
     * @param expiration how long an access token stays valid, e.g. PT1H. Short lifetimes limit the damage of a stolen token.
     */
    public record Jwt(@NotNull Resource privateKey,
                      @NotNull Resource publicKey,
                      @NotBlank String issuer,
                      @NotNull Duration expiration)
    {
    }

    /**
     * "app.security.cors.*"
     *
     * @param allowedOrigins exact origins (scheme + host + port) of the SPA, e.g. http://localhost:4200.
     *                       Never use "*" for an authenticated API.
     */
    public record Cors(@NotEmpty List<String> allowedOrigins)
    {
    }

    /**
     * "app.security.oauth2.*"
     *
     * @param authorizedRedirectUri the SPA page that receives the token after Google sign-in.
     *                              Fixed in configuration - never taken from a request parameter, otherwise an attacker
     *                              could make us send a freshly issued token to their own site (open redirect).
     */
    public record OAuth2(@NotBlank String authorizedRedirectUri)
    {
    }
}
