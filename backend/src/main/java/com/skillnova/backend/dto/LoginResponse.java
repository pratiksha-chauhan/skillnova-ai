package com.skillnova.backend.dto;

import java.util.UUID;

/**
 * Returned on successful login. {@code token} is a JWT access token the
 * client must send as {@code Authorization: Bearer <token>} on subsequent
 * requests to protected endpoints (see {@code JwtAuthenticationFilter}).
 * {@code expiresInMs} lets the client know when to expect it to expire so
 * it can proactively prompt re-login.
 */
public record LoginResponse(
        UUID id,
        String email,
        String fullName,
        String token,
        long expiresInMs
) {}
