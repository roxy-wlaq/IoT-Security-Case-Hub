package com.company.casehub.testcase.dto;

import jakarta.validation.constraints.Size;

/**
 * Shared request body for the submit-review / publish / return / reject /
 * deprecate lifecycle endpoints. {@code comment} is optional for SUBMIT,
 * PUBLISH and DEPRECATE but required for RETURN and REJECT — the latter is
 * enforced in the Service layer (TEST_CASE_REVIEW_COMMENT_REQUIRED).
 */
public record LifecycleActionRequest(@Size(max = 2000) String comment) {
}
