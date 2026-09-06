-- V019: extend the frozen audit action catalog (V018) with the interactive
-- admin user-management events. ROLE_CHANGE already existed (used by the
-- bootstrap ADMIN creation); the five new values cover the user management
-- module: create / profile update / enable / disable / password reset.
--
-- The V018 CHECK constraint is replaced, not edited in place: the original
-- migration stays untouched (Migration Version Owner rule).

ALTER TABLE casehub.audit_records
    DROP CONSTRAINT IF EXISTS chk_audit_records_action;

ALTER TABLE casehub.audit_records
    ADD CONSTRAINT chk_audit_records_action CHECK (action IN (
        'LOGIN', 'LOGIN_FAILURE', 'ROLE_CHANGE', 'PROJECT_CREATE', 'PROJECT_ARCHIVE',
        'TEST_CASE_PUBLISH', 'TEST_CASE_DEPRECATE', 'GENERATION_RULE_UPDATE',
        'CAPABILITY_LIBRARY_UPDATE', 'EVIDENCE_DELETE',
        'USER_CREATE', 'USER_UPDATE', 'USER_ENABLE', 'USER_DISABLE',
        'USER_PASSWORD_RESET'));
