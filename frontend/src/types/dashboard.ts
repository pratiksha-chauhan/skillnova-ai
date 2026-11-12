/**
 * Mirrors backend `com.skillnova.backend.dto.DashboardResponse` - the
 * aggregated overview returned by `GET /api/dashboard`.
 */
export interface DashboardResponse {
    totalRoadmaps: number;
    activeRoadmaps: number;
    overallTaskStats: DashboardTaskStats;
    roadmaps: DashboardRoadmapProgressSummary[];
    upcomingTasks: DashboardUpcomingTaskSummary[];
    skillGapsByPriority: Record<string, number>;
}

export interface DashboardTaskStats {
    totalTasks: number;
    completedTasks: number;
    inProgressTasks: number;
    pendingTasks: number;
    /** 0.0-100.0, rounded to 1 decimal place. */
    completionPercentage: number;
}

export interface DashboardRoadmapProgressSummary {
    roadmapId: string;
    targetRole: string;
    status: string;
    totalTasks: number;
    completedTasks: number;
    completionPercentage: number;
}

export interface DashboardUpcomingTaskSummary {
    taskId: string;
    roadmapId: string;
    title: string;
    taskType: string;
    monthNumber: number | null;
    weekNumber: number | null;
}
