package com.skillnova.backend.service;

import com.skillnova.backend.dto.DashboardResponse;
import com.skillnova.backend.entity.Priority;
import com.skillnova.backend.entity.Roadmap;
import com.skillnova.backend.entity.RoadmapStatus;
import com.skillnova.backend.entity.RoadmapTask;
import com.skillnova.backend.entity.TaskStatus;
import com.skillnova.backend.entity.TaskType;
import com.skillnova.backend.repository.RoadmapRepository;
import com.skillnova.backend.repository.RoadmapTaskRepository;
import com.skillnova.backend.repository.SkillGapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RoadmapRepository roadmapRepository;

    @Mock
    private RoadmapTaskRepository roadmapTaskRepository;

    @Mock
    private SkillGapRepository skillGapRepository;

    private DashboardService dashboardService;

    private Roadmap roadmap(UUID id, String targetRole, RoadmapStatus status) {
        Roadmap roadmap = new Roadmap();
        roadmap.setId(id);
        roadmap.setTargetRole(targetRole);
        roadmap.setStatus(status);
        roadmap.setCreatedAt(Instant.now());
        roadmap.setUpdatedAt(Instant.now());
        return roadmap;
    }

    private RoadmapTask task(UUID roadmapId, String title, int month, int sortOrder) {
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setRoadmap(roadmap);
        task.setTitle(title);
        task.setTaskType(TaskType.LEARNING);
        task.setStatus(TaskStatus.PENDING);
        task.setMonthNumber(month);
        task.setSortOrder(sortOrder);
        return task;
    }

    @Test
    void returnsZeroedDashboardWhenUserHasNoRoadmaps() {
        dashboardService = new DashboardService(roadmapRepository, roadmapTaskRepository, skillGapRepository);
        UUID userId = UUID.randomUUID();
        when(roadmapRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(roadmapTaskRepository.tallyStatusCountsByUser(userId)).thenReturn(List.of());
        when(roadmapTaskRepository.findTop5ByRoadmap_User_IdAndRoadmap_StatusAndStatusNotOrderByMonthNumberAscSortOrderAsc(
                eq(userId), any(), any())).thenReturn(List.of());
        when(skillGapRepository.tallyPriorityCountsByUserAndRoadmapStatus(eq(userId), any())).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(userId);

        assertThat(response.totalRoadmaps()).isZero();
        assertThat(response.activeRoadmaps()).isZero();
        assertThat(response.overallTaskStats().totalTasks()).isZero();
        assertThat(response.overallTaskStats().completionPercentage()).isZero();
        assertThat(response.roadmaps()).isEmpty();
        assertThat(response.upcomingTasks()).isEmpty();
        // Every priority is pre-seeded at 0 even with no skill gaps yet.
        assertThat(response.skillGapsByPriority()).containsEntry("HIGH", 0L).containsEntry("MEDIUM", 0L).containsEntry("LOW", 0L);
    }

    @Test
    void aggregatesTaskCountsAcrossMultipleRoadmaps() {
        dashboardService = new DashboardService(roadmapRepository, roadmapTaskRepository, skillGapRepository);
        UUID userId = UUID.randomUUID();
        UUID activeRoadmapId = UUID.randomUUID();
        UUID archivedRoadmapId = UUID.randomUUID();

        Roadmap active = roadmap(activeRoadmapId, "Staff Engineer", RoadmapStatus.ACTIVE);
        Roadmap archived = roadmap(archivedRoadmapId, "Backend Developer", RoadmapStatus.ARCHIVED);
        when(roadmapRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of(active, archived));

        // active roadmap: 2 completed, 1 in-progress, 1 pending
        // archived roadmap: 3 completed
        when(roadmapTaskRepository.tallyStatusCountsByUser(userId)).thenReturn(List.<Object[]>of(
                new Object[]{activeRoadmapId, TaskStatus.COMPLETED, 2L},
                new Object[]{activeRoadmapId, TaskStatus.IN_PROGRESS, 1L},
                new Object[]{activeRoadmapId, TaskStatus.PENDING, 1L},
                new Object[]{archivedRoadmapId, TaskStatus.COMPLETED, 3L}
        ));
        when(roadmapTaskRepository.findTop5ByRoadmap_User_IdAndRoadmap_StatusAndStatusNotOrderByMonthNumberAscSortOrderAsc(
                eq(userId), any(), any())).thenReturn(List.of());
        when(skillGapRepository.tallyPriorityCountsByUserAndRoadmapStatus(eq(userId), any())).thenReturn(List.<Object[]>of(
                new Object[]{Priority.HIGH, 3L}
        ));

        DashboardResponse response = dashboardService.getDashboard(userId);

        assertThat(response.totalRoadmaps()).isEqualTo(2);
        assertThat(response.activeRoadmaps()).isEqualTo(1);

        assertThat(response.overallTaskStats().totalTasks()).isEqualTo(7);
        assertThat(response.overallTaskStats().completedTasks()).isEqualTo(5);
        assertThat(response.overallTaskStats().inProgressTasks()).isEqualTo(1);
        assertThat(response.overallTaskStats().pendingTasks()).isEqualTo(1);
        assertThat(response.overallTaskStats().completionPercentage()).isEqualTo(71.4);

        DashboardResponse.RoadmapProgressSummary activeSummary = response.roadmaps().stream()
                .filter(r -> r.roadmapId().equals(activeRoadmapId)).findFirst().orElseThrow();
        assertThat(activeSummary.totalTasks()).isEqualTo(4);
        assertThat(activeSummary.completedTasks()).isEqualTo(2);
        assertThat(activeSummary.completionPercentage()).isEqualTo(50.0);

        DashboardResponse.RoadmapProgressSummary archivedSummary = response.roadmaps().stream()
                .filter(r -> r.roadmapId().equals(archivedRoadmapId)).findFirst().orElseThrow();
        assertThat(archivedSummary.totalTasks()).isEqualTo(3);
        assertThat(archivedSummary.completionPercentage()).isEqualTo(100.0);

        assertThat(response.skillGapsByPriority()).containsEntry("HIGH", 3L).containsEntry("MEDIUM", 0L);
    }

    @Test
    void returnsUpcomingTasksFromActiveRoadmapsInOrder() {
        dashboardService = new DashboardService(roadmapRepository, roadmapTaskRepository, skillGapRepository);
        UUID userId = UUID.randomUUID();
        UUID roadmapId = UUID.randomUUID();
        when(roadmapRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(roadmapTaskRepository.tallyStatusCountsByUser(userId)).thenReturn(List.of());
        RoadmapTask firstTask = task(roadmapId, "Learn Kubernetes", 1, 1);
        RoadmapTask secondTask = task(roadmapId, "Build a project", 1, 2);
        when(roadmapTaskRepository.findTop5ByRoadmap_User_IdAndRoadmap_StatusAndStatusNotOrderByMonthNumberAscSortOrderAsc(
                eq(userId), eq(RoadmapStatus.ACTIVE), eq(TaskStatus.COMPLETED)))
                .thenReturn(List.of(firstTask, secondTask));
        when(skillGapRepository.tallyPriorityCountsByUserAndRoadmapStatus(eq(userId), any())).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(userId);

        assertThat(response.upcomingTasks()).hasSize(2);
        assertThat(response.upcomingTasks().get(0).title()).isEqualTo("Learn Kubernetes");
        assertThat(response.upcomingTasks().get(0).roadmapId()).isEqualTo(roadmapId);
        assertThat(response.upcomingTasks().get(1).title()).isEqualTo("Build a project");
    }
}
