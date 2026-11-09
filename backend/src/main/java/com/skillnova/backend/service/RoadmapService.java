package com.skillnova.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.dto.RoadmapGenerationRequest;
import com.skillnova.backend.dto.RoadmapResponse;
import com.skillnova.backend.dto.RoadmapSummaryResponse;
import com.skillnova.backend.entity.CertificationRecommendation;
import com.skillnova.backend.entity.Difficulty;
import com.skillnova.backend.entity.Priority;
import com.skillnova.backend.entity.ProjectRecommendation;
import com.skillnova.backend.entity.Roadmap;
import com.skillnova.backend.entity.RoadmapStatus;
import com.skillnova.backend.entity.RoadmapTask;
import com.skillnova.backend.entity.SkillGap;
import com.skillnova.backend.entity.SkillLevel;
import com.skillnova.backend.entity.TaskStatus;
import com.skillnova.backend.entity.TaskType;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.AiGenerationException;
import com.skillnova.backend.exception.IncompleteProfileException;
import com.skillnova.backend.exception.RoadmapNotFoundException;
import com.skillnova.backend.exception.TaskNotFoundException;
import com.skillnova.backend.exception.UserNotFoundException;
import com.skillnova.backend.repository.RoadmapRepository;
import com.skillnova.backend.repository.RoadmapTaskRepository;
import com.skillnova.backend.repository.UserRepository;
import com.skillnova.backend.util.SkillsCsvConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates roadmap generation and persistence:
 * <ol>
 *   <li>Merges a (possibly empty) {@link RoadmapGenerationRequest} with the
 *       authenticated user's stored profile - request fields win when
 *       present, otherwise the profile value is used.</li>
 *   <li>Validates that enough information exists to generate a meaningful
 *       roadmap.</li>
 *   <li>Delegates the AI call to {@link AiRoadmapProvider}.</li>
 *   <li>Maps the resulting {@link RoadmapAiResponse} into the
 *       {@link Roadmap} aggregate (Phase 9) and persists it, along with its
 *       raw JSON for audit/debugging.</li>
 * </ol>
 * Also exposes read paths for listing and fetching previously-generated,
 * ownership-scoped roadmaps.
 */
@Service
public class RoadmapService {

    private final UserRepository userRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final AiRoadmapProvider aiRoadmapProvider;
    private final ObjectMapper objectMapper;

