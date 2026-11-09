package com.skillnova.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight roadmap listing row, returned by {@code GET /api/roadmap}.
 * Omits the (potentially large) task/skill-gap/certification/project
 * collections and the raw AI response - use
 * {@code GET /api/roadmap/{id}} for the full {@link RoadmapResponse}.
 */
public record RoadmapSummaryResponse(
        UUID id,
        String targetRole,
        String status,
        Integer targetTimelineMonths,
        Instant createdAt,
        Instant updatedAt
) {}
