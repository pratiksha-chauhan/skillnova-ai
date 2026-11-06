package com.skillnova.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.exception.AiGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * {@link AiRoadmapProvider} backed by Google's Gemini API
 * (https://ai.google.dev - free-tier key from https://aistudio.google.com/apikey).
 *
 * Uses {@code generationConfig.responseMimeType = "application/json"} plus
 * explicit prompt instructions describing the exact JSON shape to return,
 * rather than Gemini's structured {@code responseSchema} field - this keeps
 * the implementation simple and provider-agnostic (the same prompting
 * strategy works if we ever add an OpenAI provider behind the same
 * {@link AiRoadmapProvider} interface).
 */
@Service
public class GeminiRoadmapProvider implements AiRoadmapProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public GeminiRoadmapProvider(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.timeout-ms}") long timeoutMs
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "gemini.api-key must be configured (set the GEMINI_API_KEY environment variable). "
                            + "Get a free key at https://aistudio.google.com/apikey");
        }
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                // Header form keeps the key out of URLs (and therefore out of
                // access logs), unlike the alternative ?key= query param.
                .defaultHeader("x-goog-api-key", apiKey)
                .requestFactory(timeoutAwareRequestFactory(timeoutMs))
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    private static ClientHttpRequestFactory timeoutAwareRequestFactory(long timeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        return ClientHttpRequestFactories.get(settings);
    }

    @Override
    public RoadmapAiResponse generateRoadmap(RoadmapPromptContext context) {
        String prompt = buildPrompt(context);

        GeminiGenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(prompt))
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);
        } catch (RestClientException e) {
            throw new AiGenerationException("Gemini API call failed: " + e.getMessage(), e);
        }

        String text = extractText(response);
        return parseRoadmap(text);
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.4
                )
        );
    }

    private String extractText(GeminiGenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AiGenerationException("Gemini returned no candidates (possibly blocked by safety filters)");
        }
        GeminiCandidate candidate = response.candidates().get(0);
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw new AiGenerationException("Gemini candidate had no content parts");
        }
        String text = candidate.content().parts().get(0).text();
        if (text == null || text.isBlank()) {
            throw new AiGenerationException("Gemini returned an empty response body");
        }
        return stripMarkdownFences(text);
    }

    /**
     * Defensive cleanup: despite {@code responseMimeType=application/json},
     * models occasionally still wrap output in ```json ... ``` fences.
     */
    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private RoadmapAiResponse parseRoadmap(String json) {
        try {
            return objectMapper.readValue(json, RoadmapAiResponse.class);
        } catch (JsonProcessingException e) {
            throw new AiGenerationException(
                    "Gemini response was not valid JSON matching the expected roadmap shape", e);
        }
    }

    private String buildPrompt(RoadmapPromptContext context) {
        String skills = (context.currentSkills() == null || context.currentSkills().isEmpty())
                ? "none listed"
                : String.join(", ", context.currentSkills());

        return """
                You are a career roadmap planning assistant. Generate a personalized,
                month-by-month learning roadmap as STRICT JSON only - no markdown, no
                commentary, matching EXACTLY this shape:

                {
                  "summary": string,
                  "skillGaps": [
                    { "skillName": string, "currentLevel": "NONE"|"BEGINNER"|"INTERMEDIATE"|"ADVANCED"|"EXPERT",
                      "requiredLevel": "NONE"|"BEGINNER"|"INTERMEDIATE"|"ADVANCED"|"EXPERT",
                      "priority": "LOW"|"MEDIUM"|"HIGH" }
                  ],
                  "tasks": [
                    { "monthNumber": integer, "weekNumber": integer or null, "title": string,
                      "description": string, "taskType": "LEARNING"|"PROJECT"|"CERTIFICATION"|"INTERVIEW_PREP",
                      "sortOrder": integer }
                  ],
                  "certifications": [
                    { "title": string, "provider": string, "url": string or null, "priority": "LOW"|"MEDIUM"|"HIGH" }
                  ],
                  "projects": [
                    { "title": string, "description": string, "difficulty": "BEGINNER"|"INTERMEDIATE"|"ADVANCED",
                      "techStack": string }
                  ]
                }

                Candidate profile:
                - Current job role: %s
                - Years of experience: %s
                - Current skills: %s
                - Target role: %s
                - Daily study hours available: %s
                - Target timeline (months): %s

                Spread "tasks" realistically across all months of the timeline (monthNumber
                from 1 to the target timeline), ordered by sortOrder within each month.
                Return ONLY the JSON object, nothing else.
                """.formatted(
                nullToNa(context.currentJobRole()),
                formatDecimal(context.yearsExperience()),
                skills,
                context.targetRole(),
                formatDecimal(context.dailyStudyHours()),
                context.targetTimelineMonths()
        );
    }

    private static String nullToNa(String value) {
        return (value == null || value.isBlank()) ? "not specified" : value;
    }

    private static String formatDecimal(BigDecimal value) {
        return value == null ? "not specified" : value.toPlainString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiCandidate(GeminiContent content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiContent(List<GeminiPart> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiPart(@JsonProperty("text") String text) {}
}
