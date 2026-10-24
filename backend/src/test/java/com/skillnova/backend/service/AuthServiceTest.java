package com.skillnova.backend.service;

import com.skillnova.backend.dto.LoginRequest;
import com.skillnova.backend.dto.LoginResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.InvalidCredentialsException;
import com.skillnova.backend.repository.UserRepository;
import com.skillnova.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Real JwtService (not mocked) - exercises the actual token generation
    // code path with a deterministic test-only secret, so these tests also
    // double as a sanity check that AuthService wires JwtService correctly.
    private final JwtService jwtService = new JwtService(
            "test-only-signing-secret-at-least-256-bits-long-0123456789", 3_600_000L);

    private AuthService authService;

    private User existingUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("pratiksha@example.com")
                .fullName("Pratiksha Chauhan")
                .passwordHash("hashed-password")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void returnsUserDetailsAndJwtWhenCredentialsAreValid() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
        User user = existingUser();

        when(userRepository.findByEmail("pratiksha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123", "hashed-password")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("Pratiksha@Example.com", "Secret123"));

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo("pratiksha@example.com");
        assertThat(response.fullName()).isEqualTo("Pratiksha Chauhan");
        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresInMs()).isEqualTo(3_600_000L);

        // The issued token must itself be valid and carry the right subject.
        assertThat(jwtService.isValid(response.token())).isTrue();
        assertThat(jwtService.extractUserId(response.token())).isEqualTo(user.getId());
        assertThat(jwtService.extractEmail(response.token())).isEqualTo("pratiksha@example.com");
    }

    @Test
    void rejectsLoginWhenEmailIsUnknown() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsLoginWhenPasswordDoesNotMatch() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
        User user = existingUser();

        when(userRepository.findByEmail("pratiksha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("pratiksha@example.com", "WrongPassword")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
