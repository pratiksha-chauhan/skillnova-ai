package com.skillnova.backend.service;

import com.skillnova.backend.dto.LoginRequest;
import com.skillnova.backend.dto.LoginResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.InvalidCredentialsException;
import com.skillnova.backend.repository.UserRepository;
import com.skillnova.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles credential verification and JWT issuance for login. Kept separate
 * from {@link UserService} (registration/profile) because authentication is
 * a distinct concern.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                token,
                jwtService.getExpirationMs()
        );
    }
}
