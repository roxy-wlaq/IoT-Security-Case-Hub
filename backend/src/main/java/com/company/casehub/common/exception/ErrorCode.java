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

    // ---- Master Test Case Library (Phase 6) ----
    TEST_CASE_NOT_FOUND(HttpStatus.NOT_FOUND, "Test case not found."),
    TEST_CASE_CODE_DUPLICATE(HttpStatus.CONFLICT, "Test case code already exists."),
    TEST_CASE_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Test case version not found."),
    TEST_CASE_VERSION_DUPLICATE(HttpStatus.CONFLICT, "Test case version already exists."),
    TEST_CASE_VERSION_IMMUTABLE(HttpStatus.CONFLICT, "This test case version is immutable."),
    TEST_CASE_DRAFT_REQUIRED(HttpStatus.CONFLICT, "A Draft version is required for this operation."),
    TEST_CASE_STEP_SEQUENCE_DUPLICATE(HttpStatus.CONFLICT, "Test step sequence is duplicated."),
    TEST_CASE_STEP_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "Test step content is required."),
    TEST_CASE_CATEGORY_INVALID(HttpStatus.BAD_REQUEST, "Test case category is missing or disabled."),
    TEST_CASE_TAG_INVALID(HttpStatus.BAD_REQUEST, "One or more test case tags are invalid."),
    TEST_CASE_TOOL_INVALID(HttpStatus.BAD_REQUEST, "One or more test case tools are invalid."),
    TEST_CASE_STANDARD_INVALID(HttpStatus.BAD_REQUEST, "One or more standards are invalid."),
    TEST_CASE_PROGRESSIVE_ROLE_INVALID(HttpStatus.BAD_REQUEST, "Progressive role is invalid."),
    TEST_CASE_SORT_FIELD_INVALID(HttpStatus.BAD_REQUEST, "Test case sort field is invalid."),
    TEST_CASE_DRAFT_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "You cannot edit this Draft."),

    // ---- Test Case Lifecycle (Phase 7) ----
    TEST_CASE_LIFECYCLE_TRANSITION_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "This lifecycle transition is not valid for the current status."),
    TEST_CASE_REVISION_CLOSED(HttpStatus.CONFLICT, "This revision is closed and cannot be submitted, reviewed or edited."),
    TEST_CASE_REVIEW_COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "A review comment is required for this action."),
    TEST_CASE_DRAFT_INCOMPLETE(HttpStatus.UNPROCESSABLE_ENTITY, "The draft is missing required fields before review."),
    TEST_CASE_LIFECYCLE_FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this lifecycle action."),
    TEST_CASE_REVISION_SOURCE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "The revision source version is invalid."),
    TEST_CASE_CONTRIBUTOR_INVALID(HttpStatus.BAD_REQUEST, "The contributor is invalid or already added."),

    // ---- Decision Point / Master DAG (Phase 8) ----
    TEST_CASE_TRANSITION_TARGET_COUNT_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "The transition has an invalid number of targets."),
    TEST_CASE_TRANSITION_TARGET_INVALID(HttpStatus.BAD_REQUEST, "One or more transition targets are invalid."),
    TEST_CASE_DAG_CYCLE_DETECTED(HttpStatus.UNPROCESSABLE_ENTITY, "The master test case logic graph must not contain a cycle."),

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
