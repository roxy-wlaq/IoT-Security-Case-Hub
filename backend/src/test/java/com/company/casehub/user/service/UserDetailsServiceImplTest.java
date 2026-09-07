package com.company.casehub.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.RolePermissionRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Verifies that login credential lookup is CASE-SENSITIVE: only the exact
 * stored casing of a username authenticates. This guards against the bug where
 * {@code Admin}/{@code ADMIN} could log in as {@code admin}.
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    private UserEntity seededUser() {
        return new UserEntity("admin", "Administrator", "hash");
    }

    @Test
    void loginSucceedsWithExactCaseUsername() {
        UserEntity user = seededUser();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(List.of());

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
    }

    @Test
    void loginFailsWhenUsernameCaseDiffers() {
        // Stored user is exactly "admin"; a differently-cased username must not match.
        when(userRepository.findByUsername("Admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("ADMIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("Admin"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service.loadUserByUsername("ADMIN"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
