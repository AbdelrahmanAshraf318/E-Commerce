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
 * Everything under "app.security.*" in application.properties.
 * Validated on startup so a missing key file or redirect URI fails fast instead of on the first login.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(@Valid @NotNull Jwt jwt,
                                 @Valid @NotNull Cors cors,
                                 @Valid @NotNull OAuth2 oauth2)
{
    public record Jwt(@NotNull Resource privateKey,
                      @NotNull Resource publicKey,
                      @NotBlank String issuer,
                      @NotNull Duration expiration)
    {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins)
    {
    }

    public record OAuth2(@NotBlank String authorizedRedirectUri)
    {
    }
}
