package com.sporty.jackpot.api;

import java.time.Instant;
import java.util.List;

/** Uniform JSON error body returned by the {@link ApiExceptionHandler}. */
public record ApiError(Instant timestamp, int status, String error, String message, List<String> details) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, List.of());
    }

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
