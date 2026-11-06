package com.skillnova.backend.service;

import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.dto.RoadmapGenerationRequest;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.exception.IncompleteProfileException;
import com.skillnova.backend.exception.UserNotFoundException;
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
    private AiRoadmapProvider aiRoadmapProvider;

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

    private RoadmapAiResponse stubbedAiResponse() {
        return new RoadmapAiResponse("summary", List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void generatesRoadmapUsingStoredProfileWhenRequestIsEmpty() {
        roadmapService = new RoadmapService(userRepository, aiRoadmapProvider);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(aiRoadmapProvider.generateRoadmap(any())).thenReturn(stubbedAiResponse());

        RoadmapAiResponse response = roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY);

        assertThat(response.summary()).isEqualTo("summary");

        ArgumentCaptor<RoadmapPromptContext> captor = ArgumentCaptor.forClass(RoadmapPromptContext.class);
        verify(aiRoadmapProvider).generateRoadmap(captor.capture());
        RoadmapPromptContext context = captor.getValue();
        assertThat(context.targetRole()).isEqualTo("Staff Engineer");
        assertThat(context.targetTimelineMonths()).isEqualTo(12);
        assertThat(context.currentSkills()).containsExactly("Java", "Spring Boot");
    }

    @Test
    void requestFieldsOverrideStoredProfileFields() {
        roadmapService = new RoadmapService(userRepository, aiRoadmapProvider);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(aiRoadmapProvider.generateRoadmap(any())).thenReturn(stubbedAiResponse());

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
        roadmapService = new RoadmapService(userRepository, aiRoadmapProvider);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        user.setTargetRole(null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY))
                .isInstanceOf(IncompleteProfileException.class);

        verify(aiRoadmapProvider, never()).generateRoadmap(any());
    }

    @Test
    void rejectsGenerationWhenTargetTimelineMissingFromBothRequestAndProfile() {
        roadmapService = new RoadmapService(userRepository, aiRoadmapProvider);
        UUID id = UUID.randomUUID();
        User user = fullProfileUser(id);
        user.setTargetTimelineMonths(null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY))
                .isInstanceOf(IncompleteProfileException.class);

        verify(aiRoadmapProvider, never()).generateRoadmap(any());
    }

    @Test
    void throwsUserNotFoundWhenUserDoesNotExist() {
        roadmapService = new RoadmapService(userRepository, aiRoadmapProvider);
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.generateRoadmap(id, RoadmapGenerationRequest.EMPTY))
                .isInstanceOf(UserNotFoundException.class);
    }
}
