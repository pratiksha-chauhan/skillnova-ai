package com.skillnova.backend.repository;

import com.skillnova.backend.entity.RoadmapStatus;
import com.skillnova.backend.entity.RoadmapTask;
import com.skillnova.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoadmapTaskRepository extends JpaRepository<RoadmapTask, UUID> {

    /**
     * Ownership-scoped single-task lookup for the Phase 11 status-update
     * endpoint: returns empty (not just the task) when the task exists but
     * belongs to a different roadmap or a different user's roadmap, so
     * callers can return 404 without leaking whether the ID exists at all -
     * same pattern as {@code RoadmapRepository.findByIdAndUser_Id}.
     */
    Optional<RoadmapTask> findByIdAndRoadmap_IdAndRoadmap_User_Id(UUID id, UUID roadmapId, UUID userId);

    /**
     * Per-roadmap, per-status task counts for every roadmap owned by the
     * given user, in one query (used to build each
     * {@code DashboardResponse.RoadmapProgressSummary} without N+1 queries).
     * Each row is {@code [UUID roadmapId, TaskStatus status, Long count]}.
     */
    @Query("""
            select t.roadmap.id, t.status, count(t)
            from RoadmapTask t
            where t.roadmap.user.id = :userId
            group by t.roadmap.id, t.status
            """)
    List<Object[]> tallyStatusCountsByUser(@Param("userId") UUID userId);

    /**
     * Soonest not-yet-completed tasks across the user's ACTIVE roadmaps,
     * ordered by month/sort order - used for the dashboard's
     * "upcoming tasks" card.
     */
    List<RoadmapTask> findTop5ByRoadmap_User_IdAndRoadmap_StatusAndStatusNotOrderByMonthNumberAscSortOrderAsc(
            UUID userId, RoadmapStatus roadmapStatus, TaskStatus excludedStatus);
}
