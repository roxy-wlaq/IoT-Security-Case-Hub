package com.company.casehub.project.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.project.dto.ProjectCapabilityRequest;
import com.company.casehub.project.dto.ProjectCapabilityResponse;
import com.company.casehub.project.entity.ProjectCapabilityEntity;
import com.company.casehub.project.entity.ProjectCapabilitySource;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectCapabilityRepository;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectCapabilityService {

    private final ProjectCapabilityRepository repository;
    private final ProjectRepository projectRepository;
    private final CapabilityRepository capabilityRepository;
    private final UserRepository userRepository;
    private final ProjectAccessPolicy accessPolicy;
    private final CapabilityEngine engine;

    public ProjectCapabilityService(ProjectCapabilityRepository repository,
                                    ProjectRepository projectRepository,
                                    CapabilityRepository capabilityRepository,
                                    UserRepository userRepository,
                                    ProjectAccessPolicy accessPolicy,
                                    CapabilityEngine engine) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.capabilityRepository = capabilityRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public List<ProjectCapabilityResponse> list(UUID projectId, UserPrincipal principal) {
        accessPolicy.requireView(projectId, principal);
        List<ProjectCapabilityEntity> rows = repository.findByProjectId(projectId);
        return capabilityRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(capability -> rows.stream().filter(row -> row.getCapability().getId().equals(capability.getId()))
                        .findFirst().map(this::toResponse)
                        .orElse(new ProjectCapabilityResponse(capability.getId(), capability.getParentId(),
                                capability.getCode(), capability.getName(), ProjectCapabilityValue.UNKNOWN,
                                null, false, null, null)))
                .toList();
    }

    @Transactional
    public ProjectCapabilityResponse setValue(UUID projectId, UUID capabilityId,
                                              ProjectCapabilityRequest request, UserPrincipal principal) {
        accessPolicy.requireManage(projectId, principal);
        if (request == null || request.value() == null
                || request.source() == null || request.source() == ProjectCapabilitySource.DERIVED) {
            throw new ConflictException(ErrorCode.PROJECT_CAPABILITY_INVALID, "Explicit capability value/source is invalid");
        }
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found: " + projectId));
        CapabilityEntity capability = capabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CAPABILITY_NOT_FOUND, "Capability not found: " + capabilityId));
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        ProjectCapabilityEntity row = repository.findByProjectIdAndCapabilityId(projectId, capabilityId)
                .orElseGet(ProjectCapabilityEntity::new);
        row.setProject(project);
        row.setCapability(capability);
        row.setValue(request.value());
        row.setSource(request.source());
        row.setDerived(false);
        row.setComment(request.comment());
        row.setUpdatedBy(user);
        ProjectCapabilityResponse response = toResponse(repository.save(row));
        engine.recalculateDerivedParents(projectId, capabilityId, user);
        return response;
    }

    private ProjectCapabilityResponse toResponse(ProjectCapabilityEntity row) {
        return new ProjectCapabilityResponse(row.getCapability().getId(), row.getCapability().getParentId(),
                row.getCapability().getCode(), row.getCapability().getName(), row.getValue(), row.getSource(),
                row.isDerived(), row.getComment(), row.getUpdatedAt());
    }
}
