package com.company.casehub.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsValidPassword() {
        assertThatCode(() -> policy.validate("ValidPassword123!", "alice"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> policy.validate("   ", "alice"))
                .isInstanceOf(CaseHubException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void rejectsTooShort() {
        assertThatThrownBy(() -> policy.validate("short1", "alice"))
                .isInstanceOf(CaseHubException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void rejectsTooLong() {
        String longPwd = "a".repeat(PasswordPolicy.MAX_LENGTH + 1);
        assertThatThrownBy(() -> policy.validate(longPwd, "alice"))
                .isInstanceOf(CaseHubException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void acceptsBoundaryLength() {
        assertThatCode(() -> policy.validate("a".repeat(PasswordPolicy.MIN_LENGTH), "alice"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validate("a".repeat(PasswordPolicy.MAX_LENGTH), "alice"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEqualToUsernameCaseInsensitive() {
        String username = "sharedsecret12";
        assertThatThrownBy(() -> policy.validate("sharedsecret12", username))
                .isInstanceOf(CaseHubException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION);
        // case-insensitive match must also be rejected
        assertThatThrownBy(() -> policy.validate("SHAREDSECRET12", username))
                .isInstanceOf(CaseHubException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void acceptsPasswordSimilarButNotEqual() {
        assertThatCode(() -> policy.validate("Alice123456789", "alice"))
                .doesNotThrowAnyException();
    }
}
