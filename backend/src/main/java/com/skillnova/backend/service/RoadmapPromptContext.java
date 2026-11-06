package com.skillnova.backend.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Normalized input to an {@link AiRoadmapProvider}, after
 * {@link RoadmapService} has merged a {@code RoadmapGenerationRequest} with
 * the user's stored profile and validated the required fields are present.
 * Decoupled from both the HTTP request DTO and the {@code User} entity so
 * the provider implementation has no knowledge of either.
 */
public record RoadmapPromptContext(
        String currentJobRole,
        BigDecimal yearsExperience,
        List<String> currentSkills,
        String targetRole,
        BigDecimal dailyStudyHours,
        Integer targetTimelineMonths
) {}
