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
 * ever hardcoded (security requirement). The created user must change password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapUserService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

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
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        userRoleRepository.save(new UserRoleEntity(admin, adminRole));
        log.info("Initial ADMIN user '{}' created (must_change_password=true).", adminUsername);
    }
}
