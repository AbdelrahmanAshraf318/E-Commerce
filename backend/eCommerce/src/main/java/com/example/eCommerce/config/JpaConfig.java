package com.example.eCommerce.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig
{
    private static final String SYSTEM_AUDITOR = "system";

    /**
     * Fills @CreatedBy / @LastModifiedBy. Without it, BaseEntity.createdBy (NOT NULL) made every insert fail.
     * Falls back to "system" for sign-ups, OAuth2 provisioning and startup seeding.
     */
    @Bean
    public AuditorAware<String> auditorAware()
    {
        return () -> Optional.of(Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName)
                .orElse(SYSTEM_AUDITOR));
    }
}
