package com.skillnova.backend.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-signing-secret-at-least-256-bits-long-0123456789";

    @Test
    void generatesAndValidatesATokenRoundTrip() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "user@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void rejectsAnExpiredToken() throws InterruptedException {
        // 1ms expiration so the token is already expired by the time we check it.
        JwtService jwtService = new JwtService(SECRET, 1L);
        String token = jwtService.generateToken(UUID.randomUUID(), "user@example.com");

        Thread.sleep(25);

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void rejectsATamperedToken() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        String token = jwtService.generateToken(UUID.randomUUID(), "user@example.com");

        String tampered = token.substring(0, token.length() - 1)
                + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtService signedWithSecretA = new JwtService(SECRET, 3_600_000L);
        JwtService verifiesWithSecretB = new JwtService(
                "a-completely-different-signing-secret-0123456789-abcdefghij", 3_600_000L);

        String token = signedWithSecretA.generateToken(UUID.randomUUID(), "user@example.com");

        assertThat(verifiesWithSecretB.isValid(token)).isFalse();
        assertThatThrownBy(() -> verifiesWithSecretB.parseClaims(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void rejectsAMalformedToken() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);

        assertThat(jwtService.isValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void constructorRejectsABlankSecret() {
        assertThatThrownBy(() -> new JwtService("", 3_600_000L))
                .isInstanceOf(IllegalStateException.class);
    }
}
