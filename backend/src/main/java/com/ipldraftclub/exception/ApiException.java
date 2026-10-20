package com.ipldraftclub.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Base class for errors that map directly to an HTTP response. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> details;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<String> details) {
        super(message);
        this.status = status;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetails() {
        return details;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException unprocessable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public static ApiException tooManyRequests(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
