package com.skillnova.backend.service;

import com.skillnova.backend.dto.ProfileResponse;
import com.skillnova.backend.dto.ProfileUpdateRequest;
import com.skillnova.backend.dto.RegisterRequest;
import com.skillnova.backend.dto.RegisterResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.EmailAlreadyExistsException;
import com.skillnova.backend.exception.UserNotFoundException;
import com.skillnova.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    private User fullUser(UUID id) {
        return User.builder()
                .id(id)
                .email("pratiksha@example.com")
                .fullName("Pratiksha Chauhan")
                .passwordHash("hashed-password")
                .currentJobRole("Backend Developer")
                .yearsExperience(new BigDecimal("2.5"))
                .currentSkills("Java,Spring Boot,PostgreSQL")
                .targetRole("Staff Engineer")
                .dailyStudyHours(new BigDecimal("2.0"))
                .targetTimelineMonths(12)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void returnsFullProfileForExistingUser() {
        userService = new UserService(userRepository, passwordEncoder);
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(fullUser(id)));

        ProfileResponse response = userService.getProfile(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.currentJobRole()).isEqualTo("Backend Developer");
        assertThat(response.currentSkills()).containsExactly("Java", "Spring Boot", "PostgreSQL");
        assertThat(response.targetTimelineMonths()).isEqualTo(12);
    }

    @Test
    void getProfileThrowsWhenUserNoLongerExists() {
        userService = new UserService(userRepository, passwordEncoder);
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfileReplacesAllFieldsAndConvertsSkillsListToCsv() {
        userService = new UserService(userRepository, passwordEncoder);
        UUID id = UUID.randomUUID();
        User existing = fullUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Pratiksha C.",
                "Senior Backend Developer",
                new BigDecimal("3.0"),
                List.of("Java", "Kubernetes"),
                "Principal Engineer",
                new BigDecimal("1.5"),
                18
        );

        ProfileResponse response = userService.updateProfile(id, request);

        assertThat(response.fullName()).isEqualTo("Pratiksha C.");
        assertThat(response.currentJobRole()).isEqualTo("Senior Backend Developer");
        assertThat(response.currentSkills()).containsExactly("Java", "Kubernetes");
        assertThat(response.targetRole()).isEqualTo("Principal Engineer");
        assertThat(response.targetTimelineMonths()).isEqualTo(18);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentSkills()).isEqualTo("Java,Kubernetes");
    }

    @Test
    void updateProfileClearsOptionalFieldsWhenOmitted() {
        userService = new UserService(userRepository, passwordEncoder);
        UUID id = UUID.randomUUID();
        User existing = fullUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Pratiksha Chauhan", null, null, null, null, null, null);

        ProfileResponse response = userService.updateProfile(id, request);

        assertThat(response.currentJobRole()).isNull();
        assertThat(response.yearsExperience()).isNull();
        assertThat(response.currentSkills()).isEmpty();
        assertThat(response.targetRole()).isNull();
        assertThat(response.dailyStudyHours()).isNull();
        assertThat(response.targetTimelineMonths()).isNull();
    }

    @Test
    void updateProfileThrowsWhenUserNoLongerExists() {
        userService = new UserService(userRepository, passwordEncoder);
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Someone", null, null, null, null, null, null);

        assertThatThrownBy(() -> userService.updateProfile(id, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}
