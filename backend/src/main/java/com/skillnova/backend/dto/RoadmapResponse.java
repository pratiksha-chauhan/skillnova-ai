package com.skillnova.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full persisted representation of a roadmap, returned by
 * {@code POST /api/roadmap/generate} (201) and {@code GET /api/roadmap/{id}}
 * (200). Unlike {@link RoadmapAiResponse} (the raw AI preview shape), every
 * row carries its own persisted {@code id} and the roadmap carries its
 * {@code status}, so clients can reference/update individual tasks in later
 * phases (Dashboard, Progress Tracking).
 */
public record RoadmapResponse(
        UUID id,
        String targetRole,
        String currentJobRole,
        BigDecimal yearsExperience,
        BigDecimal dailyStudyHours,
        Integer targetTimelineMonths,
        String status,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        List<SkillGapResponse> skillGaps,
        List<RoadmapTaskResponse> tasks,
        List<CertificationResponse> certifications,
        List<ProjectResponse> projects
) {

    public record SkillGapResponse(
            UUID id,
            String skillName,
            String currentLevel,
            String requiredLevel,
            String priority
    ) {}

    public record RoadmapTaskResponse(
            UUID id,
            Integer monthNumber,
            Integer weekNumber,
            String title,
            String description,
            String taskType,
            String status,
            Integer sortOrder,
            Instant completedAt
    ) {}

    public record CertificationResponse(
            UUID id,
            String title,
            String provider,
            String url,
            String priority
    ) {}

    public record ProjectResponse(
            UUID id,
            String title,
            String description,
            String difficulty,
            String techStack
    ) {}
}
