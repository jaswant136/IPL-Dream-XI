package com.ipldraftclub.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised instead of ever returning an empty player pool as a "successful" draft.
 * Maps to HTTP 422 (the request is valid, but the data cannot satisfy it).
 */
public class NoEligiblePoolException extends ApiException {

    public NoEligiblePoolException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
