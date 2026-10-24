package com.skillnova.backend.exception;

/**
 * Thrown when login credentials do not match an existing account. The
 * message is deliberately generic (not "email not found" vs "wrong
 * password") so the API never reveals whether a given email is registered.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
