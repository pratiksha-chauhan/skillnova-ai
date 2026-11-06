package com.skillnova.backend.dto;

import java.util.List;

/**
 * Structured AI-generated roadmap preview returned by
 * {@code POST /api/roadmap/generate}. Not persisted yet - Phase 9 takes this
 * exact shape and writes it into the {@code roadmaps}/{@code roadmap_tasks}/
 * {@code skill_gaps}/{@code certification_recommendations}/
 * {@code project_recommendations} tables, which is why every enum-like
 * field here (level, priority, taskType, difficulty) uses the same
 * upper-case string values as the corresponding Postgres CHECK constraints.
 */
public record RoadmapAiResponse(
        String summary,
        List<SkillGapAi> skillGaps,
        List<RoadmapTaskAi> tasks,
        List<CertificationAi> certifications,
        List<ProjectAi> projects
) {

    public record SkillGapAi(
            String skillName,
            String currentLevel,
            String requiredLevel,
            String priority
    ) {}

    public record RoadmapTaskAi(
            Integer monthNumber,
            Integer weekNumber,
            String title,
            String description,
            String taskType,
            Integer sortOrder
    ) {}

    public record CertificationAi(
            String title,
            String provider,
            String url,
            String priority
    ) {}

    public record ProjectAi(
            String title,
            String description,
            String difficulty,
            String techStack
    ) {}
}
