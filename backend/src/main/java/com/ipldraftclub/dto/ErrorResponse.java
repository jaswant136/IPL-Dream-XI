package com.ipldraftclub.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(Instant timestamp, int status, String error, String message, String path, List<String> details) {

    public static ErrorResponse of(HttpStatus status, String message, String path, List<String> details) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path,
                details == null ? List.of() : details);
    }
}
