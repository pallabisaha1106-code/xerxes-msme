package com.xerxes.msme.common.exception;

/** Raised when a request is well-formed but violates a domain rule (e.g. insufficient stock). */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
