package com.skillnova.backend.controller;

import com.skillnova.backend.dto.LoginRequest;
import com.skillnova.backend.dto.LoginResponse;
import com.skillnova.backend.dto.MeResponse;
import com.skillnova.backend.dto.RegisterRequest;
import com.skillnova.backend.dto.RegisterResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.service.AuthService;
import com.skillnova.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Protected route used to verify the JWT filter chain end-to-end: the
     * {@code User} principal is populated by {@code JwtAuthenticationFilter}
     * from a validated token's subject claim. Requires
     * {@code Authorization: Bearer <token>}; returns 401 (via
     * {@code JwtAuthenticationEntryPoint}) if missing/invalid/expired.
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new MeResponse(user.getId(), user.getEmail(), user.getFullName()));
    }
}
