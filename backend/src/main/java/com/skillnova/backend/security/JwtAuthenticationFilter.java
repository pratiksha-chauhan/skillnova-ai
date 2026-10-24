package com.skillnova.backend.security;

import com.skillnova.backend.entity.User;
import com.skillnova.backend.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads {@code Authorization: Bearer <token>}, validates it via
 * {@link JwtService}, and - if valid - loads the user and populates the
 * {@link SecurityContextHolder} so downstream controllers/method security
 * see an authenticated principal.
 *
 * Runs once per request, before Spring Security's authorization checks.
 * Missing/invalid/expired tokens simply leave the context unauthenticated;
 * the existing {@code anyRequest().authenticated()} rule (enforced via the
 * entry point configured in {@code SecurityConfig}) is what actually
 * produces the 401 response for protected routes.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            if (!jwtService.isValid(token)) {
                return;
            }

            UUID userId = jwtService.extractUserId(token);
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                // Token is cryptographically valid but the user no longer
                // exists (e.g. deleted account) - treat as unauthenticated.
                return;
            }

            var authenticationToken = new UsernamePasswordAuthenticationToken(
                    user.get(), null, List.of());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (JwtException | IllegalArgumentException e) {
            // Malformed/tampered token - leave context unauthenticated,
            // let the entry point return 401.
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
