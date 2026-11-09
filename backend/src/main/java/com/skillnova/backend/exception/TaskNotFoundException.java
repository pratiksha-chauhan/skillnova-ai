package com.skillnova.backend.exception;

import java.util.UUID;

/**
 * Thrown when a roadmap task referenced by ID either does not exist, or
 * exists but does not belong to the given roadmap / the authenticated
 * user's own roadmap. All three cases are treated identically (404) so
 * ownership is never leaked via a 403 vs 404 distinction - same pattern as
 * {@link RoadmapNotFoundException}.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID taskId) {
        super("Task '" + taskId + "' was not found");
    }
}
