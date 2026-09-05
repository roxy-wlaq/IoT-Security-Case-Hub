package com.company.casehub.project.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.project.dto.ProjectCreateRequest;
import com.company.casehub.project.dto.ProjectResponse;
import com.company.casehub.project.dto.ProjectSummaryResponse;
import com.company.casehub.project.dto.ProjectUpdateRequest;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectCoordinatorEntity;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.entity.ProjectStandardEntity;
import com.company.casehub.project.entity.ProjectStatus;
import com.company.casehub.project.repository.ProjectCoordinatorRepository;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.project.repository.ProjectStandardRepository;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectCoordinatorRepository coordinatorRepository;
    private final ProjectStandardRepository standardRepository;
    private final UserRepository userRepository;
    private final StandardTaskTypeRepository standardTaskTypeRepository;
    private final ProjectAccessPolicy accessPolicy;
    private final UserRoleRepository userRoleRepository;
    private final com.company.casehub.audit.service.AuditService auditService;

    public ProjectService(ProjectRepository projectRepository,
                           ProjectCoordinatorRepository coordinatorRepository,
                           ProjectStandardRepository standardRepository,
                           UserRepository userRepository,
                           StandardTaskTypeRepository standardTaskTypeRepository,
                           ProjectAccessPolicy accessPolicy,
                           UserRoleRepository userRoleRepository,
                           com.company.casehub.audit.service.AuditService auditService) {
        this.projectRepository = projectRepository;
        this.coordinatorRepository = coordinatorRepository;
        this.standardRepository = standardRepository;
        this.userRepository = userRepository;
        this.standardTaskTypeRepository = standardTaskTypeRepository;
        this.accessPolicy = accessPolicy;
        this.userRoleRepository = userRoleRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request, UserPrincipal principal) {
        List<StandardTaskTypeEntity> standards = resolveStandards(request.standardTaskTypeIds());
        UserEntity creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        UUID coordinatorId = request.primaryCoordinatorId() == null ? principal.getId() : request.primaryCoordinatorId();
        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Coordinator not found"));
        requireCoordinator(coordinatorId);

        ProjectEntity project = new ProjectEntity();
        project.setProjectNumber(generateProjectNumber());
        project.setProjectName(request.projectName().trim());
        project.setDeviceName(request.deviceName().trim());
        project.setGenerationMode(request.generationMode() == null ? GenerationMode.FULL : request.generationMode());
        project.setStatus(ProjectStatus.DRAFT);
        project.setCreatedBy(creator);
        project = projectRepository.save(project);
        replaceStandards(project, standards);
        replacePrimaryCoordinator(project, coordinator);
        auditService.record(com.company.casehub.audit.entity.AuditAction.PROJECT_CREATE, principal,
                com.company.casehub.audit.entity.AuditResourceType.PROJECT, project.getId(),
                project.getProjectNumber(), java.util.Map.of(
                        "projectName", project.getProjectName(),
                        "generationMode", project.getGenerationMode().name()));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> list(UserPrincipal principal) {
        List<ProjectEntity> projects = accessPolicy.isAdmin(principal)
                ? projectRepository.findAllByOrderByCreatedAtDesc()
                : projectRepository.findAllVisibleToCoordinator(principal.getId());
        return projects.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UserPrincipal principal) {
        accessPolicy.requireView(projectId, principal);
        return toResponse(requireProject(projectId));
    }

    @Transactional
    public ProjectResponse update(UUID projectId, ProjectUpdateRequest request, UserPrincipal principal) {
        accessPolicy.requireManage(projectId, principal);
        ProjectEntity project = requireProject(projectId);
        project.setProjectName(request.projectName().trim());
        project.setDeviceName(request.deviceName().trim());
        if (request.generationMode() != null) {
            project.setGenerationMode(request.generationMode());
        }
        if (request.standardTaskTypeIds() != null) {
            replaceStandards(project, resolveStandards(request.standardTaskTypeIds()));
        }
        if (request.primaryCoordinatorId() != null) {
            UserEntity coordinator = userRepository.findById(request.primaryCoordinatorId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Coordinator not found"));
            requireCoordinator(coordinator.getId());
            replacePrimaryCoordinator(project, coordinator);
        }
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse changeStatus(UUID projectId, ProjectStatus status, UserPrincipal principal) {
        accessPolicy.requireManage(projectId, principal);
        ProjectEntity project = requireProject(projectId);
        ProjectStatus previous = project.getStatus();
        project.setStatus(status);
        ProjectResponse response = toResponse(projectRepository.save(project));
        if (status == ProjectStatus.ARCHIVED && previous != ProjectStatus.ARCHIVED) {
            auditService.record(com.company.casehub.audit.entity.AuditAction.PROJECT_ARCHIVE, principal,
                    com.company.casehub.audit.entity.AuditResourceType.PROJECT, project.getId(),
                    project.getProjectNumber(), java.util.Map.of(
                            "fromStatus", previous.name(), "toStatus", status.name()));
        }
        return response;
    }

    private ProjectEntity requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found: " + projectId));
    }

    private List<StandardTaskTypeEntity> resolveStandards(List<UUID> ids) {
        if (ids == null || ids.isEmpty() || new HashSet<>(ids).size() != ids.size()) {
            throw new ConflictException(ErrorCode.PROJECT_STANDARD_INVALID, "Project must contain unique standards");
        }
        List<StandardTaskTypeEntity> standards = standardTaskTypeRepository.findAllById(ids);
        if (standards.size() != ids.size() || standards.stream().anyMatch(s -> !s.isEnabled())) {
            throw new ConflictException(ErrorCode.PROJECT_STANDARD_INVALID, "One or more standards are missing or disabled");
        }
        return ids.stream().map(id -> standards.stream().filter(s -> s.getId().equals(id)).findFirst().orElseThrow()).toList();
    }

    private void replaceStandards(ProjectEntity project, List<StandardTaskTypeEntity> standards) {
        standardRepository.deleteAll(project.getStandards());
        project.getStandards().clear();
        for (StandardTaskTypeEntity standard : standards) {
            ProjectStandardEntity link = new ProjectStandardEntity();
            link.setProject(project);
            link.setStandardTaskType(standard);
            project.getStandards().add(link);
        }
        standardRepository.saveAll(project.getStandards());
    }

    private void replacePrimaryCoordinator(ProjectEntity project, UserEntity coordinator) {
        coordinatorRepository.deleteAll(project.getCoordinators());
        project.getCoordinators().clear();
        ProjectCoordinatorEntity link = new ProjectCoordinatorEntity();
        link.setProject(project);
        link.setUser(coordinator);
        link.setPrimary(true);
        project.getCoordinators().add(link);
        coordinatorRepository.save(link);
    }

    private void requireCoordinator(UUID userId) {
        if (userRoleRepository.findByUserId(userId).stream().noneMatch(userRole ->
                "TEST_COORDINATOR".equals(userRole.getRole().getCode())
                        || "ADMIN".equals(userRole.getRole().getCode()))) {
            throw new ConflictException(ErrorCode.PROJECT_PRIMARY_COORDINATOR_CONFLICT,
                    "Primary Coordinator must have the TEST_COORDINATOR role");
        }
    }

    private String generateProjectNumber() {
        String value;
        do {
            value = "PRJ-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        } while (projectRepository.existsByProjectNumber(value));
        return value;
    }

    private ProjectSummaryResponse toSummary(ProjectEntity project) {
        return new ProjectSummaryResponse(project.getId(), project.getProjectNumber(), project.getProjectName(),
                project.getDeviceName(), project.getGenerationMode(), project.getStatus(), project.getCreatedAt());
    }

    private ProjectResponse toResponse(ProjectEntity project) {
        List<ProjectResponse.CoordinatorResponse> coordinators = project.getCoordinators().stream()
                .map(c -> new ProjectResponse.CoordinatorResponse(c.getUser().getId(), c.getUser().getUsername(),
                        c.getUser().getDisplayName(), c.isPrimary()))
                .toList();
        return new ProjectResponse(project.getId(), project.getProjectNumber(), project.getProjectName(),
                project.getDeviceName(), project.getGenerationMode(), project.getStatus(),
                project.getCreatedBy().getId(),
                project.getStandards().stream().map(s -> s.getStandardTaskType().getId()).toList(),
                coordinators, project.getCreatedAt(), project.getUpdatedAt());
    }
}
