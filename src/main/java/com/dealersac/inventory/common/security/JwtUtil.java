package com.dealersac.inventory.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT generation and validation.
 *
 * Security:
 * - HS256 algorithm explicitly enforced (no alg confusion)
 * - Secret loaded from environment variable only
 * - Includes jti (JWT ID) for token revocation support
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT with tenantId and role claims.
     */
    public String generateToken(String username, String tenantId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .claim("role", role)
                .id(UUID.randomUUID().toString())   // jti — for blacklisting
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS256) // explicit algorithm
                .compact();
    }

    /**
     * Parse and validate a JWT. Returns the Claims or throws.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT error: {}", e.getMessage());
        }
        return false;
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String extractTenantId(String token) {
        return parseToken(token).get("tenantId", String.class);
    }

    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    public Date extractExpiration(String token) {
        return parseToken(token).getExpiration();
    }
}
