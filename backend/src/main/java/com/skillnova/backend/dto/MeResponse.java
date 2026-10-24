package com.skillnova.backend.dto;

import java.util.UUID;

/**
 * Minimal authenticated-identity payload returned by the protected
 * {@code GET /api/auth/me} endpoint - used to verify the JWT filter chain
 * end-to-end (valid token -> 200 with this body; missing/invalid/expired
 * token -> 401). Full profile fields are exposed in Phase 7.
 */
public record MeResponse(
        UUID id,
        String email,
        String fullName
) {}
