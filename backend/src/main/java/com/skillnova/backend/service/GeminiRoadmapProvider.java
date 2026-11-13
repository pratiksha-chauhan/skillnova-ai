package com.skillnova.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.exception.AiGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Logger log = LoggerFactory.getLogger(GeminiRoadmapProvider.class);

    // HTTP statuses worth retrying: 429 (rate limited) and 5xx server-side
    // errors (502/503/504 are typically transient capacity/overload issues).
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 500, 502, 503, 504);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final long maxBackoffMs;

    public GeminiRoadmapProvider(
            @Qualifier("geminiRestClientBuilder") RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.retry.max-attempts}") int maxAttempts,
            @Value("${gemini.retry.initial-backoff-ms}") long initialBackoffMs,
            @Value("${gemini.retry.max-backoff-ms}") long maxBackoffMs
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
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
    }

    @Override
    public RoadmapAiResponse generateRoadmap(RoadmapPromptContext context) {
        String prompt = buildPrompt(context);
        String rawBody = callGeminiWithRetry(prompt);

        GeminiGenerateContentResponse response;
        try {
            response = objectMapper.readValue(rawBody, GeminiGenerateContentResponse.class);
        } catch (JsonProcessingException e) {
            throw new AiGenerationException("Gemini response envelope was not valid JSON", e);
        }

        String text = extractText(response);
        return parseRoadmap(text);
    }

    /**
     * Calls Gemini's generateContent endpoint, retrying with exponential
     * backoff on transient failures: HTTP 429/5xx responses (rate limiting,
     * temporary overload) and I/O errors such as read timeouts. Non-transient
     * failures (e.g. 400/401/404) fail immediately without retrying.
     */
    private String callGeminiWithRetry(String prompt) {
        long backoffMs = initialBackoffMs;
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return doCallGemini(prompt);
            } catch (TransientAiGenerationException e) {
                lastFailure = e;
                if (attempt == maxAttempts) {
                    break;
                }
                log.warn("Gemini API call failed on attempt {}/{} ({}); retrying in {}ms",
                        attempt, maxAttempts, e.getMessage(), backoffMs);
                sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            }
        }

        throw new AiGenerationException(
                "Gemini API call failed after " + maxAttempts + " attempts: " + lastFailure.getMessage(),
                lastFailure);
    }

    private String doCallGemini(String prompt) {
        try {
            // Read via exchange()+raw InputStream (not retrieve().body(...)) because
            // Gemini sometimes returns Content-Type: application/octet-stream instead
            // of application/json/text, and Spring's HttpMessageConverter lookup -
            // used by both GeminiGenerateContentResponse.class and String.class -
            // rejects that content type outright. exchange() bypasses converter
            // selection entirely, so the body is read as raw bytes regardless of the
            // declared Content-Type; it's parsed manually below.
            return restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(prompt))
                    .exchange((request, response) -> {
                        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        HttpStatusCode status = response.getStatusCode();
                        if (status.isError()) {
                            String message = "Gemini API returned HTTP " + status.value() + ": " + body;
                            if (RETRYABLE_STATUS_CODES.contains(status.value())) {
                                throw new TransientAiGenerationException(message);
                            }
                            throw new AiGenerationException(message);
                        }
                        return body;
                    });
        } catch (RestClientException e) {
            // Connection resets, read timeouts, etc. are transient - the same
            // call frequently succeeds a moment later.
            throw new TransientAiGenerationException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiGenerationException("Interrupted while waiting to retry Gemini API call", e);
        }
    }

    /** Marks a failure as transient/worth retrying; never escapes this class. */
    private static final class TransientAiGenerationException extends RuntimeException {
        TransientAiGenerationException(String message) {
            super(message);
        }

        TransientAiGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
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
