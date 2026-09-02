package com.company.casehub.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V007MasterTestCaseSchemaTest {

    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V007__fix_master_test_case_constraints.sql");

    @Test
    void correctsMasterForeignKeyAndAddsTrigramIndexWithoutChangingV006() throws Exception {
        assertThat(Files.exists(MIGRATION)).isTrue();
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains(
                "ALTER TABLE casehub.test_case_versions",
                "DROP CONSTRAINT fk_test_case_versions_master",
                "ADD CONSTRAINT fk_test_case_versions_master",
                "ON DELETE RESTRICT",
                "CREATE INDEX",
                "USING GIN (case_code gin_trgm_ops)");
    }
}
