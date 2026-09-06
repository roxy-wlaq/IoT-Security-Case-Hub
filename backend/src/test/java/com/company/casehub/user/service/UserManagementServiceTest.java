package com.company.casehub.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.auth.service.PasswordPolicy;
import com.company.casehub.auth.service.SessionRegistryService;
import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.user.dto.UserCreateRequest;
import com.company.casehub.user.dto.UserPasswordResetRequest;
import com.company.casehub.user.dto.UserRolesUpdateRequest;
import com.company.casehub.user.dto.UserUpdateRequest;
import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordPolicy passwordPolicy;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SessionRegistryService sessionRegistryService;
    @Mock
    private AuditService auditService;

    private UserManagementService service;

    private final UUID adminId = UUID.randomUUID();
    private UserPrincipal admin;
    private final RoleEntity adminRole = new RoleEntity("ADMIN", "Administrator", null);
    private final RoleEntity testerRole = new RoleEntity("TESTER", "Tester", null);

    @BeforeEach
    void setUp() {
        service = new UserManagementService(userRepository, roleRepository, userRoleRepository,
                passwordPolicy, passwordEncoder, sessionRegistryService, auditService);
        admin = principal(adminId, "ADMIN");
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}hash");
    }

    private UserPrincipal principal(UUID id, String... roles) {
        return new UserPrincipal(id, "actor-" + id, "hash", "Actor", true, false,
                Set.of(roles), Set.of("user:read", "user:create", "user:update", "user:disable", "role:manage"));
    }

    private UserEntity persistedUser(UUID id, String username, boolean enabled) {
        UserEntity user = new UserEntity(username, "Display " + username, "{bcrypt}hash");
        user.setId(id);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    void createGeneratesInitialPasswordAndForcesChange() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(roleRepository.findByCode("TESTER")).thenReturn(Optional.of(testerRole));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new UserCreateRequest("alice", "Alice", null, List.of("TESTER")), admin);

        assertThat(response.generatedPassword()).isNotBlank().hasSize(16);
        assertThat(response.mustChangePassword()).isTrue();
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("{bcrypt}hash");
        assertThat(captor.getValue().isMustChangePassword()).isTrue();
        verify(auditService).record(eq(AuditAction.USER_CREATE), eq(admin), eq("USER"),
                eq(captor.getValue().getId()), eq("alice"), any());
    }

    @Test
    void createRejectsDuplicateUsernameCaseInsensitively() {
        when(userRepository.existsByUsernameIgnoreCase("Alice")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("Alice", "Alice", null, List.of("TESTER")), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_USERNAME_DUPLICATE);
    }

    @Test
    void createRejectsUnknownRole() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(roleRepository.findByCode("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("alice", "Alice", null, List.of("GHOST")), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_ROLE_INVALID);
    }

    @Test
    void createRejectsBlankRoleList() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("alice", "Alice", null, List.of(" ")), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_ROLE_INVALID);
    }

    @Test
    void updateChangesDisplayName() {
        UUID id = UUID.randomUUID();
        UserEntity user = persistedUser(id, "alice", true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(id, new UserUpdateRequest("New Name"), admin);

        assertThat(response.displayName()).isEqualTo("New Name");
        verify(auditService).record(eq(AuditAction.USER_UPDATE), eq(admin), eq("USER"), eq(id), eq("alice"), any());
    }

    @Test
    void updateRolesReplacesRoleSetAndAuditsRoleChange() {
        UUID id = UUID.randomUUID();
        UserEntity user = persistedUser(id, "alice", true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(id)).thenReturn(
                List.of(new UserRoleEntity(user, testerRole)),
                List.of(new UserRoleEntity(user, adminRole)));
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(adminRole));

        var response = service.updateRoles(id, new UserRolesUpdateRequest(List.of("ADMIN")), admin);

        assertThat(response.roles()).containsExactly("ADMIN");
        verify(userRoleRepository).deleteAllForUser(id);
        verify(auditService).record(eq(AuditAction.ROLE_CHANGE), eq(admin), eq("USER"), eq(id), eq("alice"), any());
    }

    @Test
    void adminCannotRemoveOwnAdminRole() {
        UUID id = adminId;
        UserEntity self = persistedUser(id, "actor-" + adminId, true);
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(roleRepository.findByCode("TESTER")).thenReturn(Optional.of(testerRole));

        assertThatThrownBy(() -> service.updateRoles(id,
                new UserRolesUpdateRequest(List.of("TESTER")), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_SELF_OPERATION_FORBIDDEN);
        verify(userRoleRepository, never()).deleteAllForUser(any());
    }

    @Test
    void disableExpiresSessionsAndAudits() {
        UUID id = UUID.randomUUID();
        UserEntity user = persistedUser(id, "alice", true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRoleRepository.findByUserId(id)).thenReturn(List.of());

        service.setEnabled(id, false, admin);

        assertThat(user.isEnabled()).isFalse();
        verify(sessionRegistryService).expireSessions(id);
        verify(auditService).record(eq(AuditAction.USER_DISABLE), eq(admin), eq("USER"), eq(id), eq("alice"), any());
    }

    @Test
    void adminCannotDisableSelf() {
        UserEntity self = persistedUser(adminId, "actor-" + adminId, true);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.setEnabled(adminId, false, admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_SELF_OPERATION_FORBIDDEN);
    }

    @Test
    void enablingUserDoesNotExpireSessions() {
        UUID id = UUID.randomUUID();
        UserEntity user = persistedUser(id, "alice", false);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRoleRepository.findByUserId(id)).thenReturn(List.of());

        var response = service.setEnabled(id, true, admin);

        assertThat(response.enabled()).isTrue();
        verify(sessionRegistryService, never()).expireSessions(any());
        verify(auditService).record(eq(AuditAction.USER_ENABLE), eq(admin), eq("USER"), eq(id), eq("alice"), any());
    }

    @Test
    void resetPasswordGeneratesTemporaryPasswordAndForcesChange() {
        UUID id = UUID.randomUUID();
        UserEntity user = persistedUser(id, "alice", true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resetPassword(id, new UserPasswordResetRequest(null), admin);

        assertThat(response.generated()).isTrue();
        assertThat(response.password()).isNotBlank();
        assertThat(response.mustChangePassword()).isTrue();
        verify(sessionRegistryService).expireSessions(id);
        verify(auditService).record(eq(AuditAction.USER_PASSWORD_RESET), eq(admin), eq("USER"), eq(id), eq("alice"), any());
    }

    @Test
    void resetPasswordWithSuppliedPasswordIsNotEchoed() {
        UUID id = UUID.randomUUID();
        UserEntity user = persistedUser(id, "alice", true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resetPassword(id, new UserPasswordResetRequest("Temp-Passw0rd!"), admin);

        assertThat(response.generated()).isFalse();
        assertThat(response.password()).isNull();
        verify(passwordPolicy).validate("Temp-Passw0rd!", "alice");
    }

    @Test
    void resetPasswordRejectsSelfOperation() {
        UserEntity self = persistedUser(adminId, "actor-" + adminId, true);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.resetPassword(adminId,
                new UserPasswordResetRequest("Temp-Passw0rd!"), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_SELF_OPERATION_FORBIDDEN);
    }

    @Test
    void unknownUserRaisesUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UserUpdateRequest("x"), admin))
                .isInstanceOf(CaseHubException.class)
                .extracting(e -> ((CaseHubException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
