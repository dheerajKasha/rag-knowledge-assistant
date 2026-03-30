package com.enterprise.ragqa.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, Instant.now());
    }
}
