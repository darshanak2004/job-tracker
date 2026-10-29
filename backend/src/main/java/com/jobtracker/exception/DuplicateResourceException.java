package com.jobtracker.exception;

/** Maps to HTTP 409 (e.g. email already taken, already applied to this job). */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
