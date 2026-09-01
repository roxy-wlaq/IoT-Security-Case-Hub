package com.company.casehub.common.exception;

/**
 * Thrown for request-level validation that is not expressed via Jakarta Bean
 * Validation (maps to 400).
 */
public class ValidationException extends CaseHubException {

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ValidationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
