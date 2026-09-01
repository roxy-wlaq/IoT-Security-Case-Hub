package com.company.casehub.common.exception;

/**
 * Thrown when an operation would violate a domain/business invariant (maps to 422).
 */
public class BusinessRuleException extends CaseHubException {

    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessRuleException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
