package com.skillnova.backend.exception;

import java.util.UUID;

/**
 * Thrown when a user referenced by ID (e.g. from a validated JWT subject
 * claim) can no longer be found - a rare race condition where the account
 * was deleted between authentication and the operation being performed.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User '" + userId + "' was not found");
    }
}
