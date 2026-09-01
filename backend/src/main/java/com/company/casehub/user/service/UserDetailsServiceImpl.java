package com.company.casehub.user.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.user.entity.PermissionEntity;
import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.RolePermissionEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.user.repository.RolePermissionRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link UserPrincipal} (with effective roles + permissions) for Spring
 * Security authentication. Throws {@link UsernameNotFoundException} for unknown
 * users so the provider maps it to invalid-credentials.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        for (UserRoleEntity ur : userRoleRepository.findByUserId(user.getId())) {
            RoleEntity role = ur.getRole();
            roles.add(role.getCode());
            for (RolePermissionEntity rp : rolePermissionRepository.findByRoleId(role.getId())) {
                PermissionEntity permission = rp.getPermission();
                permissions.add(permission.getCode());
            }
        }
        return UserPrincipal.from(user, roles, permissions);
    }

    @Transactional(readOnly = true)
    public UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found: " + userId));
    }
}
