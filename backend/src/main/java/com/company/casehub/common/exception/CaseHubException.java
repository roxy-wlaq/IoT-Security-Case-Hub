package com.company.casehub.common.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Root of the V1 application exception hierarchy. Carries a frozen {@link ErrorCode}
 * (which already encodes the HTTP status) plus optional structured {@code details}.
 */
@Getter
public class CaseHubException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public CaseHubException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public CaseHubException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public CaseHubException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public CaseHubException withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
