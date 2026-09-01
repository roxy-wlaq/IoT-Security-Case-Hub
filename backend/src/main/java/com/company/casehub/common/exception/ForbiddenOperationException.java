package com.company.casehub.common.exception;

/**
 * Thrown when an authenticated caller is not allowed to perform the requested
 * operation on the specific resource (resource-level authorization failure).
 */
public class ForbiddenOperationException extends CaseHubException {

    public ForbiddenOperationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ForbiddenOperationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
