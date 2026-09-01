package com.company.casehub.common.exception;

/**
 * Thrown on uniqueness / state conflicts (maps to 409).
 */
public class ConflictException extends CaseHubException {

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ConflictException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
