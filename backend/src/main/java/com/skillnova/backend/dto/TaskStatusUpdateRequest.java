package com.skillnova.backend.dto;

import com.skillnova.backend.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/roadmap/{roadmapId}/tasks/{taskId}}
 * (Phase 11). Any of the three {@link TaskStatus} values may be requested
 * from any current status - there is no enforced sequential path, so a
 * user can freely mark a task back to {@code PENDING} or jump straight to
 * {@code COMPLETED}.
 */
public record TaskStatusUpdateRequest(
        @NotNull(message = "status is required") TaskStatus status
) {}
