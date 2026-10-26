package com.skillnova.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full-replace payload for {@code PUT /api/users/profile}. Every field is
 * optional except {@code fullName} (the one profile field that is NOT NULL
 * in the database); any field left {@code null} clears that attribute, and
 * any field omitted from the JSON body is treated the same as explicit
 * {@code null} (Java records can't distinguish "absent" from "null" without
 * Optional-wrapping every field, which isn't worth the complexity here) -
 * so clients should always send the full desired profile state, not a
 * partial diff.
 */
public record ProfileUpdateRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @Size(max = 150, message = "Current job role must be at most 150 characters")
        String currentJobRole,

        @DecimalMin(value = "0.0", message = "Years of experience must be at least 0")
        @DecimalMax(value = "99.9", message = "Years of experience must be at most 99.9")
        @Digits(integer = 2, fraction = 1, message = "Years of experience allows at most 1 decimal place")
        BigDecimal yearsExperience,

        @Size(max = 50, message = "At most 50 skills are allowed")
        List<@NotBlank(message = "Skill entries cannot be blank") @Size(max = 100, message = "Each skill must be at most 100 characters") String> currentSkills,

        @Size(max = 150, message = "Target role must be at most 150 characters")
        String targetRole,

        @DecimalMin(value = "0.0", message = "Daily study hours must be at least 0")
        @DecimalMax(value = "24.0", message = "Daily study hours must be at most 24")
        @Digits(integer = 2, fraction = 1, message = "Daily study hours allows at most 1 decimal place")
        BigDecimal dailyStudyHours,

        @Min(value = 1, message = "Target timeline must be at least 1 month")
        @Max(value = 120, message = "Target timeline must be at most 120 months")
        Integer targetTimelineMonths
) {}