    public RoadmapService(
            UserRepository userRepository,
            RoadmapRepository roadmapRepository,
            RoadmapTaskRepository roadmapTaskRepository,
            AiRoadmapProvider aiRoadmapProvider,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.roadmapRepository = roadmapRepository;
        this.roadmapTaskRepository = roadmapTaskRepository;
        this.aiRoadmapProvider = aiRoadmapProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RoadmapResponse generateRoadmap(UUID userId, RoadmapGenerationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String targetRole = firstNonBlank(request.targetRole(), user.getTargetRole());
        Integer targetTimelineMonths = request.targetTimelineMonths() != null
                ? request.targetTimelineMonths()
                : user.getTargetTimelineMonths();

        if (targetRole == null || targetRole.isBlank() || targetTimelineMonths == null) {
            throw new IncompleteProfileException(
                    "A target role and target timeline (in months) are required to generate a roadmap. "
                            + "Set them via PUT /api/users/profile, or include targetRole/targetTimelineMonths "
                            + "in this request.");
        }

        String currentJobRole = firstNonBlank(request.currentJobRole(), user.getCurrentJobRole());
        BigDecimal yearsExperience = request.yearsExperience() != null
                ? request.yearsExperience()
                : user.getYearsExperience();
        BigDecimal dailyStudyHours = request.dailyStudyHours() != null
                ? request.dailyStudyHours()
                : user.getDailyStudyHours();
        List<String> currentSkills = (request.currentSkills() != null && !request.currentSkills().isEmpty())
                ? request.currentSkills()
                : SkillsCsvConverter.toList(user.getCurrentSkills());

        RoadmapPromptContext context = new RoadmapPromptContext(
                currentJobRole,
                yearsExperience,
                currentSkills,
                targetRole,
                dailyStudyHours,
                targetTimelineMonths
        );

        RoadmapAiResponse aiResponse = aiRoadmapProvider.generateRoadmap(context);

        Roadmap roadmap = buildRoadmapEntity(user, context, aiResponse);
        Roadmap saved = roadmapRepository.save(roadmap);
        return toRoadmapResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoadmapSummaryResponse> listRoadmaps(UUID userId) {
        return roadmapRepository.findAllByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(RoadmapService::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoadmapResponse getRoadmap(UUID userId, UUID roadmapId) {
        Roadmap roadmap = roadmapRepository.findByIdAndUser_Id(roadmapId, userId)
                .orElseThrow(() -> new RoadmapNotFoundException(roadmapId));
        return toRoadmapResponse(roadmap);
    }

    /**
     * Transitions a single task's status (Phase 11). Any {@link TaskStatus}
     * value may be requested from any current status - there is no
     * enforced sequential path. {@code completedAt} is stamped when the
     * task becomes {@code COMPLETED}, and cleared when it moves away from
     * {@code COMPLETED}.
     * <p>
     * After saving the task, the parent roadmap's own status is
     * auto-reconciled against its tasks: an {@code ACTIVE} roadmap flips to
     * {@code COMPLETED} once every one of its tasks is {@code COMPLETED},
     * and a {@code COMPLETED} roadmap flips back to {@code ACTIVE} the
     * moment any task is no longer {@code COMPLETED} (e.g. the user
     * un-checks something after finishing). {@code ARCHIVED} roadmaps are
     * left untouched by task edits - archiving is a deliberate user action
     * that task status changes should not silently undo.
     */
    @Transactional
    public RoadmapResponse.RoadmapTaskResponse updateTaskStatus(UUID userId, UUID roadmapId, UUID taskId, TaskStatus newStatus) {
        RoadmapTask task = roadmapTaskRepository.findByIdAndRoadmap_IdAndRoadmap_User_Id(taskId, roadmapId, userId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setStatus(newStatus);
        task.setCompletedAt(newStatus == TaskStatus.COMPLETED ? Instant.now() : null);
        RoadmapTask saved = roadmapTaskRepository.save(task);

        reconcileRoadmapStatus(saved.getRoadmap());

        return toTaskResponse(saved);
    }

    private void reconcileRoadmapStatus(Roadmap roadmap) {
        if (roadmap.getStatus() == RoadmapStatus.ARCHIVED) {
            return;
        }

        List<RoadmapTask> tasks = roadmap.getTasks();
        boolean allCompleted = !tasks.isEmpty()
                && tasks.stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (allCompleted && roadmap.getStatus() != RoadmapStatus.COMPLETED) {
            roadmap.setStatus(RoadmapStatus.COMPLETED);
            roadmapRepository.save(roadmap);
        } else if (!allCompleted && roadmap.getStatus() == RoadmapStatus.COMPLETED) {
            roadmap.setStatus(RoadmapStatus.ACTIVE);
            roadmapRepository.save(roadmap);
        }
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    // ---- AI response -> entity mapping -------------------------------------

    private Roadmap buildRoadmapEntity(User user, RoadmapPromptContext context, RoadmapAiResponse aiResponse) {
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTargetRole(context.targetRole());
        roadmap.setCurrentJobRole(context.currentJobRole());
        roadmap.setYearsExperience(context.yearsExperience());
        roadmap.setDailyStudyHours(context.dailyStudyHours());
        roadmap.setTargetTimelineMonths(context.targetTimelineMonths());
        roadmap.setAiSummary(aiResponse.summary());
        roadmap.setRawAiResponse(serializeAiResponse(aiResponse));

        if (aiResponse.skillGaps() != null) {
            aiResponse.skillGaps().forEach(sg -> roadmap.addSkillGap(toSkillGapEntity(sg)));
        }
        if (aiResponse.tasks() != null) {
            aiResponse.tasks().forEach(t -> roadmap.addTask(toTaskEntity(t)));
        }
        if (aiResponse.certifications() != null) {
            aiResponse.certifications().forEach(c -> roadmap.addCertification(toCertificationEntity(c)));
        }
        if (aiResponse.projects() != null) {
            aiResponse.projects().forEach(p -> roadmap.addProject(toProjectEntity(p)));
        }

        return roadmap;
    }

    private String serializeAiResponse(RoadmapAiResponse aiResponse) {
        try {
            return objectMapper.writeValueAsString(aiResponse);
        } catch (JsonProcessingException e) {
            // Should never happen (we just deserialized this shape), but
            // storing the raw response is best-effort - don't fail
            // generation over it.
            return null;
        }
    }

    private static SkillGap toSkillGapEntity(RoadmapAiResponse.SkillGapAi ai) {
        SkillGap entity = new SkillGap();
        entity.setSkillName(requireNonBlank(ai.skillName(), "skillGaps[].skillName"));
        entity.setCurrentLevel(parseEnum(SkillLevel.class, ai.currentLevel(), "skillGaps[].currentLevel"));
        entity.setRequiredLevel(parseEnum(SkillLevel.class, ai.requiredLevel(), "skillGaps[].requiredLevel"));
        entity.setPriority(parseEnum(Priority.class, ai.priority(), "skillGaps[].priority"));
        return entity;
    }

    private static RoadmapTask toTaskEntity(RoadmapAiResponse.RoadmapTaskAi ai) {
        RoadmapTask entity = new RoadmapTask();
        entity.setMonthNumber(requireNonNull(ai.monthNumber(), "tasks[].monthNumber"));
        entity.setWeekNumber(ai.weekNumber());
        entity.setTitle(requireNonBlank(ai.title(), "tasks[].title"));
        entity.setDescription(ai.description());
        entity.setTaskType(parseEnum(TaskType.class, ai.taskType(), "tasks[].taskType"));
        entity.setSortOrder(ai.sortOrder() != null ? ai.sortOrder() : 0);
        return entity;
    }

    private static CertificationRecommendation toCertificationEntity(RoadmapAiResponse.CertificationAi ai) {
        CertificationRecommendation entity = new CertificationRecommendation();
        entity.setTitle(requireNonBlank(ai.title(), "certifications[].title"));
        entity.setProvider(ai.provider());
        entity.setUrl(ai.url());
        entity.setPriority(parseEnum(Priority.class, ai.priority(), "certifications[].priority"));
        return entity;
    }

    private static ProjectRecommendation toProjectEntity(RoadmapAiResponse.ProjectAi ai) {
        ProjectRecommendation entity = new ProjectRecommendation();
        entity.setTitle(requireNonBlank(ai.title(), "projects[].title"));
        entity.setDescription(ai.description());
        entity.setDifficulty(parseEnum(Difficulty.class, ai.difficulty(), "projects[].difficulty"));
        entity.setTechStack(ai.techStack());
        return entity;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AiGenerationException("Gemini response was missing a value for " + fieldName);
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AiGenerationException(
                    "Gemini returned an unrecognized value '" + value + "' for " + fieldName);
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AiGenerationException("Gemini response was missing a value for " + fieldName);
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new AiGenerationException("Gemini response was missing a value for " + fieldName);
        }
        return value;
    }

    // ---- entity -> response mapping -----------------------------------------

    private static RoadmapResponse toRoadmapResponse(Roadmap roadmap) {
        return new RoadmapResponse(
                roadmap.getId(),
                roadmap.getTargetRole(),
                roadmap.getCurrentJobRole(),
                roadmap.getYearsExperience(),
                roadmap.getDailyStudyHours(),
                roadmap.getTargetTimelineMonths(),
                roadmap.getStatus().name(),
                roadmap.getAiSummary(),
                roadmap.getCreatedAt(),
                roadmap.getUpdatedAt(),
                roadmap.getSkillGaps().stream().map(RoadmapService::toSkillGapResponse).toList(),
                roadmap.getTasks().stream().map(RoadmapService::toTaskResponse).toList(),
                roadmap.getCertifications().stream().map(RoadmapService::toCertificationResponse).toList(),
                roadmap.getProjects().stream().map(RoadmapService::toProjectResponse).toList()
        );
    }

    private static RoadmapSummaryResponse toSummaryResponse(Roadmap roadmap) {
        return new RoadmapSummaryResponse(
                roadmap.getId(),
                roadmap.getTargetRole(),
                roadmap.getStatus().name(),
                roadmap.getTargetTimelineMonths(),
                roadmap.getCreatedAt(),
                roadmap.getUpdatedAt()
        );
    }

    private static RoadmapResponse.SkillGapResponse toSkillGapResponse(SkillGap entity) {
        return new RoadmapResponse.SkillGapResponse(
                entity.getId(),
                entity.getSkillName(),
                entity.getCurrentLevel().name(),
                entity.getRequiredLevel().name(),
                entity.getPriority().name()
        );
    }

    private static RoadmapResponse.RoadmapTaskResponse toTaskResponse(RoadmapTask entity) {
        return new RoadmapResponse.RoadmapTaskResponse(
                entity.getId(),
                entity.getMonthNumber(),
                entity.getWeekNumber(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getTaskType().name(),
                entity.getStatus().name(),
                entity.getSortOrder(),
                entity.getCompletedAt()
        );
    }

    private static RoadmapResponse.CertificationResponse toCertificationResponse(CertificationRecommendation entity) {
        return new RoadmapResponse.CertificationResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getProvider(),
                entity.getUrl(),
                entity.getPriority().name()
        );
    }

    private static RoadmapResponse.ProjectResponse toProjectResponse(ProjectRecommendation entity) {
        return new RoadmapResponse.ProjectResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDifficulty().name(),
                entity.getTechStack()
        );
    }
}
