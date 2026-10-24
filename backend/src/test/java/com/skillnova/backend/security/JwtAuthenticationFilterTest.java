package com.skillnova.backend.security;

import com.skillnova.backend.entity.User;
import com.skillnova.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit-level verification of {@link JwtAuthenticationFilter} behavior in
 * isolation (no Spring context / DB needed) - mirrors the project's existing
 * Mockito-based testing style. Full end-to-end verification against the
 * live filter chain + real Postgres (missing token -> 401, valid token ->
 * 200, tampered/expired token -> 401) is done via curl per the Phase 6
 * manual verification checklist.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-only-signing-secret-at-least-256-bits-long-0123456789";

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User existingUser(UUID id) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void leavesContextUnauthenticatedWhenNoAuthorizationHeaderPresent() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void leavesContextUnauthenticatedWhenTokenIsInvalid() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);

        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-real-jwt");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void leavesContextUnauthenticatedWhenTokenIsExpired() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 1L);
        String token = jwtService.generateToken(UUID.randomUUID(), "user@example.com");
        Thread.sleep(25);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticatesWhenTokenIsValidAndUserExists() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "user@example.com");

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser(userId)));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(((User) auth.getPrincipal()).getId()).isEqualTo(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void leavesContextUnauthenticatedWhenTokenValidButUserNoLongerExists() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "user@example.com");

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
