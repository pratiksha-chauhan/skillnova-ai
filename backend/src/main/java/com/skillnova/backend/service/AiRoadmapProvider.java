package com.skillnova.backend.service;

import com.skillnova.backend.dto.RoadmapAiResponse;

/**
 * Abstraction over whichever AI provider generates roadmap content. Keeping
 * this interface separate from the concrete {@code GeminiRoadmapProvider}
 * means swapping providers (e.g. to OpenAI) later is an isolated change -
 * {@link RoadmapService} and the controller never depend on provider
 * specifics.
 */
public interface AiRoadmapProvider {

    /**
     * @throws com.skillnova.backend.exception.AiGenerationException if the
     *         provider call fails or returns a response that cannot be
     *         parsed into a {@link RoadmapAiResponse}
     */
    RoadmapAiResponse generateRoadmap(RoadmapPromptContext context);
}
