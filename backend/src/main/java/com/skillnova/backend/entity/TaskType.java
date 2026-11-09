package com.skillnova.backend.entity;

/**
 * Category of a {@link RoadmapTask}. Mirrors the
 * {@code chk_roadmap_tasks_type} CHECK constraint in
 * {@code V1__create_core_schema.sql}.
 */
public enum TaskType {
    LEARNING,
    PROJECT,
    CERTIFICATION,
    INTERVIEW_PREP
}
