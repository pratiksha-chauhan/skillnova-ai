package com.skillnova.backend.exception;

import java.util.UUID;

/**
 * Thrown when a roadmap referenced by ID either does not exist, or exists
 * but does not belong to the authenticated user. Both cases are treated
 * identically (404) so ownership is never leaked via a 403 vs 404
 * distinction.
 */
public class RoadmapNotFoundException extends RuntimeException {

    public RoadmapNotFoundException(UUID roadmapId) {
        super("Roadmap '" + roadmapId + "' was not found");
    }
}
