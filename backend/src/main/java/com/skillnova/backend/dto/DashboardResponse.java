package com.skillnova.backend.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregated overview for the authenticated user's dashboard, returned by
 * {@code GET /api/dashboard}. Combines data across all of the user's
 * roadmaps so the frontend (Phase 14) doesn't have to fan out to
 * {@code GET /api/roadmap} + per-roadmap detail calls just to render a
 * summary screen.
 */
public record DashboardResponse(
        int totalRoadmaps,
        int activeRoadmaps,
        TaskStats overallTaskStats,
        List<RoadmapProgressSummary> roadmaps,
        List<UpcomingTaskSummary> upcomingTasks,
        Map<String, Long> skillGapsByPriority
) {

    /**
     * @param completionPercentage 0.0-100.0, rounded to 1 decimal place;
     *                              0.0 when {@code totalTasks} is 0 (never
     *                              divides by zero).
     */
    public record TaskStats(
            long totalTasks,
            long completedTasks,
            long inProgressTasks,
            long pendingTasks,
            double completionPercentage
    ) {}

    /** Per-roadmap progress, across every roadmap the user has ever generated (not just ACTIVE ones). */
    public record RoadmapProgressSummary(
            UUID roadmapId,
            String targetRole,
            String status,
            long totalTasks,
            long completedTasks,
            double completionPercentage
    ) {}

    /** A not-yet-completed task from one of the user's ACTIVE roadmaps, soonest first. */
    public record UpcomingTaskSummary(
            UUID taskId,
            UUID roadmapId,
            String title,
            String taskType,
            Integer monthNumber,
            Integer weekNumber
    ) {}
}
