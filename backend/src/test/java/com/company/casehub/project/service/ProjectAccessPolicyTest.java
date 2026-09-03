package com.company.casehub.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.project.entity.ProjectCoordinatorEntity;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectCoordinatorRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAccessPolicyTest {

    @Mock
    private ProjectCoordinatorRepository coordinatorRepository;

    @Test
    void adminCanManageAnyProject() {
        ProjectAccessPolicy policy = new ProjectAccessPolicy(coordinatorRepository);
        assertThat(policy.canManage(UUID.randomUUID(), principal("ADMIN"))).isTrue();
    }

    @Test
    void coordinatorCanManageOnlyAssignedProject() {
        ProjectAccessPolicy policy = new ProjectAccessPolicy(coordinatorRepository);
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(coordinatorRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);

        assertThat(policy.canManage(projectId, principal(userId, "TEST_COORDINATOR"))).isTrue();
        assertThat(policy.canManage(projectId, principal(UUID.randomUUID(), "TEST_COORDINATOR"))).isFalse();
    }

    @Test
    void coordinatorEntityCanExposePrimaryAssignment() {
        ProjectEntity project = new ProjectEntity();
        ProjectCoordinatorEntity assignment = new ProjectCoordinatorEntity();
        assignment.setPrimary(true);
        project.getCoordinators().add(assignment);
        assertThat(project.getCoordinators()).singleElement().extracting(ProjectCoordinatorEntity::isPrimary)
                .isEqualTo(true);
    }

    private static UserPrincipal principal(String... roles) {
        return new UserPrincipal(UUID.randomUUID(), "user", "hash", "User", true, false,
                Set.of(roles), Set.of("project:read", "project:update"));
    }

    private static UserPrincipal principal(UUID id, String... roles) {
        return new UserPrincipal(id, "user", "hash", "User", true, false,
                Set.of(roles), Set.of("project:read", "project:update"));
    }
}
