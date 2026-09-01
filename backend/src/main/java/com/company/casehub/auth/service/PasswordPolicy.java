package com.company.casehub.auth.service;

import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * V1 password rules (Security & RBAC Detail):
 *  - length 12..128
 *  - not blank / not all whitespace
 *  - not equal to the username (case-insensitive)
 * Violations raise {@code PASSWORD_POLICY_VIOLATION} (400).
 */
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    public void validate(String rawPassword, String username) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CaseHubException(ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "Password must not be blank.");
        }
        int len = rawPassword.length();
        if (len < MIN_LENGTH || len > MAX_LENGTH) {
            throw new CaseHubException(ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "Password length must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.");
        }
        if (username != null && !username.isBlank()
                && rawPassword.equalsIgnoreCase(username)) {
            throw new CaseHubException(ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "Password must not be equal to the username.");
        }
    }
}
