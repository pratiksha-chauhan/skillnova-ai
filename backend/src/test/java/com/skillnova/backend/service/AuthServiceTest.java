package com.skillnova.backend.service;

import com.skillnova.backend.dto.LoginRequest;
import com.skillnova.backend.dto.LoginResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.InvalidCredentialsException;
import com.skillnova.backend.repository.UserRepository;
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
    void returnsUserDetailsWhenCredentialsAreValid() {
        authService = new AuthService(userRepository, passwordEncoder);
        User user = existingUser();

        when(userRepository.findByEmail("pratiksha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123", "hashed-password")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("Pratiksha@Example.com", "Secret123"));

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo("pratiksha@example.com");
        assertThat(response.fullName()).isEqualTo("Pratiksha Chauhan");
    }

    @Test
    void rejectsLoginWhenEmailIsUnknown() {
        authService = new AuthService(userRepository, passwordEncoder);

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsLoginWhenPasswordDoesNotMatch() {
        authService = new AuthService(userRepository, passwordEncoder);
        User user = existingUser();

        when(userRepository.findByEmail("pratiksha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("pratiksha@example.com", "WrongPassword")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
