package com.skillnova.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.dto.RoadmapGenerationRequest;
import com.skillnova.backend.dto.RoadmapResponse;
import com.skillnova.backend.dto.RoadmapSummaryResponse;
import com.skillnova.backend.entity.Roadmap;
import com.skillnova.backend.entity.RoadmapStatus;
import com.skillnova.backend.entity.RoadmapTask;
import com.skillnova.backend.entity.TaskStatus;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.AiGenerationException;
import com.skillnova.backend.exception.IncompleteProfileException;
import com.skillnova.backend.exception.RoadmapNotFoundException;
import com.skillnova.backend.exception.TaskNotFoundException;
import com.skillnova.backend.exception.UserNotFoundException;
import com.skillnova.backend.repository.RoadmapRepository;
import com.skillnova.backend.repository.RoadmapTaskRepository;
import com.skillnova.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoadmapRepository roadmapRepository;

    @Mock
    private RoadmapTaskRepository roadmapTaskRepository;

    @Mock
    private AiRoadmapProvider aiRoadmapProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RoadmapService roadmapService;

    private User fullProfileUser(UUID id) {
        return User.builder()
                .id(id)
                .email("pratiksha@example.com")
                .fullName("Pratiksha Chauhan")
                .passwordHash("hashed")
                .currentJobRole("Backend Developer")
                .yearsExperience(new BigDecimal("2.5"))
                .currentSkills("Java,Spring Boot")
                .targetRole("Staff Engineer")
                .dailyStudyHours(new BigDecimal("2.0"))
                .targetTimelineMonths(12)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private RoadmapAiResponse fullAiResponse() {
        return new RoadmapAiResponse(
                "Become a Staff Engineer in 12 months",
                List.of(new RoadmapAiResponse.SkillGapAi("Kubernetes", "BEGINNER", "ADVANCED", "HIGH")),
                List.of(new RoadmapAiResponse.RoadmapTaskAi(1, 1, "Learn Kubernetes basics", "desc", "LEARNING", 1)),
                List.of(new RoadmapAiResponse.CertificationAi("CKA", "Linux Foundation", "https://cka.example", "MEDIUM")),
                List.of(new RoadmapAiResponse.ProjectAi("Deploy a microservice", "desc", "INTERMEDIATE", "Java, Kubernetes"))
        );
    }

    // Returns whatever Roadmap entity was passed to save(), simulating the
    // DB assigning @PrePersist defaults (id/timestamps/status) without a
    // real Hibernate session.
    private void stubSaveReturnsSameEntityWithDefaults() {
        when(roadmapRepository.save(any(Roadmap.class))).thenAnswer(invocation -> {
            Roadmap roadmap = invocation.getArgument(0);
            if (roadmap.getId() == null) {
                roadmap.setId(UUID.randomUUID());
            }
            if (roadmap.getStatus() == null) {
                roadmap.setStatus(RoadmapStatus.ACTIVE);
            }
            Instant now = Instant.now();
            roadmap.setCreatedAt(now);
            roadmap.setUpdatedAt(now);
            roadmap.getTasks().forEach(t -> {
                t.setId(UUID.randomUUID());
                t.setCreatedAt(now);
                t.setUpdatedAt(now);
            });
            roadmap.getSkillGaps().forEach(s -> {
                s.setId(UUID.randomUUID());
                s.setCreatedAt(now);
            });
            roadmap.getCertifications().forEach(c -> {
                c.setId(UUID.randomUUID());
                c.setCreatedAt(now);
            });
            roadmap.getProjects().forEach(p -> {
                p.setId(UUID.randomUUID());
                p.setCreatedAt(now);
            });
            return roadmap;
        });
    }

    @Test
    void generatesAndPersistsRoadmapUsingStoredProfileWhenRequestIsEmpty() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(aiRoadmapProvider.generateRoadmap(any())).thenReturn(fullAiResponse());
        stubSaveReturnsSameEntityWithDefaults();

        RoadmapResponse response = roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY);

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.summary()).isEqualTo("Become a Staff Engineer in 12 months");
        assertThat(response.skillGaps()).hasSize(1);
        assertThat(response.skillGaps().get(0).skillName()).isEqualTo("Kubernetes");
        assertThat(response.skillGaps().get(0).priority()).isEqualTo("HIGH");
        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().get(0).status()).isEqualTo("PENDING");
        assertThat(response.certifications()).hasSize(1);
        assertThat(response.projects()).hasSize(1);

        ArgumentCaptor<Roadmap> captor = ArgumentCaptor.forClass(Roadmap.class);
        verify(roadmapRepository).save(captor.capture());
        Roadmap persisted = captor.getValue();
        assertThat(persisted.getUser()).isEqualTo(user);
        assertThat(persisted.getTargetRole()).isEqualTo("Staff Engineer");
        assertThat(persisted.getTargetTimelineMonths()).isEqualTo(12);
        assertThat(persisted.getRawAiResponse()).contains("Kubernetes");
    }

    @Test
    void requestFieldsOverrideStoredProfileFields() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(aiRoadmapProvider.generateRoadmap(any())).thenReturn(fullAiResponse());
        stubSaveReturnsSameEntityWithDefaults();

        RoadmapGenerationRequest request = new RoadmapGenerationRequest(
                "Principal Engineer", null, null, List.of("Kubernetes"), null, 18);

        roadmapService.generateRoadmap(id, request);

        ArgumentCaptor<RoadmapPromptContext> captor = ArgumentCaptor.forClass(RoadmapPromptContext.class);
        verify(aiRoadmapProvider).generateRoadmap(captor.capture());
        RoadmapPromptContext context = captor.getValue();
        assertThat(context.targetRole()).isEqualTo("Principal Engineer");
        assertThat(context.targetTimelineMonths()).isEqualTo(18);
        assertThat(context.currentSkills()).containsExactly("Kubernetes");
        // Fields not overridden still fall back to the stored profile.
        assertThat(context.currentJobRole()).isEqualTo("Backend Developer");
    }

    @Test
    void rejectsGenerationWhenTargetRoleMissingFromBothRequestAndProfile() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        user.setTargetRole(null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY))
                .isInstanceOf(IncompleteProfileException.class);

        verify(aiRoadmapProvider, never()).generateRoadmap(any());
        verify(roadmapRepository, never()).save(any());
    }

    @Test
    void rejectsGenerationWhenTargetTimelineMissingFromBothRequestAndProfile() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        user.setTargetTimelineMonths(null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY))
                .isInstanceOf(IncompleteProfileException.class);

        verify(aiRoadmapProvider, never()).generateRoadmap(any());
        verify(roadmapRepository, never()).save(any());
    }

    @Test
    void throwsUserNotFoundWhenUserDoesNotExist() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void rejectsPersistenceWhenAiReturnsAnUnrecognizedEnumValue() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        RoadmapAiResponse badResponse = new RoadmapAiResponse(
                "summary",
                List.of(new RoadmapAiResponse.SkillGapAi("Kubernetes", "NOVICE", "ADVANCED", "HIGH")),
                List.of(),
                List.of(),
                List.of()
        );
        when(aiRoadmapProvider.generateRoadmap(any())).thenReturn(badResponse);

        assertThatThrownBy(() -> roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY))
                .isInstanceOf(AiGenerationException.class);

        verify(roadmapRepository, never()).save(any());
    }

    @Test
    void listsRoadmapsForUserNewestFirst() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID userId = UUID.randomUUID();
        User user = fullProfileUser(userId);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        roadmap.setTargetRole("Staff Engineer");
        roadmap.setStatus(RoadmapStatus.ACTIVE);
        roadmap.setTargetTimelineMonths(12);
        roadmap.setCreatedAt(Instant.now());
        roadmap.setUpdatedAt(Instant.now());
        when(roadmapRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of(roadmap));

        List<RoadmapSummaryResponse> result = roadmapService.listRoadmaps(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetRole()).isEqualTo("Staff Engineer");
        assertThat(result.get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    void getRoadmapReturnsOwnershipScopedRoadmap() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID roadmapId = UUID.randomUUID();
        User user = fullProfileUser(userId);
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setUser(user);
        roadmap.setTargetRole("Staff Engineer");
        roadmap.setStatus(RoadmapStatus.ACTIVE);
        roadmap.setCreatedAt(Instant.now());
        roadmap.setUpdatedAt(Instant.now());
        when(roadmapRepository.findByIdAndUser_Id(roadmapId, userId)).thenReturn(Optional.of(roadmap));

        RoadmapResponse response = roadmapService.getRoadmap(userId, roadmapId);

        assertThat(response.id()).isEqualTo(roadmapId);
        assertThat(response.targetRole()).isEqualTo("Staff Engineer");
    }

    @Test
    void getRoadmapThrowsNotFoundWhenMissingOrNotOwned() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID roadmapId = UUID.randomUUID();
        when(roadmapRepository.findByIdAndUser_Id(roadmapId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.getRoadmap(userId, roadmapId))
                .isInstanceOf(RoadmapNotFoundException.class);
    }

    private RoadmapTask taskWithStatus(Roadmap roadmap, TaskStatus status) {
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setRoadmap(roadmap);
        task.setMonthNumber(1);
        task.setTitle("Learn Kubernetes basics");
        task.setTaskType(com.skillnova.backend.entity.TaskType.LEARNING);
        task.setStatus(status);
        task.setSortOrder(1);
        roadmap.getTasks().add(task);
        return task;
    }

    @Test
    void updateTaskStatusThrowsNotFoundWhenMissingOrNotOwned() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID roadmapId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(roadmapTaskRepository.findByIdAndRoadmap_IdAndRoadmap_User_Id(taskId, roadmapId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.updateTaskStatus(userId, roadmapId, taskId, TaskStatus.IN_PROGRESS))
                .isInstanceOf(TaskNotFoundException.class);

        verify(roadmapTaskRepository, never()).save(any());
    }

    @Test
    void updateTaskStatusStampsCompletedAtAndAutoCompletesRoadmapWhenAllTasksDone() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setStatus(RoadmapStatus.ACTIVE);
        RoadmapTask alreadyDone = taskWithStatus(roadmap, TaskStatus.COMPLETED);
        RoadmapTask target = taskWithStatus(roadmap, TaskStatus.IN_PROGRESS);
        when(roadmapTaskRepository.findByIdAndRoadmap_IdAndRoadmap_User_Id(target.getId(), roadmapId, userId))
                .thenReturn(Optional.of(target));
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoadmapResponse.RoadmapTaskResponse response =
                roadmapService.updateTaskStatus(userId, roadmapId, target.getId(), TaskStatus.COMPLETED);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedAt()).isNotNull();
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.COMPLETED);
        verify(roadmapRepository).save(roadmap);
        assertThat(alreadyDone.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void updateTaskStatusClearsCompletedAtAndRevertsRoadmapToActiveWhenUncompleted() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setStatus(RoadmapStatus.COMPLETED);
        RoadmapTask target = taskWithStatus(roadmap, TaskStatus.COMPLETED);
        target.setCompletedAt(Instant.now());
        when(roadmapTaskRepository.findByIdAndRoadmap_IdAndRoadmap_User_Id(target.getId(), roadmapId, userId))
                .thenReturn(Optional.of(target));
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoadmapResponse.RoadmapTaskResponse response =
                roadmapService.updateTaskStatus(userId, roadmapId, target.getId(), TaskStatus.IN_PROGRESS);

        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.completedAt()).isNull();
        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.ACTIVE);
        verify(roadmapRepository).save(roadmap);
    }

    @Test
    void updateTaskStatusLeavesArchivedRoadmapStatusUntouched() {
        roadmapService = new RoadmapService(userRepository, roadmapRepository, roadmapTaskRepository, aiRoadmapProvider, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID roadmapId = UUID.randomUUID();
        Roadmap roadmap = new Roadmap();
        roadmap.setId(roadmapId);
        roadmap.setStatus(RoadmapStatus.ARCHIVED);
        RoadmapTask target = taskWithStatus(roadmap, TaskStatus.PENDING);
        when(roadmapTaskRepository.findByIdAndRoadmap_IdAndRoadmap_User_Id(target.getId(), roadmapId, userId))
                .thenReturn(Optional.of(target));
        when(roadmapTaskRepository.save(any(RoadmapTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roadmapService.updateTaskStatus(userId, roadmapId, target.getId(), TaskStatus.COMPLETED);

        assertThat(roadmap.getStatus()).isEqualTo(RoadmapStatus.ARCHIVED);
        verify(roadmapRepository, never()).save(any());
    }
}
