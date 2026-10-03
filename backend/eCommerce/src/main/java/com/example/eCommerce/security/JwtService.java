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
 * <p>
 * A token looks like {@code header.payload.signature}, each part Base64URL-encoded:
 * <pre>
 * header:  {"alg":"RS256"}
 * payload: {"sub":"&lt;user id&gt;","iss":"smartcart-api","iat":...,"exp":...,"email":"...","roles":["ROLE_CUSTOMER"]}
 * </pre>
 * The payload is only <b>encoded, not encrypted</b> - anyone can read it - so it must never contain secrets.
 * What the signature guarantees is that nobody without the private key can <b>change</b> it.
 */
@Service
public class JwtService
{
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String issuer;
    private final Duration expiration;

    /**
     * Loads both keys once, at startup. If either key is missing or invalid, {@link KeyUtils} throws and the
     * application refuses to start - far better than discovering it on the first login.
     *
     * @param securityProperties the "app.security.jwt.*" settings
     */
    public JwtService(SecurityProperties securityProperties)
    {
        SecurityProperties.Jwt jwt = securityProperties.jwt();
        this.privateKey = KeyUtils.loadPrivateKey(jwt.privateKey());
        this.publicKey = KeyUtils.loadPublicKey(jwt.publicKey());
        this.issuer = jwt.issuer();
        this.expiration = jwt.expiration();
    }

    /**
     * Creates a signed access token for a customer who has just proven who they are
     * (password login, sign-up, reactivation or Google sign-in).
     * <p>
     * Claims written:
     * <ul>
     *   <li>{@code sub} - the user id. The id never changes; an email can, so the id is the safer identifier.</li>
     *   <li>{@code iss} - who issued the token; checked again on every request.</li>
     *   <li>{@code iat}/{@code exp} - issued-at and expiry. After {@code exp} the token is rejected automatically.</li>
     *   <li>{@code email}, {@code roles} - convenience data for clients. The server does NOT trust them for
     *       authorization: {@link JwtFilter} reloads the user and their roles from the database.</li>
     * </ul>
     * Signed with the private key using RS256 (RSA + SHA-256).
     *
     * @param customer the authenticated customer (roles must already be loaded)
     * @return the compact token string "header.payload.signature"
     */
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
     * <p>
     * What the parser checks, in order:
     * <ol>
     *   <li>The token is well-formed and <b>signed</b> - unsigned tokens ({@code "alg":"none"}) are refused.</li>
     *   <li>The signature matches our public key - i.e. it was created with our private key and not modified since.</li>
     *   <li>{@code exp} is in the future (jjwt checks this automatically).</li>
     *   <li>{@code iss} equals our issuer ({@code requireIssuer}).</li>
     * </ol>
     * Only then is the subject read and parsed as a UUID.
     *
     * @param token the raw token, without the "Bearer " prefix
     * @return the id of the user the token was issued to
     * @throws JwtException if the token is invalid, tampered with, expired, from another issuer,
     *                      or its subject is not a user id
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

    /**
     * Token lifetime in seconds - returned to clients as "expiresIn" (the OAuth2 convention),
     * so they know when to ask the user to sign in again.
     *
     * @return the configured expiration, in seconds
     */
    public long getExpirationSeconds()
    {
        return expiration.toSeconds();
    }
}
