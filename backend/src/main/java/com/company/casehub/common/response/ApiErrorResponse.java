package com.company.casehub.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Unified error body returned by {@link com.company.casehub.common.exception.GlobalExceptionHandler}.
 * Never leaks stack traces, SQL, or internal class names to the client.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        Map<String, Object> details) {

    public static ApiErrorResponse of(String code, String message, String traceId, Map<String, Object> details) {
        return new ApiErrorResponse(code, message, traceId, Instant.now(), details);
    }
}
