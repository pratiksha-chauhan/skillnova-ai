package com.skillnova.backend.entity;

/**
 * Lifecycle status of a {@link Roadmap}. Mirrors the
 * {@code chk_roadmaps_status} CHECK constraint in
 * {@code V1__create_core_schema.sql}.
 */
public enum RoadmapStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}
