package com.skillnova.backend.entity;

/**
 * Proficiency level used by {@link SkillGap#getCurrentLevel()} and
 * {@link SkillGap#getRequiredLevel()}. Mirrors the
 * {@code chk_skill_gaps_current_level} / {@code chk_skill_gaps_required_level}
 * CHECK constraints in {@code V1__create_core_schema.sql}, and the same
 * upper-case strings the AI prompt in {@code GeminiRoadmapProvider} asks
 * Gemini to return.
 */
public enum SkillLevel {
    NONE,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}
