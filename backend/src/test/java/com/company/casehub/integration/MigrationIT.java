package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.user.repository.PermissionRepository;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void flywayCreatesSchemaAndExtension() {
        Integer ext = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class);
        assertThat(ext).isEqualTo(1);

        Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'casehub' "
                        + "AND table_name IN ('users','roles','permissions','user_roles','role_permissions')",
                Integer.class);
        assertThat(tables).isEqualTo(5);
    }

    @Test
    void seedsRolesPermissionsAndMapping() {
        assertThat(roleRepository.count()).isEqualTo(3);
        assertThat(permissionRepository.count()).isEqualTo(54);

        Integer rpCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM casehub.role_permissions", Integer.class);
        // ADMIN(54) + TEST_COORDINATOR(36 = 31 from V002 + 5 from V005) + TESTER(21)
        assertThat(rpCount).isEqualTo(111);

        assertThat(roleRepository.findByCode("ADMIN")).isPresent();
        assertThat(roleRepository.findByCode("TEST_COORDINATOR")).isPresent();
        assertThat(roleRepository.findByCode("TESTER")).isPresent();
    }

    @Test
    void coordinatorHasReferenceAndCapabilityReadPermissions() {
        List<String> codes = List.of("standard:read", "category:read", "tag:read", "tool:read", "capability:read");

        Integer granted = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM casehub.role_permissions rp "
                        + "JOIN casehub.roles r ON r.id = rp.role_id "
                        + "JOIN casehub.permissions p ON p.id = rp.permission_id "
                        + "WHERE r.code = 'TEST_COORDINATOR' AND p.code = ANY (?)",
                Integer.class,
                (Object) codes.toArray(new String[0]));
        assertThat(granted).isEqualTo(codes.size());

        // V005 is additive: it introduces no coordinator manage permission.
        Integer manageGrants = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM casehub.role_permissions rp "
                        + "JOIN casehub.roles r ON r.id = rp.role_id "
                        + "JOIN casehub.permissions p ON p.id = rp.permission_id "
                        + "WHERE r.code = 'TEST_COORDINATOR' AND p.code IN ("
                        + "'standard:manage','category:manage','tag:manage','tool:manage',"
                        + "'capability:manage_library')",
                Integer.class);
        assertThat(manageGrants).isZero();
    }

    @Test
    void adminAndTesterPermissionSetsAreUnchangedByV005() {
        Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM casehub.role_permissions rp JOIN casehub.roles r ON r.id = rp.role_id "
                        + "WHERE r.code = 'ADMIN'",
                Integer.class);
        Integer testerCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM casehub.role_permissions rp JOIN casehub.roles r ON r.id = rp.role_id "
                        + "WHERE r.code = 'TESTER'",
                Integer.class);

        assertThat(adminCount).isEqualTo(54);
        assertThat(testerCount).isEqualTo(21);
    }

    @Test
    void usernameLowercaseUniqueIndexIsEnforced() {
        jdbcTemplate.update("INSERT INTO casehub.users (id, username, display_name, password_hash, enabled) "
                + "VALUES (gen_random_uuid(), 'Alice', 'Alice', 'x', true)");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO casehub.users (id, username, display_name, password_hash, enabled) "
                        + "VALUES (gen_random_uuid(), 'alice', 'alice', 'x', true)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userRepositoryCaseInsensitiveLookup() {
        Map<String, Object> inserted = jdbcTemplate.queryForMap(
                "INSERT INTO casehub.users (id, username, display_name, password_hash, enabled) "
                        + "VALUES (gen_random_uuid(), 'Bobby', 'Bob', 'x', true) RETURNING id");
        java.util.UUID id = (java.util.UUID) inserted.get("id");

        assertThat(userRepository.findByUsernameIgnoreCase("bobby")).isPresent();
        assertThat(userRepository.findByUsernameIgnoreCase("BOBBY").orElseThrow().getId()).isEqualTo(id);
    }

    @Test
    void batch3MigrationCreatesExecutionStackTables() {
        Integer phase8Tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'casehub' "
                        + "AND table_name IN ('decision_points','transitions','transition_targets')", Integer.class);
        assertThat(phase8Tables).isEqualTo(3);
        Integer runtimeTables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'casehub' "
                        + "AND table_name IN ('project_decision_selections','branch_outcomes',"
                        + "'project_test_case_triggers','evidence','notes')", Integer.class);
        assertThat(runtimeTables).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM casehub.flyway_schema_history WHERE version = '016'", Integer.class)).isEqualTo(1);
    }

    @Test
    void batch2MigrationsCreatePlanningTablesAndConstraints() {
        List<String> tables = List.of(
                "projects", "project_standards", "project_coordinators", "project_capabilities",
                "generation_rules", "generation_condition_groups", "generation_conditions",
                "generation_rule_outputs", "generation_runs", "generation_recommendations",
                "generation_recommendation_rules", "project_test_case_preferences",
                "project_test_cases", "project_test_case_sources", "project_test_case_assignees");
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'casehub' "
                        + "AND table_name = ANY (?)", Integer.class, (Object) tables.toArray(new String[0]));
        assertThat(tableCount).isEqualTo(tables.size());

        Integer primaryIndex = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'casehub' "
                        + "AND indexname = 'uq_project_primary_coordinator'", Integer.class);
        assertThat(primaryIndex).isEqualTo(1);

        Integer projectMasterIndex = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'casehub' "
                        + "AND indexname = 'uq_project_master_test_case'", Integer.class);
        assertThat(projectMasterIndex).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM casehub.flyway_schema_history WHERE version = '015'", Integer.class))
                .isEqualTo(1);
    }
}
