package com.company.casehub.project.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectCoordinatorRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProjectAccessPolicy {

    private final ProjectCoordinatorRepository coordinatorRepository;

    public ProjectAccessPolicy(ProjectCoordinatorRepository coordinatorRepository) {
        this.coordinatorRepository = coordinatorRepository;
    }

    public boolean isAdmin(UserPrincipal principal) {
        return principal != null && principal.getRoles().contains("ADMIN");
    }

    public boolean canManage(UUID projectId, UserPrincipal principal) {
        return isAdmin(principal) || principal != null
                && coordinatorRepository.existsByProjectIdAndUserId(projectId, principal.getId());
    }

    public boolean canView(UUID projectId, UserPrincipal principal) {
        return canManage(projectId, principal);
    }

    public void requireManage(UUID projectId, UserPrincipal principal) {
        if (!canManage(projectId, principal)) {
            throw new com.company.casehub.common.exception.ForbiddenOperationException(
                    com.company.casehub.common.exception.ErrorCode.PROJECT_ACCESS_FORBIDDEN,
                    "You cannot manage this Project: " + projectId);
        }
    }
}
