package com.jobtracker.exception;

/** Maps to HTTP 422 - request is well-formed but violates a business rule. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
