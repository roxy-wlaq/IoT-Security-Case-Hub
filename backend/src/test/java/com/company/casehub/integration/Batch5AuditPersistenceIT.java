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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class Batch5AuditPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private AuditRecordRepository repository;

    @Autowired
    private AuditService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    void persistsEveryFrozenAuthoritativeMutationActionExactlyOnce() {
        UUID actorId = UUID.randomUUID();
        List<AuditAction> actions = List.of(
                AuditAction.ROLE_CHANGE,
                AuditAction.PROJECT_CREATE,
                AuditAction.PROJECT_ARCHIVE,
                AuditAction.TEST_CASE_PUBLISH,
                AuditAction.TEST_CASE_DEPRECATE,
                AuditAction.GENERATION_RULE_UPDATE,
                AuditAction.CAPABILITY_LIBRARY_UPDATE,
                AuditAction.EVIDENCE_DELETE);

        actions.forEach(action -> service.record(action, actorId, "admin", action.name(),
                action.name(), action.name(), Map.of("outcome", "success")));

        assertThat(repository.findAll()).extracting(AuditRecordEntity::getAction)
                .containsExactlyInAnyOrderElementsOf(actions);
    }

    @Test
    void auditInsertRollsBackWithTheBusinessTransaction() {
        UUID actorId = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.record(AuditAction.PROJECT_CREATE, actorId, "admin", "PROJECT",
                    "project-rollback", "rollback", Map.of("outcome", "success"));
            throw new IllegalStateException("business mutation failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.findAll()).isEmpty();
    }
}
