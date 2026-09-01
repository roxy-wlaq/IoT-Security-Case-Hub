package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.user.repository.PermissionRepository;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
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
        // ADMIN(54) + TEST_COORDINATOR(31) + TESTER(21)
        assertThat(rpCount).isEqualTo(106);

        assertThat(roleRepository.findByCode("ADMIN")).isPresent();
        assertThat(roleRepository.findByCode("TEST_COORDINATOR")).isPresent();
        assertThat(roleRepository.findByCode("TESTER")).isPresent();
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
}
