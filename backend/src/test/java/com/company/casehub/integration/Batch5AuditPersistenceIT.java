package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.audit.dto.AuditLogQuery;
import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.entity.AuditRecordEntity;
import com.company.casehub.audit.repository.AuditRecordRepository;
import com.company.casehub.audit.service.AuditService;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class Batch5AuditPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private AuditRecordRepository repository;

    @Autowired
    private AuditService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearAuditRows() {
        repository.deleteAllInBatch();
    }

    @Test
    void v018CreatesAuditTableAndEnforcesActorRules() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'casehub' AND table_name = 'audit_records'", Integer.class);
        assertThat(tableCount).isEqualTo(1);

        jdbcTemplate.update("INSERT INTO casehub.audit_records "
                        + "(id, action, actor_id, actor_username, resource_type) "
                        + "VALUES (gen_random_uuid(), 'LOGIN_FAILURE', NULL, 'failed-user', 'AUTHENTICATION')");

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO casehub.audit_records "
                        + "(id, action, actor_id, actor_username, resource_type) "
                        + "VALUES (gen_random_uuid(), 'PROJECT_CREATE', NULL, 'admin', 'PROJECT')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsNestedSanitizedDetailsAndQueriesThem() {
        UUID actorId = UUID.randomUUID();
        service.record(AuditAction.PROJECT_CREATE, actorId, "admin", "PROJECT", "p-1", "P-1",
                Map.of("request", Map.of("password", "secret", "name", "safe"),
                        "items", List.of(Map.of("csrfToken", "csrf", "label", "safe"))));

        AuditRecordEntity row = repository.findAll().stream().findFirst().orElseThrow();
        assertThat(row.getActorId()).isEqualTo(actorId);
        assertThat(row.getDetail().toString()).contains("name=safe").doesNotContain("password", "secret", "csrfToken", "csrf");
        assertThat(service.query(new AuditLogQuery(0, 20, AuditAction.PROJECT_CREATE,
                "PROJECT", "p-1", "admin", null, null)).content()).hasSize(1);
    }

    @Test
    void paginationUsesIdAsStableSecondaryOrdering() {
                    Timestamp occurredAt = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbcTemplate.update("INSERT INTO casehub.audit_records "
                        + "(id, occurred_at, action, actor_id, actor_username, resource_type, resource_id) "
                        + "VALUES (?, ?, 'LOGIN', gen_random_uuid(), 'admin', 'AUTHENTICATION', ?)",
                firstId, occurredAt, firstId.toString());
        jdbcTemplate.update("INSERT INTO casehub.audit_records "
                        + "(id, occurred_at, action, actor_id, actor_username, resource_type, resource_id) "
                        + "VALUES (?, ?, 'LOGIN', gen_random_uuid(), 'admin', 'AUTHENTICATION', ?)",
                secondId, occurredAt, secondId.toString());

        var page0 = service.query(new AuditLogQuery(0, 1, null, null, null, null, null, null));
        var page1 = service.query(new AuditLogQuery(1, 1, null, null, null, null, null, null));

        assertThat(page0.content()).extracting("id").containsExactly(secondId);
        assertThat(page1.content()).extracting("id").containsExactly(firstId);
    }
}
