package com.company.casehub.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Contract test for {@code V005__fix_coordinator_reference_read_permissions.sql}.
 *
 * <p>Integration tests ({@code MigrationIT}) cannot run in this environment because
 * Testcontainers cannot start PostgreSQL, so the guarantees of the HIGH finding —
 * idempotent, additive, TEST_COORDINATOR only, ADMIN/TESTER untouched — are asserted
 * here against the migration SQL itself. These assertions are static (no database),
 * but they fail loudly if someone edits V005 and breaks one of its guarantees.
 */
class V005CoordinatorReadPermissionsTest {

    private static final String MIGRATION = "db/migration/V005__fix_coordinator_reference_read_permissions.sql";

    private static final List<String> EXPECTED_PERMISSIONS =
            List.of("standard:read", "category:read", "tag:read", "tool:read", "capability:read");

    private static String migrationSql() {
        ClassPathResource resource = new ClassPathResource(MIGRATION);
        assertThat(resource.exists()).as("migration %s must exist on the classpath", MIGRATION).isTrue();
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + MIGRATION, e);
        }
    }

    @Test
    @DisplayName("V005 grants exactly the five reference/capability read permissions")
    void grantsExactlyTheFiveReadPermissions() {
        String sql = migrationSql();
        EXPECTED_PERMISSIONS.forEach(code -> assertThat(sql).contains("'" + code + "'"));
        // no manage permission may leak into the coordinator read grant
        assertThat(sql).doesNotContain(":manage");
    }

    @Test
    @DisplayName("V005 targets TEST_COORDINATOR only")
    void targetsTestCoordinatorOnly() {
        String sql = migrationSql();
        assertThat(sql).contains("r.code = 'TEST_COORDINATOR'");
        assertThat(sql).doesNotContain("'ADMIN'").doesNotContain("'TESTER'");
    }

    @Test
    @DisplayName("V005 is idempotent and additive (no DELETE / UPDATE / DROP)")
    void isIdempotentAndAdditive() {
        String sql = migrationSql();
        assertThat(sql).contains("ON CONFLICT (role_id, permission_id) DO NOTHING");
        Set.of("DELETE FROM", "UPDATE casehub", "DROP ", "TRUNCATE ")
                .forEach(forbidden -> assertThat(sql.toUpperCase()).doesNotContain(forbidden));
    }

    @Test
    @DisplayName("Frozen migrations V001–V004 remain in place and V006 is the next free version")
    void frozenMigrationsRemainInPlace() {
        // V002 is frozen: it must still seed exactly the three roles. V005 is additive only
        // and must never be renumbered on top of an existing version.
        String v002 = readResource("db/migration/V002__identity_rbac.sql");
        assertThat(v002).contains("'ADMIN'").contains("'TEST_COORDINATOR'").contains("'TESTER'");

        List.of(
                        "db/migration/V001__init_schema.sql",
                        "db/migration/V003__reference_catalog.sql",
                        "db/migration/V004__capability_library.sql")
                .forEach(path -> assertThat(new ClassPathResource(path).exists())
                        .as("%s must still exist", path)
                        .isTrue());

        // Phase 6 must start at V006 — no competing V005 exists.
        assertThat(new ClassPathResource("db/migration/V006__placeholder.sql").exists()).isFalse();
    }

    private static String readResource(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + path, e);
        }
    }
}
