package com.skillnova.backend.entity;

/**
 * Priority used by {@link SkillGap} and {@link CertificationRecommendation}.
 * Mirrors the {@code chk_skill_gaps_priority} /
 * {@code chk_certification_recommendations_priority} CHECK constraints in
 * {@code V1__create_core_schema.sql}.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH
}
