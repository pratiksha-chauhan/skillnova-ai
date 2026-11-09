package com.skillnova.backend.entity;

/**
 * Difficulty of a {@link ProjectRecommendation}. Mirrors the
 * {@code chk_project_recommendations_difficulty} CHECK constraint in
 * {@code V1__create_core_schema.sql}.
 */
public enum Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
