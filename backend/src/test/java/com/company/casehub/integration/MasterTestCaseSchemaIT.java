package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

class MasterTestCaseSchemaIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private UUID userId;
    private UUID categoryId;
    private UUID tagId;
    private UUID toolId;
    private UUID standardId;
    private UUID masterId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        tagId = UUID.randomUUID();
        toolId = UUID.randomUUID();
        standardId = UUID.randomUUID();
        masterId = UUID.randomUUID();
        jdbc.update("INSERT INTO casehub.users (id, username, display_name, password_hash) VALUES (?, ?, ?, ?)",
                userId, "schema_" + userId.toString().substring(0, 8), "Schema User", "hash");
        jdbc.update("INSERT INTO casehub.categories (id, code, name, level) VALUES (?, ?, ?, 1)",
                categoryId, "schema_" + categoryId.toString().substring(0, 8), "Schema Category");
        jdbc.update("INSERT INTO casehub.tags (id, code, name) VALUES (?, ?, ?)",
                tagId, "schema_" + tagId.toString().substring(0, 8), "Schema Tag " + tagId.toString().substring(0, 8));
        jdbc.update("INSERT INTO casehub.tools (id, code, name) VALUES (?, ?, ?)",
                toolId, "schema_" + toolId.toString().substring(0, 8), "Schema Tool " + toolId.toString().substring(0, 8));
        jdbc.update("INSERT INTO casehub.standard_task_types (id, code, name, type) VALUES (?, ?, ?, 'STANDARD')",
                standardId, "schema_" + standardId.toString().substring(0, 8), "Schema Standard " + standardId.toString().substring(0, 8));
        jdbc.update("INSERT INTO casehub.master_test_cases (id, case_code, category_id, created_by) VALUES (?, ?, ?, ?)",
                masterId, "SCHEMA-" + masterId.toString().substring(0, 8), categoryId, userId);
    }

    @Test
    void v006AndV007TablesAndCaseCodeIndexExist() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'casehub' AND table_name IN "
                        + "('master_test_cases', 'test_case_versions', 'test_steps', 'test_case_tags', 'test_case_tools', "
                        + "'test_case_standard_mappings', 'test_case_attachments')", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'casehub' AND indexname = 'idx_master_test_cases_case_code_trgm'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void caseCodeIsUniqueIgnoringCase() {
        jdbc.update("INSERT INTO casehub.master_test_cases (id, case_code, category_id, created_by) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), "Case-Code", categoryId, userId);

        assertConstraintViolation(() -> jdbc.update(
                "INSERT INTO casehub.master_test_cases (id, case_code, category_id, created_by) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), "case-code", categoryId, userId));
    }

    @Test
    void versionNumberIsUniquePerMaster() {
        insertVersion(UUID.randomUUID(), 1, 0, "DRAFT", false, "Version One");

        assertConstraintViolation(() -> insertVersion(UUID.randomUUID(), 1, 0, "DRAFT", false, "Duplicate Version"));
    }

    @Test
    void onlyOneCurrentVersionIsAllowed() {
        insertVersion(UUID.randomUUID(), 1, 1, "PUBLISHED", true, "Current One");

        assertConstraintViolation(() -> insertVersion(UUID.randomUUID(), 1, 2, "PUBLISHED", true, "Duplicate Current"));
    }

    @Test
    void draftCannotBeCurrent() {
        assertConstraintViolation(() -> insertVersion(UUID.randomUUID(), 2, 0, "DRAFT", true, "Draft Current"));
    }

    @Test
    void versionFloorsAreEnforced() {
        assertConstraintViolation(() -> insertVersion(UUID.randomUUID(), 0, 0, "DRAFT", false, "Invalid Major"));
        assertConstraintViolation(() -> insertVersion(UUID.randomUUID(), 3, -1, "DRAFT", false, "Invalid Minor"));
    }

    @Test
    void relationKeysAndStepSequenceAreUnique() {
        UUID versionId = UUID.randomUUID();
        insertVersion(versionId, 4, 0, "DRAFT", false, "Relations");
        jdbc.update("INSERT INTO casehub.test_steps (id, test_case_version_id, sequence_no, content) VALUES (?, ?, 1, 'one')",
                UUID.randomUUID(), versionId);
        assertConstraintViolation(() -> jdbc.update(
                "INSERT INTO casehub.test_steps (id, test_case_version_id, sequence_no, content) VALUES (?, ?, 1, 'duplicate')",
                UUID.randomUUID(), versionId));

        jdbc.update("INSERT INTO casehub.test_case_tags (id, master_test_case_id, tag_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), masterId, tagId);
        assertConstraintViolation(() -> jdbc.update(
                "INSERT INTO casehub.test_case_tags (id, master_test_case_id, tag_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), masterId, tagId));

        jdbc.update("INSERT INTO casehub.test_case_tools (id, test_case_version_id, tool_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), versionId, toolId);
        assertConstraintViolation(() -> jdbc.update(
                "INSERT INTO casehub.test_case_tools (id, test_case_version_id, tool_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), versionId, toolId));

        jdbc.update("INSERT INTO casehub.test_case_standard_mappings (id, test_case_version_id, standard_task_type_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), versionId, standardId);
        assertConstraintViolation(() -> jdbc.update(
                "INSERT INTO casehub.test_case_standard_mappings (id, test_case_version_id, standard_task_type_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), versionId, standardId));
    }

    @Test
    void deletingMasterWithVersionIsRestricted() {
        insertVersion(UUID.randomUUID(), 5, 0, "DRAFT", false, "Referenced Master");

        assertConstraintViolation(() -> jdbc.update("DELETE FROM casehub.master_test_cases WHERE id = ?", masterId));
    }

    private void insertVersion(UUID id, int major, int minor, String status, boolean current, String name) {
        jdbc.update("INSERT INTO casehub.test_case_versions "
                        + "(id, master_test_case_id, version_major, version_minor, status, is_current_version, case_name, selection_mode, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'SINGLE', ?)",
                id, masterId, major, minor, status, current, name, userId);
    }

    private static void assertConstraintViolation(ThrowingCallable operation) {
        assertThatThrownBy(operation).isInstanceOf(DataAccessException.class);
    }
}
