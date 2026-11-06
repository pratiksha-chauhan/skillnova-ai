package com.skillnova.backend.exception;

/**
 * Thrown when a roadmap generation request lacks enough profile data
 * (target role, target timeline) to build a meaningful prompt - neither the
 * request body nor the user's stored profile supplied them. Mapped to 422
 * Unprocessable Entity: the request is syntactically valid but cannot be
 * acted on yet.
 */
public class IncompleteProfileException extends RuntimeException {

    public IncompleteProfileException(String message) {
        super(message);
    }
}
