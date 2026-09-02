-- Phase 6 review fix: preserve Master rows while versions still reference them.
ALTER TABLE casehub.test_case_versions
    DROP CONSTRAINT fk_test_case_versions_master;

ALTER TABLE casehub.test_case_versions
    ADD CONSTRAINT fk_test_case_versions_master
    FOREIGN KEY (master_test_case_id) REFERENCES casehub.master_test_cases(id)
    ON DELETE RESTRICT;

-- Frozen schema requirement for case-code similarity/search operations.
CREATE INDEX IF NOT EXISTS idx_master_test_cases_case_code_trgm
    ON casehub.master_test_cases USING GIN (case_code gin_trgm_ops);
