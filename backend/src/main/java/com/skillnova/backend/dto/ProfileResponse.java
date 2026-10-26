package com.skillnova.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full profile representation of the authenticated user, returned by
 * {@code GET /api/users/profile} and after a successful
 * {@code PUT /api/users/profile}.
 */
public record ProfileResponse(
        UUID id,
        String email,
        String fullName,
        String currentJobRole,
        BigDecimal yearsExperience,
        List<String> currentSkills,
        String targetRole,
        BigDecimal dailyStudyHours,
        Integer targetTimelineMonths,
        Instant createdAt,
        Instant updatedAt
) {}
