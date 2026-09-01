package com.company.casehub.common.exception;

import com.company.casehub.common.response.ApiErrorResponse;
import com.company.casehub.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Single entry point for every error response. Translates the V1 exception
 * hierarchy and framework exceptions into the frozen {@link ApiErrorResponse}
 * shape. Production responses never contain stack traces, SQL or class names.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String traceId(HttpServletRequest request) {
        Object v = request.getAttribute(TraceIdFilter.REQUEST_ATTR);
        return v != null ? v.toString() : null;
    }

    @ExceptionHandler(CaseHubException.class)
    public ResponseEntity<ApiErrorResponse> handleCaseHub(CaseHubException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}: {}", code.name(), ex.getMessage(), ex.getErrorCode(), ex);
        } else if (code.getHttpStatus().is4xxClientError()) {
            log.info("[{}] {} (trace={})", code.name(), ex.getMessage(), traceId(request));
        }
        ApiErrorResponse body = ApiErrorResponse.of(
                code.name(), ex.getMessage(), traceId(request), ex.getDetails());
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> details = new java.util.LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.put(fe.getField(), fe.getDefaultMessage());
        }
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.VALIDATION_FAILED.name(), ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                traceId(request), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.VALIDATION_FAILED.name(), "Malformed request body.", traceId(request), Map.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.RESOURCE_NOT_FOUND.name(), ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage(),
                traceId(request), Map.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.RESOURCE_NOT_FOUND.name(), ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage(),
                traceId(request), Map.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.AUTH_UNAUTHORIZED.name(), ErrorCode.AUTH_UNAUTHORIZED.getDefaultMessage(),
                traceId(request), Map.of());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.AUTH_UNAUTHENTICATED.name(), ErrorCode.AUTH_UNAUTHENTICATED.getDefaultMessage(),
                traceId(request), Map.of());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("[INTERNAL_ERROR] trace={}", traceId(request), ex);
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                traceId(request), Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
