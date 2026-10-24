package com.skillnova.backend.exception;

/**
 * Thrown when attempting to register with an email address that is already
 * associated with an account.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
