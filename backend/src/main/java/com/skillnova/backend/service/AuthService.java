package com.skillnova.backend.service;

import com.skillnova.backend.dto.LoginRequest;
import com.skillnova.backend.dto.LoginResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.InvalidCredentialsException;
import com.skillnova.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles credential verification for login. Kept separate from
 * {@link UserService} (registration/profile) because authentication is a
 * distinct concern that will grow further in Phase 6 (JWT issuance).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
