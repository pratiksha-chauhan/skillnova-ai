/**
 * Mirrors backend `com.skillnova.backend.dto.RoadmapSummaryResponse` and
 * `RoadmapResponse` - the roadmap listing row and full roadmap detail
 * shapes returned by `GET /api/roadmap` and `GET /api/roadmap/{id}`
 * respectively. The full Roadmap UI (cards, task checklists, skill gaps,
 * certs/projects) is built out in Phase 15; this phase only needs enough
 * of the shape to back a thin placeholder detail page.
 */
export interface RoadmapSummaryResponse {
    id: string;
    targetRole: string;
    status: string;
    targetTimelineMonths: number | null;
    createdAt: string;
    updatedAt: string;
}

export interface RoadmapTaskResponse {
    id: string;
    monthNumber: number | null;
    weekNumber: number | null;
    title: string;
    description: string;
    taskType: string;
    status: string;
    sortOrder: number | null;
    completedAt: string | null;
}

export interface SkillGapResponse {
    id: string;
    skillName: string;
    currentLevel: string;
    requiredLevel: string;
    priority: string;
}

export interface CertificationResponse {
    id: string;
    title: string;
    provider: string;
    url: string | null;
    priority: string;
}

export interface ProjectResponse {
    id: string;
    title: string;
    description: string;
    difficulty: string;
    techStack: string;
}

export interface RoadmapResponse {
    id: string;
    targetRole: string;
    currentJobRole: string | null;
    yearsExperience: string | null;
    dailyStudyHours: string | null;
    targetTimelineMonths: number | null;
    status: string;
    summary: string;
    createdAt: string;
    updatedAt: string;
    skillGaps: SkillGapResponse[];
    tasks: RoadmapTaskResponse[];
    certifications: CertificationResponse[];
    projects: ProjectResponse[];
}
