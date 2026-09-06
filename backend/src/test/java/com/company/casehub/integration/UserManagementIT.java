package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.repository.AuditRecordRepository;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.testcase.dto.PagedResponse;
import com.company.casehub.user.dto.UserCreateRequest;
import com.company.casehub.user.dto.UserCreatedResponse;
import com.company.casehub.user.dto.UserPasswordResetRequest;
import com.company.casehub.user.dto.UserRolesUpdateRequest;
import com.company.casehub.user.dto.UserSummaryResponse;
import com.company.casehub.user.dto.UserUpdateRequest;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import com.company.casehub.user.service.UserManagementService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Real-PostgreSQL integration test for the admin user-management module:
 * Flyway V002 seed roles + V019 audit actions, JPA specification search,
 * forced-password-change flags and audit persistence.
 */
class UserManagementIT extends AbstractIntegrationTest {

    @Autowired
    private UserManagementService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID adminId = UUID.randomUUID();
    private UserPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = new UserPrincipal(adminId, "admin-actor", "hash", "Admin Actor", true, false,
                Set.of("ADMIN"), Set.of("user:read", "user:create", "user:update", "user:disable", "role:manage"));
    }

    @AfterEach
    void cleanUp() {
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        auditRecordRepository.deleteAllInBatch();
    }

    @Test
    void createUserPersistsForcedChangeAndAudit() {
        UserCreatedResponse response = service.create(
                new UserCreateRequest("alice", "Alice", null, List.of("TESTER")), admin);

        assertThat(response.generatedPassword()).hasSize(16);
        assertThat(response.mustChangePassword()).isTrue();
        assertThat(response.roles()).containsExactly("TESTER");

        UserEntity stored = userRepository.findById(response.id()).orElseThrow();
        assertThat(passwordEncoder.matches(response.generatedPassword(), stored.getPasswordHash())).isTrue();
        assertThat(stored.isMustChangePassword()).isTrue();
        assertThat(stored.isEnabled()).isTrue();

        assertThat(auditRecordRepository.findAll())
                .anySatisfy(row -> {
                    assertThat(row.getAction()).isEqualTo(AuditAction.USER_CREATE);
                    assertThat(row.getResourceLabel()).isEqualTo("alice");
                });
    }

    @Test
    void createRejectsDuplicateAndUnknownRoles() {
        service.create(new UserCreateRequest("bob", "Bob", "Str0ng-Passw0rd!", List.of("TESTER")), admin);

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("BOB", "Bob 2", null, List.of("TESTER")), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_USERNAME_DUPLICATE);

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("carol", "Carol", null, List.of("NOPE")), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_ROLE_INVALID);

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("dave", "Dave", "dave", List.of("TESTER")), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void listAppliesQEnabledRoleFiltersAndPagination() {
        service.create(new UserCreateRequest("alice.tester", "Alice Alpha", "Str0ng-Passw0rd!", List.of("TESTER")), admin);
        var bob = service.create(new UserCreateRequest("bob.coord", "Bob Beta", "Str0ng-Passw0rd!", List.of("TEST_COORDINATOR")), admin);
        service.setEnabled(bob.id(), false, admin);
        service.create(new UserCreateRequest("carol.admin", "Alice Carol", "Str0ng-Passw0rd!", List.of("ADMIN")), admin);

        // q matches username or displayName
        PagedResponse<UserSummaryResponse> byQ = service.list("alice", null, null, 0, 20);
        assertThat(byQ.content()).extracting(UserSummaryResponse::username)
                .containsExactly("alice.tester", "carol.admin");

        // enabled=false filter
        PagedResponse<UserSummaryResponse> disabled = service.list(null, false, null, 0, 20);
        assertThat(disabled.content()).extracting(UserSummaryResponse::username).containsExactly("bob.coord");

        // role filter
        PagedResponse<UserSummaryResponse> testers = service.list(null, null, "TESTER", 0, 20);
        assertThat(testers.content()).extracting(UserSummaryResponse::username).containsExactly("alice.tester");

        // pagination
        PagedResponse<UserSummaryResponse> paged = service.list(null, null, null, 0, 2);
        assertThat(paged.totalElements()).isEqualTo(3);
        assertThat(paged.totalPages()).isEqualTo(2);
        assertThat(paged.content()).hasSize(2);
        assertThat(paged.first()).isTrue();
        assertThat(paged.last()).isFalse();

        // unknown role -> error
        assertThatThrownBy(() -> service.list(null, null, "GHOST", 0, 20))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_ROLE_INVALID);
    }

    @Test
    void updateDisplayNameUpdateRolesEnableDisableFullFlow() {
        var created = service.create(
                new UserCreateRequest("erin", "Erin", "Str0ng-Passw0rd!", List.of("TESTER")), admin);

        var renamed = service.update(created.id(), new UserUpdateRequest("Erin Two"), admin);
        assertThat(renamed.displayName()).isEqualTo("Erin Two");

        var promoted = service.updateRoles(created.id(),
                new UserRolesUpdateRequest(List.of("TEST_COORDINATOR", "TESTER")), admin);
        assertThat(promoted.roles()).containsExactlyInAnyOrder("TEST_COORDINATOR", "TESTER");

        var disabled = service.setEnabled(created.id(), false, admin);
        assertThat(disabled.enabled()).isFalse();
        var enabled = service.setEnabled(created.id(), true, admin);
        assertThat(enabled.enabled()).isTrue();

        var actions = auditRecordRepository.findAll().stream().map(r -> r.getAction()).toList();
        assertThat(actions).contains(
                AuditAction.USER_CREATE, AuditAction.USER_UPDATE, AuditAction.ROLE_CHANGE,
                AuditAction.USER_DISABLE, AuditAction.USER_ENABLE);
    }

    @Test
    void resetPasswordStoresHashForcesChangeAndAudits() {
        var created = service.create(
                new UserCreateRequest("frank", "Frank", "Str0ng-Passw0rd!", List.of("TESTER")), admin);

        var reset = service.resetPassword(created.id(), new UserPasswordResetRequest(null), admin);

        assertThat(reset.generated()).isTrue();
        assertThat(reset.password()).isNotBlank();
        assertThat(reset.mustChangePassword()).isTrue();

        UserEntity stored = userRepository.findById(created.id()).orElseThrow();
        assertThat(passwordEncoder.matches(reset.password(), stored.getPasswordHash())).isTrue();
        assertThat(stored.isMustChangePassword()).isTrue();

        assertThat(auditRecordRepository.findAll())
                .anySatisfy(row -> assertThat(row.getAction()).isEqualTo(AuditAction.USER_PASSWORD_RESET));
    }

    @Test
    void selfProtectionGuardsWorkAgainstRealData() {
        UserCreatedResponse self = service.create(
                new UserCreateRequest("admin-actor", "Admin Actor", "Str0ng-Passw0rd!", List.of("ADMIN")), admin);
        UserPrincipal selfPrincipal = new UserPrincipal(self.id(), "admin-actor", "hash", "Admin Actor",
                true, false, Set.of("ADMIN"),
                Set.of("user:read", "user:create", "user:update", "user:disable", "role:manage"));

        assertThatThrownBy(() -> service.setEnabled(self.id(), false, selfPrincipal))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_SELF_OPERATION_FORBIDDEN);

        assertThatThrownBy(() -> service.updateRoles(self.id(),
                new UserRolesUpdateRequest(List.of("TESTER")), selfPrincipal))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_SELF_OPERATION_FORBIDDEN);

        assertThatThrownBy(() -> service.resetPassword(self.id(),
                new UserPasswordResetRequest("Str0ng-Passw0rd!"), selfPrincipal))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_SELF_OPERATION_FORBIDDEN);
    }

    @Test
    void v019AcceptsNewUserAuditActions() {
        jdbcTemplate.update("INSERT INTO casehub.audit_records "
                + "(id, action, actor_id, actor_username, resource_type) "
                + "VALUES (gen_random_uuid(), 'USER_DISABLE', ?, 'admin-actor', 'USER')", adminId);

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO casehub.audit_records "
                + "(id, action, actor_id, actor_username, resource_type) "
                + "VALUES (gen_random_uuid(), 'NOT_AN_ACTION', ?, 'admin-actor', 'USER')", adminId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
