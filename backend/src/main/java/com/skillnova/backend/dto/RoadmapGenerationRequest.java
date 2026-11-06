package com.skillnova.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Optional per-request overrides for {@code POST /api/roadmap/generate}.
 * Every field is optional: any field left {@code null} (or the whole body
 * omitted) falls back to the authenticated user's stored profile
 * ({@code PUT /api/users/profile}). {@code targetRole} and
 * {@code targetTimelineMonths} must be resolvable from request-or-profile
 * or generation is rejected with 422 (see {@code RoadmapService}).
 */
public record RoadmapGenerationRequest(

        @Size(max = 150, message = "Target role must be at most 150 characters")
        String targetRole,

        @Size(max = 150, message = "Current job role must be at most 150 characters")
        String currentJobRole,

        @DecimalMin(value = "0.0", message = "Years of experience must be at least 0")
        @DecimalMax(value = "99.9", message = "Years of experience must be at most 99.9")
        BigDecimal yearsExperience,

        @Size(max = 50, message = "At most 50 skills are allowed")
        List<@NotBlank(message = "Skill entries cannot be blank") @Size(max = 100, message = "Each skill must be at most 100 characters") String> currentSkills,

        @DecimalMin(value = "0.0", message = "Daily study hours must be at least 0")
        @DecimalMax(value = "24.0", message = "Daily study hours must be at most 24")
        BigDecimal dailyStudyHours,

        @Min(value = 1, message = "Target timeline must be at least 1 month")
        @Max(value = 120, message = "Target timeline must be at most 120 months")
        Integer targetTimelineMonths
) {

    /** Used when the client sends no request body at all. */
    public static final RoadmapGenerationRequest EMPTY =
            new RoadmapGenerationRequest(null, null, null, null, null, null);
}
