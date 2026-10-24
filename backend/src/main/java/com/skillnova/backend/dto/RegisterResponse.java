package com.skillnova.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public-facing representation of a newly registered user. Deliberately
 * excludes the password hash; entities are never returned directly from
 * controllers.
 */
public record RegisterResponse(
        UUID id,
        String email,
        String fullName,
        Instant createdAt
) {}
