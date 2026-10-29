package com.jobtracker.exception;

/** Maps to HTTP 403 - authenticated, but not the owner of the resource. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
