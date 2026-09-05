package com.company.casehub.auth.service;

import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial ADMIN user on first boot ONLY when an admin password is
 * supplied via {@code CASEHUB_BOOTSTRAP_ADMIN_PASSWORD}. No default password is
 * ever hardcoded (security requirement).
 *
 * <p>The bootstrap admin is created with {@code must_change_password = false}: the product
 * does NOT require a password change on first use. The {@code must_change_password} column
 * is retained as a DORMANT capability for a future "admin resets a user's password -> force
 * that user to change the temporary password on next login" flow, which is NOT implemented
 * in V1. Until that flow exists the field is never set to true and no forced-change gate is
 * shown in the UI.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapUserService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.company.casehub.audit.service.AuditService auditService;

    @Value("${casehub.bootstrap.admin-username:admin}")
    private String adminUsername;

    @Value("${casehub.bootstrap.admin-display-name:System Administrator}")
    private String adminDisplayName;

    @Value("${casehub.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.info("Bootstrap admin password not provided (set CASEHUB_BOOTSTRAP_ADMIN_PASSWORD); "
                    + "skipping initial ADMIN creation.");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Users already exist; skipping initial ADMIN creation.");
            return;
        }
        RoleEntity adminRole = roleRepository.findByCode("ADMIN").orElseThrow(() ->
                new IllegalStateException("ADMIN role missing from seed; Flyway V002 must run first."));

        UserEntity admin = new UserEntity(adminUsername, adminDisplayName,
                passwordEncoder.encode(adminPassword));
        admin.setMustChangePassword(false);
        userRepository.save(admin);
        userRoleRepository.save(new UserRoleEntity(admin, adminRole));
        // Phase 26: bootstrap ADMIN creation is the single authoritative role
        // mutation point in V1, so the frozen ROLE_CHANGE event is emitted here.
        auditService.record(com.company.casehub.audit.entity.AuditAction.ROLE_CHANGE,
                admin.getId(), admin.getUsername(), "USER", admin.getId().toString(), admin.getUsername(),
                java.util.Map.of("role", "ADMIN", "bootstrap", true));
        log.info("Initial ADMIN user '{}' created (must_change_password=false).", adminUsername);
    }
}
