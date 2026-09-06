package com.company.casehub.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Batch5MigrationVerificationTest {

    @Test
    void keepsV018AsTheOnlyBatch5MigrationAndFreezesItsGovernanceSchema() throws Exception {
        Path migrations = Path.of("src/main/resources/db/migration");
        String v018 = Files.readString(migrations.resolve("V018__audit_records.sql"));

        assertThat(v018).contains(
                "CREATE TABLE IF NOT EXISTS casehub.audit_records",
                "actor_username VARCHAR(100) NOT NULL",
                "chk_audit_records_actor_username",
                "chk_audit_records_actor CHECK (action = 'LOGIN_FAILURE' OR actor_id IS NOT NULL)",
                "ix_audit_records_occurred_at",
                "ix_audit_records_resource");
        assertThat(Files.exists(migrations.resolve("V017__customization_change_management.sql"))).isTrue();
        try (Stream<Path> files = Files.list(migrations)) {
            // V018 froze the governance schema. The interactive user-management module (delivered
            // after Batch 5/6) legitimately extends the audit action catalog via V019 (it only
            // rebuilds the CHECK constraint, never edits V017/V018 in place). What stays forbidden
            // is any migration *above* V019 without an approved follow-up scope.
            assertThat(files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("V0") && migrationVersion(name) > 19)
                    .toList())
                    .isEmpty();
        }
    }

    private static int migrationVersion(String fileName) {
        String digits = fileName.substring(1, fileName.indexOf('_'));
        return Integer.parseInt(digits);
    }

    @Test
    void documentsTheReadOnlyExportAndAuditContracts() throws Exception {
        Path contract = Path.of("..", "docs", "batch5-api-contract.md");
        if (!Files.exists(contract)) {
            contract = Path.of("docs", "batch5-api-contract.md");
        }
        String document = Files.readString(contract);
        assertThat(document).contains("REPEATABLE_READ", "recursive", "Backing Type", "occurredAt DESC, id DESC");
    }
}
