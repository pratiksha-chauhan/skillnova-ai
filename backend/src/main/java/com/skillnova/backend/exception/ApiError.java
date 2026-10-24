package com.skillnova.backend.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned by every failed API call.
 *
 * @param fieldErrors populated only for Bean Validation failures
 *                    (400 Bad Request); otherwise omitted/empty.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}
}
