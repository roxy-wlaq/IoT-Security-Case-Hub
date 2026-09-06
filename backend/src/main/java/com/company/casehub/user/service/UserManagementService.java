package com.company.casehub.user.service;

import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.auth.service.PasswordPolicy;
import com.company.casehub.auth.service.SessionRegistryService;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.testcase.dto.PagedResponse;
import com.company.casehub.user.dto.UserCreateRequest;
import com.company.casehub.user.dto.UserCreatedResponse;
import com.company.casehub.user.dto.UserPasswordResetRequest;
import com.company.casehub.user.dto.UserPasswordResetResponse;
import com.company.casehub.user.dto.UserRolesUpdateRequest;
import com.company.casehub.user.dto.UserSummaryResponse;
import com.company.casehub.user.dto.UserUpdateRequest;
import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Admin user management (list / create / update / roles / enable / disable /
 * password reset). Every endpoint is ADMIN-gated at the controller with the
 * matching frozen permission from V002; this service enforces the domain
 * invariants on top:
 *
 * <ul>
 *   <li>username is immutable and unique case-insensitively (V002 index),</li>
 *   <li>an admin cannot disable themselves or drop their own ADMIN role
 *       (lockout protection),</li>
 *   <li>created users and password-reset targets get
 *       {@code must_change_password = true} and active sessions of a disabled
 *       user or a password-reset target are expired via
 *       {@link SessionRegistryService},</li>
 *   <li>every mutation writes an audit record (V019 USER_* actions, ROLE_CHANGE
 *       for role assignment).</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserManagementService {

    private static final int MAX_PAGE_SIZE = 200;
    /** Unambiguous alphabet (no 0/O/1/l/I) for generated passwords. */
    private static final String PASSWORD_ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistryService sessionRegistryService;
    private final AuditService auditService;

    public UserManagementService(UserRepository userRepository, RoleRepository roleRepository,
                                 UserRoleRepository userRoleRepository, PasswordPolicy passwordPolicy,
                                 PasswordEncoder passwordEncoder, SessionRegistryService sessionRegistryService,
                                 AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistryService = sessionRegistryService;
        this.auditService = auditService;
    }

    public PagedResponse<UserSummaryResponse> list(String q, Boolean enabled, String role, int page, int size) {
        String roleCode = StringUtils.hasText(role) ? role.trim() : null;
        if (roleCode != null && roleRepository.findByCode(roleCode).isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.USER_ROLE_INVALID, "Unknown role: " + roleCode);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "username"));
        Page<UserSummaryResponse> result = userRepository
                .findAll(querySpec(q, enabled, roleCode), pageable)
                .map(user -> UserSummaryResponse.from(user, loadRoles(user.getId())));
        return PagedResponse.from(result);
    }

    @Transactional
    public UserCreatedResponse create(UserCreateRequest request, UserPrincipal actor) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException(ErrorCode.USER_USERNAME_DUPLICATE, "Username already exists: " + username);
        }

        String displayName = request.displayName().trim();
        List<RoleEntity> roles = resolveRoles(request.roles());

        boolean generated = !StringUtils.hasText(request.password());
        String rawPassword = generated ? generatePassword() : request.password();
        passwordPolicy.validate(rawPassword, username);

        UserEntity user = new UserEntity(username, displayName, passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setMustChangePassword(true);
        user = userRepository.save(user);
        assignRoles(user, roles);

        auditService.record(AuditAction.USER_CREATE, actor, "USER", user.getId(), user.getUsername(),
                Map.of("roles", roles.stream().map(RoleEntity::getCode).sorted().toList()));

        log.info("User created username={} by={}", username, actor.getUsername());
        return UserCreatedResponse.of(user, roles, generated ? rawPassword : null);
    }

    @Transactional
    public UserSummaryResponse update(UUID id, UserUpdateRequest request, UserPrincipal actor) {
        UserEntity user = requireUser(id);
        String displayName = request.displayName().trim();
        if (!displayName.equals(user.getDisplayName())) {
            user.setDisplayName(displayName);
            user = userRepository.save(user);
            auditService.record(AuditAction.USER_UPDATE, actor, "USER", user.getId(), user.getUsername(),
                    Map.of("field", "displayName"));
            log.info("User updated username={} by={}", user.getUsername(), actor.getUsername());
        }
        return UserSummaryResponse.from(user, loadRoles(user.getId()));
    }

    @Transactional
    public UserSummaryResponse updateRoles(UUID id, UserRolesUpdateRequest request, UserPrincipal actor) {
        UserEntity user = requireUser(id);
        List<RoleEntity> roles = resolveRoles(request.roles());

        if (isSelf(actor, user) && roles.stream().noneMatch(role -> "ADMIN".equals(role.getCode()))) {
            throw new ForbiddenOperationException(ErrorCode.USER_SELF_OPERATION_FORBIDDEN,
                    "You cannot remove the ADMIN role from your own account.");
        }

        List<String> previousRoles = loadRoles(user.getId()).stream().map(RoleEntity::getCode).sorted().toList();
        userRoleRepository.deleteAllForUser(user.getId());
        userRoleRepository.flush();
        assignRoles(user, roles);

        List<String> newRoleCodes = roles.stream().map(RoleEntity::getCode).sorted().toList();
        auditService.record(AuditAction.ROLE_CHANGE, actor, "USER", user.getId(), user.getUsername(),
                Map.of("roles", newRoleCodes, "previousRoles", previousRoles));
        log.info("Roles updated username={} roles={} by={}", user.getUsername(), newRoleCodes, actor.getUsername());
        return UserSummaryResponse.from(user, loadRoles(user.getId()));
    }

    @Transactional
    public UserSummaryResponse setEnabled(UUID id, boolean enabled, UserPrincipal actor) {
        UserEntity user = requireUser(id);
        if (!enabled && isSelf(actor, user)) {
            throw new ForbiddenOperationException(ErrorCode.USER_SELF_OPERATION_FORBIDDEN,
                    "You cannot disable your own account.");
        }
        if (user.isEnabled() == enabled) {
            return UserSummaryResponse.from(user, loadRoles(user.getId()));
        }
        user.setEnabled(enabled);
        user = userRepository.save(user);
        if (!enabled) {
            sessionRegistryService.expireSessions(user.getId());
        }
        auditService.record(enabled ? AuditAction.USER_ENABLE : AuditAction.USER_DISABLE,
                actor, "USER", user.getId(), user.getUsername(), Map.of());
        log.info("User {} username={} by={}", enabled ? "enabled" : "disabled",
                user.getUsername(), actor.getUsername());
        return UserSummaryResponse.from(user, loadRoles(user.getId()));
    }

    @Transactional
    public UserPasswordResetResponse resetPassword(UUID id, UserPasswordResetRequest request, UserPrincipal actor) {
        UserEntity user = requireUser(id);
        if (isSelf(actor, user)) {
            throw new ForbiddenOperationException(ErrorCode.USER_SELF_OPERATION_FORBIDDEN,
                    "Use the authenticated change-password flow for your own account.");
        }

        boolean generated = !StringUtils.hasText(request.password());
        String rawPassword = generated ? generatePassword() : request.password();
        passwordPolicy.validate(rawPassword, user.getUsername());

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setMustChangePassword(true);
        user = userRepository.save(user);
        sessionRegistryService.expireSessions(user.getId());

        auditService.record(AuditAction.USER_PASSWORD_RESET, actor, "USER", user.getId(), user.getUsername(),
                Map.of("generated", generated));
        log.info("Password reset username={} generated={} by={}", user.getUsername(), generated,
                actor.getUsername());
        return new UserPasswordResetResponse(generated, generated ? rawPassword : null, user.isMustChangePassword());
    }

    public UserEntity requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found: " + id));
    }

    private List<RoleEntity> loadRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream().map(UserRoleEntity::getRole).toList();
    }

    private List<RoleEntity> resolveRoles(List<String> roleCodes) {
        Set<String> distinct = new LinkedHashSet<>();
        for (String code : roleCodes) {
            if (!StringUtils.hasText(code)) {
                throw new ResourceNotFoundException(ErrorCode.USER_ROLE_INVALID, "Role code must not be blank.");
            }
            distinct.add(code.trim());
        }
        List<RoleEntity> roles = new ArrayList<>();
        for (String code : distinct) {
            RoleEntity role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_ROLE_INVALID,
                            "Unknown role: " + code));
            roles.add(role);
        }
        if (roles.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.USER_ROLE_INVALID, "At least one role is required.");
        }
        return roles;
    }

    private void assignRoles(UserEntity user, List<RoleEntity> roles) {
        for (RoleEntity role : roles) {
            userRoleRepository.save(new UserRoleEntity(user, role));
        }
    }

    private boolean isSelf(UserPrincipal actor, UserEntity user) {
        return actor.getId().equals(user.getId());
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static Specification<UserEntity> querySpec(String q, Boolean enabled, String roleCode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("displayName")), pattern)));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            if (roleCode != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                var userRoleRoot = subquery.from(UserRoleEntity.class);
                subquery.select(userRoleRoot.get("user").get("id"))
                        .where(cb.equal(userRoleRoot.get("role").get("code"), roleCode));
                predicates.add(root.get("id").in(subquery));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
