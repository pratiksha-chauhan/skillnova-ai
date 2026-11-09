package com.skillnova.backend.entity;

/**
 * Completion status of a {@link RoadmapTask}. Mirrors the
 * {@code chk_roadmap_tasks_status} CHECK constraint in
 * {@code V1__create_core_schema.sql}. All tasks are created {@code PENDING}
 * by {@link com.skillnova.backend.service.RoadmapService} at generation
 * time; transitions to {@code IN_PROGRESS}/{@code COMPLETED} are driven by
 * Phase 11 (Progress Tracking).
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
