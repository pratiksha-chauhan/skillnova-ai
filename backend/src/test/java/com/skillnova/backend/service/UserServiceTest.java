package com.skillnova.backend.service;

import com.skillnova.backend.dto.RegisterRequest;
import com.skillnova.backend.dto.RegisterResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.EmailAlreadyExistsException;
import com.skillnova.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @Test
    void registersNewUserWithHashedPasswordAndNormalizedEmail() {
        userService = new UserService(userRepository, passwordEncoder);

        RegisterRequest request = new RegisterRequest("Pratiksha Chauhan", "Pratiksha@Example.com", "Secret123");

        when(userRepository.existsByEmail("pratiksha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            toSave.setCreatedAt(Instant.now());
            toSave.setUpdatedAt(Instant.now());
            return toSave;
        });

        RegisterResponse response = userService.register(request);

        assertThat(response.email()).isEqualTo("pratiksha@example.com");
        assertThat(response.fullName()).isEqualTo("Pratiksha Chauhan");
        assertThat(response.id()).isNotNull();
        assertThat(response.createdAt()).isNotNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        userService = new UserService(userRepository, passwordEncoder);

        RegisterRequest request = new RegisterRequest("Someone Else", "pratiksha@example.com", "Secret123");

        when(userRepository.existsByEmail("pratiksha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("pratiksha@example.com");

        verify(userRepository, never()).save(any(User.class));
    }
}
