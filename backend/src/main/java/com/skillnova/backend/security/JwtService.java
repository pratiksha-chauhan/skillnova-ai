package com.skillnova.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates JWT access tokens.
 *
 * Tokens are signed with HMAC-SHA256 using a shared secret (configured via
 * {@code jwt.secret}, sourced from the JWT_SECRET env var in prod). The
 * subject claim carries the user's UUID (as a string) and a custom "email"
 * claim carries the email, so callers can identify the user without an
 * extra DB lookup if they only need the email for display purposes - the
 * filter still re-resolves the authoritative entity via the repository.
 */
@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret must be configured (set the JWT_SECRET environment variable)");
        }
        // HS256 requires a key of at least 256 bits; using the UTF-8 bytes of
        // the configured secret directly keeps this deterministic across
        // restarts/instances (no random per-process key).
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(EMAIL_CLAIM, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Parses and validates the token's signature and expiry, returning the
     * claims if valid.
     *
     * @throws JwtException if the token is malformed, expired, or the
     *                       signature does not match
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return parseClaims(token).get(EMAIL_CLAIM, String.class);
    }

    /**
     * @return true if the token parses and verifies cleanly; false for any
     *         malformed/expired/tampered token (never throws).
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
