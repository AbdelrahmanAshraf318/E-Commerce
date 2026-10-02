package com.example.eCommerce.security;

import com.example.eCommerce.user.entity.Customer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and verifies RS256 access tokens.
 * <p>
 * Keys come from configuration, so tokens survive restarts and every instance of the app
 * (or another service holding only the public key) can verify them.
 */
@Service
public class JwtService
{
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String issuer;
    private final Duration expiration;

    public JwtService(SecurityProperties securityProperties)
    {
        SecurityProperties.Jwt jwt = securityProperties.jwt();
        this.privateKey = KeyUtils.loadPrivateKey(jwt.privateKey());
        this.publicKey = KeyUtils.loadPublicKey(jwt.publicKey());
        this.issuer = jwt.issuer();
        this.expiration = jwt.expiration();
    }

    public String generateToken(Customer customer)
    {
        Instant now = Instant.now();

        return Jwts.builder()
                // The id never changes; an email can.
                .subject(customer.getUserId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .claim("email", customer.getEmail())
                .claim("roles", customer.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Verifies signature, issuer and expiry, then returns the user id.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public UUID extractUserId(String token)
    {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        try
        {
            return UUID.fromString(claims.getSubject());
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            throw new JwtException("Token subject is not a user id", e);
        }
    }

    public long getExpirationSeconds()
    {
        return expiration.toSeconds();
    }
}
