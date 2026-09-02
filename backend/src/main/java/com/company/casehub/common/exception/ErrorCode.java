package com.company.casehub.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Frozen V1 error codes. The HTTP status is the single source of truth used by
 * {@link GlobalExceptionHandler}; module code must never invent new status mapping.
 * Keep this list in sync with the Frontend {@code ApiError.code} switch.
 */
public enum ErrorCode {

    // ---- Auth (Phase 2) ----
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username or password."),
    AUTH_LOGIN_TEMPORARILY_BLOCKED(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Please try again later."),
    AUTH_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required."),
    AUTH_UNAUTHORIZED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action."),
    AUTH_PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "You must change your password before continuing."),
    USER_DISABLED(HttpStatus.FORBIDDEN, "This account has been disabled."),
    PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "Password does not satisfy the policy requirements."),
    AUTH_CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "The current password is incorrect."),

    // ---- Dictionary (Phase 4) ----
    STANDARD_NOT_FOUND(HttpStatus.NOT_FOUND, "Standard/Task Type not found."),
    STANDARD_CODE_DUPLICATE(HttpStatus.CONFLICT, "Standard/Task Type code already exists."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found."),
    CATEGORY_CODE_DUPLICATE(HttpStatus.CONFLICT, "Category code already exists."),
    CATEGORY_PARENT_INVALID(HttpStatus.BAD_REQUEST, "Parent category must be a level-1 category."),
    CATEGORY_HAS_CHILDREN(HttpStatus.CONFLICT, "Cannot disable a category that has active children."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "Tag not found."),
    TAG_NAME_DUPLICATE(HttpStatus.CONFLICT, "Tag name already exists."),
    TAG_CODE_DUPLICATE(HttpStatus.CONFLICT, "Tag code already exists."),
    TOOL_NOT_FOUND(HttpStatus.NOT_FOUND, "Tool not found."),
    TOOL_NAME_DUPLICATE(HttpStatus.CONFLICT, "Tool name already exists."),
    TOOL_CODE_DUPLICATE(HttpStatus.CONFLICT, "Tool code already exists."),

    // ---- Capability Library (Phase 5) ----
    CAPABILITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Capability not found."),
    CAPABILITY_CODE_DUPLICATE(HttpStatus.CONFLICT, "Capability code already exists."),
    CAPABILITY_PARENT_INVALID(HttpStatus.BAD_REQUEST, "Parent capability does not exist."),
    CAPABILITY_CYCLE_DETECTED(HttpStatus.CONFLICT, "Capability tree must not contain a cycle."),

    // ---- Generic (reused by later phases) ----
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    CONFLICT(HttpStatus.CONFLICT, "The request conflicts with the current state."),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "The operation violates a business rule."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public int getStatusValue() {
        return httpStatus.value();
    }
}
