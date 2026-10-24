package com.skillnova.backend.dto;

/**
 * Minimal registration request shape used to keep the existing
 * /api/auth/register route compiling against the Phase 3 User entity.
 * Validation constraints, password hashing, and duplicate-email handling
 * are implemented in Phase 4 (Registration API).
 */
public record RegisterRequest(
        String fullName,
        String email,
        String password
) {}
