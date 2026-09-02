package com.company.casehub.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V006MasterTestCaseSchemaTest {

    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V006__master_test_case_library.sql");

    @Test
    void definesThePhase6MasterTestCaseSchemaAndInvariants() throws Exception {
        assertThat(Files.exists(MIGRATION)).isTrue();
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "master_test_cases",
                "test_case_versions",
                "test_steps",
                "test_case_tags",
                "test_case_tools",
                "test_case_standard_mappings",
                "test_case_attachments",
                "UNIQUE (master_test_case_id, version_major, version_minor)",
                "WHERE is_current_version = TRUE",
                "CHECK (version_major >= 1)",
                "CHECK (version_minor >= 0)",
                "ON DELETE CASCADE",
                "ON DELETE RESTRICT");
        assertThat(sql).doesNotContain("decision_points", "generation_rules", "projects");
    }
}
