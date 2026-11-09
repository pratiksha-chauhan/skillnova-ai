package com.skillnova.backend.service;

import com.skillnova.backend.dto.DashboardResponse;
import com.skillnova.backend.entity.Priority;
import com.skillnova.backend.entity.Roadmap;
import com.skillnova.backend.entity.RoadmapStatus;
import com.skillnova.backend.entity.RoadmapTask;
import com.skillnova.backend.entity.TaskStatus;
import com.skillnova.backend.repository.RoadmapRepository;
import com.skillnova.backend.repository.RoadmapTaskRepository;
import com.skillnova.backend.repository.SkillGapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the aggregated dashboard overview for a user: overall task
 * progress, a per-roadmap breakdown, the soonest incomplete tasks across
 * active roadmaps, and a skill-gap-by-priority tally. All three source
 * queries ({@link RoadmapRepository}, {@link RoadmapTaskRepository},
 * {@link SkillGapRepository}) run once each per request - no N+1 queries
 * regardless of how many roadmaps/tasks the user has.
 */
@Service
public class DashboardService {

    private final RoadmapRepository roadmapRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final SkillGapRepository skillGapRepository;

    public DashboardService(
            RoadmapRepository roadmapRepository,
            RoadmapTaskRepository roadmapTaskRepository,
            SkillGapRepository skillGapRepository
    ) {
        this.roadmapRepository = roadmapRepository;
        this.roadmapTaskRepository = roadmapTaskRepository;
        this.skillGapRepository = skillGapRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID userId) {
        List<Roadmap> roadmaps = roadmapRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);

        Map<UUID, Map<TaskStatus, Long>> tallyByRoadmap = buildTaskTallyByRoadmap(userId);

        List<DashboardResponse.RoadmapProgressSummary> roadmapSummaries = roadmaps.stream()
                .map(roadmap -> toProgressSummary(roadmap, tallyByRoadmap.getOrDefault(roadmap.getId(), Map.of())))
                .toList();

        DashboardResponse.TaskStats overallTaskStats = aggregateOverallStats(tallyByRoadmap);

        int activeRoadmapCount = (int) roadmaps.stream()
                .filter(r -> r.getStatus() == RoadmapStatus.ACTIVE)
                .count();

        List<DashboardResponse.UpcomingTaskSummary> upcomingTasks = roadmapTaskRepository
                .findTop5ByRoadmap_User_IdAndRoadmap_StatusAndStatusNotOrderByMonthNumberAscSortOrderAsc(
                        userId, RoadmapStatus.ACTIVE, TaskStatus.COMPLETED)
                .stream()
                .map(DashboardService::toUpcomingTaskSummary)
                .toList();

        Map<String, Long> skillGapsByPriority = buildSkillGapTally(userId);

        return new DashboardResponse(
                roadmaps.size(),
                activeRoadmapCount,
                overallTaskStats,
                roadmapSummaries,
                upcomingTasks,
                skillGapsByPriority
        );
    }

    private Map<UUID, Map<TaskStatus, Long>> buildTaskTallyByRoadmap(UUID userId) {
        Map<UUID, Map<TaskStatus, Long>> tally = new LinkedHashMap<>();
        for (Object[] row : roadmapTaskRepository.tallyStatusCountsByUser(userId)) {
            UUID roadmapId = (UUID) row[0];
            TaskStatus status = (TaskStatus) row[1];
            Long count = (Long) row[2];
            tally.computeIfAbsent(roadmapId, id -> new EnumMap<>(TaskStatus.class)).put(status, count);
        }
        return tally;
    }

    private Map<String, Long> buildSkillGapTally(UUID userId) {
        // Pre-seed every priority at 0 so the response always has a
        // consistent shape for the frontend, even with no gaps yet.
        Map<String, Long> tally = new LinkedHashMap<>();
        for (Priority priority : Priority.values()) {
            tally.put(priority.name(), 0L);
        }
        for (Object[] row : skillGapRepository.tallyPriorityCountsByUserAndRoadmapStatus(userId, RoadmapStatus.ACTIVE)) {
            Priority priority = (Priority) row[0];
            Long count = (Long) row[1];
            tally.put(priority.name(), count);
        }
        return tally;
    }

    private static DashboardResponse.RoadmapProgressSummary toProgressSummary(
            Roadmap roadmap, Map<TaskStatus, Long> statusCounts) {
        long completed = statusCounts.getOrDefault(TaskStatus.COMPLETED, 0L);
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        return new DashboardResponse.RoadmapProgressSummary(
                roadmap.getId(),
                roadmap.getTargetRole(),
                roadmap.getStatus().name(),
                total,
                completed,
                completionPercentage(completed, total)
        );
    }

    /** Sums per-status counts straight from the full tally (every roadmap, every status) in one pass. */
    private static DashboardResponse.TaskStats aggregateOverallStats(Map<UUID, Map<TaskStatus, Long>> tallyByRoadmap) {
        long total = 0;
        long completed = 0;
        long inProgress = 0;
        long pending = 0;
        for (Map<TaskStatus, Long> statusCounts : tallyByRoadmap.values()) {
            for (Map.Entry<TaskStatus, Long> entry : statusCounts.entrySet()) {
                long count = entry.getValue();
                total += count;
                switch (entry.getKey()) {
                    case COMPLETED -> completed += count;
                    case IN_PROGRESS -> inProgress += count;
                    case PENDING -> pending += count;
                }
            }
        }
        return new DashboardResponse.TaskStats(total, completed, inProgress, pending, completionPercentage(completed, total));
    }

    private static double completionPercentage(long completed, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((completed * 1000.0) / total) / 10.0;
    }

    private static DashboardResponse.UpcomingTaskSummary toUpcomingTaskSummary(RoadmapTask task) {
        return new DashboardResponse.UpcomingTaskSummary(
                task.getId(),
                task.getRoadmap().getId(),
                task.getTitle(),
                task.getTaskType().name(),
                task.getMonthNumber(),
                task.getWeekNumber()
        );
    }
}
