package com.skillnova.backend.exception;

/**
 * Thrown when the configured AI provider (Gemini) fails to produce a usable
 * roadmap - network/timeout failure, a non-success HTTP status, or a
 * response body that doesn't parse into {@code RoadmapAiResponse}. Mapped
 * to 502 Bad Gateway: the client's request was valid, but an upstream
 * dependency failed.
 */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiGenerationException(String message) {
        super(message);
    }
}
