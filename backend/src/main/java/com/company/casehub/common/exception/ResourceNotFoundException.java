package com.company.casehub.common.exception;

/**
 * Thrown when an entity referenced by id (or natural key) does not exist.
 * Carries the specific NOT_FOUND {@link ErrorCode} so handlers stay precise.
 */
public class ResourceNotFoundException extends CaseHubException {

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
