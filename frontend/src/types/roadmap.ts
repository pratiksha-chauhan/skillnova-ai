/**
 * Mirrors backend `com.skillnova.backend.dto.RoadmapSummaryResponse` and
 * `RoadmapResponse` - the roadmap listing row and full roadmap detail
 * shapes returned by `GET /api/roadmap` and `GET /api/roadmap/{id}`
 * respectively.
 */
export interface RoadmapSummaryResponse {
    id: string;
    targetRole: string;
    status: string;
    targetTimelineMonths: number | null;
    createdAt: string;
    updatedAt: string;
}

/** The three valid values of backend `TaskStatus` - every task can be set to any of them, in any order. */
export type TaskStatusValue = "PENDING" | "IN_PROGRESS" | "COMPLETED";

/** Mirrors `com.skillnova.backend.dto.RoadmapGenerationRequest` - every field optional. */
export interface RoadmapGenerationRequest {
    targetRole?: string;
    currentJobRole?: string;
    yearsExperience?: number;
    currentSkills?: string[];
    dailyStudyHours?: number;
    targetTimelineMonths?: number;
}

export interface RoadmapTaskResponse {
    id: string;
    monthNumber: number | null;
    weekNumber: number | null;
    title: string;
    description: string;
    taskType: string;
    status: TaskStatusValue;
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
