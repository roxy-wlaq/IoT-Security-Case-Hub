package com.company.casehub.execution.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.execution.dto.AssigneeRequest;
import com.company.casehub.execution.dto.BulkAssignRequest;
import com.company.casehub.execution.dto.ProjectTestCaseResponse;
import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseSourceRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectTestPlanService {

    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseSourceRepository sourceRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final ProjectRepository projectRepository;
    private final MasterTestCaseRepository masterRepository;
    private final TestCaseVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final ProjectAccessPolicy accessPolicy;

    public ProjectTestPlanService(ProjectTestCaseRepository testCaseRepository,
                                  ProjectTestCaseSourceRepository sourceRepository,
                                  ProjectTestCaseAssigneeRepository assigneeRepository,
                                  ProjectRepository projectRepository,
                                  MasterTestCaseRepository masterRepository,
                                  TestCaseVersionRepository versionRepository,
                                  UserRepository userRepository,
                                  UserRoleRepository userRoleRepository,
                                  RoleRepository roleRepository,
                                  ProjectAccessPolicy accessPolicy) {
        this.testCaseRepository = testCaseRepository;
        this.sourceRepository = sourceRepository;
        this.assigneeRepository = assigneeRepository;
        this.projectRepository = projectRepository;
        this.masterRepository = masterRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public ProjectTestCaseResponse addMasterCase(UUID projectId, UUID masterId,
                                                  ProjectTestCaseSourceType source, UserPrincipal principal) {
        accessPolicy.requireManage(projectId, principal);
        ProjectEntity project = requireProject(projectId);
        MasterTestCaseEntity master = masterRepository.findById(masterId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Master Test Case not found"));
        TestCaseVersionEntity version = resolveCurrentPublishedVersion(masterId);
        ProjectTestCaseEntity entity = testCaseRepository.findForUpdate(projectId, masterId).orElse(null);
        if (entity != null && !entity.isRemoved()) {
            throw new ConflictException(ErrorCode.PROJECT_TEST_CASE_DUPLICATE, "Project Test Case already exists");
        }
        UserEntity actor = requireUser(principal.getId());
        if (entity == null) {
            entity = new ProjectTestCaseEntity();
            entity.setProject(project);
            entity.setMasterTestCase(master);
            entity.setTestCaseVersion(version);
            entity.setCreatedBy(actor);
            entity.setRoot(source != ProjectTestCaseSourceType.PROGRESSIVE);
        } else {
            entity.setTestCaseVersion(version);
            entity.setRemoved(false);
        }
        entity.setLastModifiedBy(actor);
        entity.setLastModifiedAt(Instant.now());
        entity = testCaseRepository.saveAndFlush(entity);
        if (source != null && !sourceRepository.existsByProjectTestCaseIdAndSourceType(entity.getId(), source)) {
            ProjectTestCaseSourceEntity sourceEntity = new ProjectTestCaseSourceEntity();
            sourceEntity.setProjectTestCase(entity);
            sourceEntity.setSourceType(source);
            sourceRepository.save(sourceEntity);
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ProjectTestCaseResponse> list(UUID projectId, UserPrincipal principal) {
        accessPolicy.requireManage(projectId, principal);
        return testCaseRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectTestCaseResponse remove(UUID id, UserPrincipal principal) {
        ProjectTestCaseEntity entity = requireCase(id);
        accessPolicy.requireManage(entity.getProject().getId(), principal);
        entity.setRemoved(true);
        entity.setLastModifiedBy(requireUser(principal.getId()));
        entity.setLastModifiedAt(Instant.now());
        return toResponse(testCaseRepository.save(entity));
    }

    @Transactional
    public ProjectTestCaseResponse restore(UUID id, UserPrincipal principal) {
        ProjectTestCaseEntity entity = requireCase(id);
        accessPolicy.requireManage(entity.getProject().getId(), principal);
        entity.setRemoved(false);
        entity.setLastModifiedBy(requireUser(principal.getId()));
        entity.setLastModifiedAt(Instant.now());
        return toResponse(testCaseRepository.save(entity));
    }

    @Transactional
    public ProjectTestCaseResponse assign(UUID id, AssigneeRequest request, UserPrincipal principal) {
        ProjectTestCaseEntity entity = requireCase(id);
        accessPolicy.requireManage(entity.getProject().getId(), principal);
        UserEntity user = requireTester(request.userId());
        if (!assigneeRepository.existsByProjectTestCaseIdAndUserId(id, user.getId())) {
            ProjectTestCaseAssigneeEntity assignment = new ProjectTestCaseAssigneeEntity();
            assignment.setProjectTestCase(entity);
            assignment.setUser(user);
            assignment.setAssignedAt(Instant.now());
            assigneeRepository.save(assignment);
        }
        return toResponse(entity);
    }

    @Transactional
    public List<ProjectTestCaseResponse> bulkAssign(BulkAssignRequest request, UserPrincipal principal) {
        for (UUID id : request.projectTestCaseIds()) {
            for (UUID userId : request.userIds()) {
                assign(id, new AssigneeRequest(userId), principal);
            }
        }
        return request.projectTestCaseIds().stream().map(this::requireCase).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TestCaseVersionEntity resolveCurrentPublishedVersion(UUID masterId) {
        return versionRepository.findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(masterId).stream()
                .filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .findFirst()
                .orElseThrow(() -> new ConflictException(ErrorCode.PROJECT_TEST_CASE_VERSION_INVALID,
                        "No current Published Version for Master Test Case: " + masterId));
    }

    private UserEntity requireTester(UUID userId) {
        UserEntity user = requireUser(userId);
        RoleEntity tester = roleRepository.findByCode("TESTER")
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "TESTER role not found"));
        boolean hasTester = userRoleRepository.findByUserId(userId).stream()
                .anyMatch(role -> role.getRole().getId().equals(tester.getId()));
        if (!hasTester) {
            throw new ConflictException(ErrorCode.PROJECT_TEST_CASE_ASSIGNEE_INVALID, "Assignee must have TESTER role");
        }
        return user;
    }

    private ProjectEntity requireProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found"));
    }

    private ProjectTestCaseEntity requireCase(UUID id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
    }

    private UserEntity requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private ProjectTestCaseResponse toResponse(ProjectTestCaseEntity entity) {
        List<ProjectTestCaseSourceType> sources = sourceRepository.findAll().stream()
                .filter(s -> s.getProjectTestCase().getId().equals(entity.getId()))
                .map(ProjectTestCaseSourceEntity::getSourceType).toList();
        List<ProjectTestCaseResponse.AssigneeResponse> assignees = assigneeRepository.findByProjectTestCaseId(entity.getId()).stream()
                .map(a -> new ProjectTestCaseResponse.AssigneeResponse(a.getUser().getId(), a.getUser().getUsername(),
                        a.getUser().getDisplayName(), a.getFirstViewedAt())).toList();
        return new ProjectTestCaseResponse(entity.getId(), entity.getProject().getId(), entity.getMasterTestCase().getId(),
                entity.getTestCaseVersion().getId(), entity.getMasterTestCase().getCaseCode(), entity.getExecutionStatus(),
                entity.getRelationStatus(), entity.isRemoved(), sources, assignees, entity.getLastModifiedAt());
    }
}
