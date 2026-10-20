package com.ipldraftclub.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/** A pick or completion attempt broke a squad/challenge rule (HTTP 422). */
public class RuleViolationException extends ApiException {

    public RuleViolationException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public RuleViolationException(String message, List<String> details) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message, details);
    }
}
