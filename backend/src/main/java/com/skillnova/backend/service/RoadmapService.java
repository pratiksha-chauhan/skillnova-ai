package com.skillnova.backend.service;

import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.dto.RoadmapGenerationRequest;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.IncompleteProfileException;
import com.skillnova.backend.exception.UserNotFoundException;
import com.skillnova.backend.repository.UserRepository;
import com.skillnova.backend.util.SkillsCsvConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates roadmap generation: merges a (possibly empty)
 * {@link RoadmapGenerationRequest} with the authenticated user's stored
 * profile - request fields win when present, otherwise the profile value is
 * used - validates that enough information exists to generate a meaningful
 * roadmap, then delegates the actual AI call to {@link AiRoadmapProvider}.
 *
 * Deliberately does not persist the result; Phase 9 adds the
 * roadmap/roadmap_tasks/skill_gaps/... persistence on top of this preview.
 */
@Service
public class RoadmapService {

    private final UserRepository userRepository;
    private final AiRoadmapProvider aiRoadmapProvider;

    public RoadmapService(UserRepository userRepository, AiRoadmapProvider aiRoadmapProvider) {
        this.userRepository = userRepository;
        this.aiRoadmapProvider = aiRoadmapProvider;
    }

    @Transactional(readOnly = true)
    public RoadmapAiResponse generateRoadmap(UUID userId, RoadmapGenerationRequest request) {
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

        return aiRoadmapProvider.generateRoadmap(context);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
