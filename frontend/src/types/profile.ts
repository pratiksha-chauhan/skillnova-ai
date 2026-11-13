/**
 * Mirrors backend `com.skillnova.backend.dto.ProfileResponse` - returned by
 * `GET /api/users/profile`, used in Phase 15 to pre-fill the roadmap
 * generation form with the user's stored profile defaults.
 */
export interface ProfileResponse {
    id: string;
    email: string;
    fullName: string;
    currentJobRole: string | null;
    yearsExperience: string | null;
    currentSkills: string[];
    targetRole: string | null;
    dailyStudyHours: string | null;
    targetTimelineMonths: number | null;
    createdAt: string;
    updatedAt: string;
}
