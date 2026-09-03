package com.company.casehub.execution.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.execution.dto.MyCaseResponse;
import com.company.casehub.execution.dto.MyProjectResponse;
import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectCoordinatorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyTestQueryService {

    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final ProjectCoordinatorRepository coordinatorRepository;

    public MyTestQueryService(ProjectTestCaseRepository testCaseRepository,
                              ProjectTestCaseAssigneeRepository assigneeRepository,
                              ProjectCoordinatorRepository coordinatorRepository) {
        this.testCaseRepository = testCaseRepository;
        this.assigneeRepository = assigneeRepository;
        this.coordinatorRepository = coordinatorRepository;
    }

    @Transactional(readOnly = true)
    public List<MyProjectResponse> listMyProjects(UserPrincipal principal) {
        if (principal.getRoles().contains("ADMIN")) {
            return testCaseRepository.findAll().stream().map(ProjectTestCaseEntity::getProject)
                    .distinct().map(this::toProject).toList();
        }
        List<ProjectEntity> assigned = testCaseRepository.findProjectsAssignedTo(principal.getId());
        return assigned.stream().filter(project -> coordinatorRepository.existsByProjectIdAndUserId(project.getId(), principal.getId())
                        || testCaseRepository.existsAssignmentInProject(project.getId(), principal.getId()))
                .map(this::toProject).toList();
    }

    @Transactional(readOnly = true)
    public List<MyCaseResponse> listMyCases(UserPrincipal principal) {
        return assigneeRepository.findByUserId(principal.getId()).stream()
                .map(assignment -> toCase(assignment.getProjectTestCase(), assignment)).toList();
    }

    @Transactional(readOnly = true)
    public List<MyCaseResponse> listProjectCases(UUID projectId, UserPrincipal principal) {
        boolean member = principal.getRoles().contains("ADMIN")
                || coordinatorRepository.existsByProjectIdAndUserId(projectId, principal.getId())
                || testCaseRepository.existsAssignmentInProject(projectId, principal.getId());
        if (!member) {
            throw new ForbiddenOperationException(ErrorCode.PROJECT_ACCESS_FORBIDDEN, "You are not a member of this Project");
        }
        return testCaseRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(entity -> toCase(entity, assigneeRepository.findByProjectTestCaseIdAndUserId(entity.getId(), principal.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public MyCaseResponse markViewed(UUID projectTestCaseId, UserPrincipal principal) {
        ProjectTestCaseAssigneeEntity assignment = assigneeRepository.findByProjectTestCaseIdAndUserId(
                        projectTestCaseId, principal.getId())
                .orElseThrow(() -> new ForbiddenOperationException(ErrorCode.PROJECT_TEST_CASE_ACCESS_FORBIDDEN,
                        "The case is not assigned to you"));
        if (assignment.getFirstViewedAt() == null) {
            assignment.setFirstViewedAt(Instant.now());
            assigneeRepository.save(assignment);
        }
        return toCase(assignment.getProjectTestCase(), assignment);
    }

    private MyProjectResponse toProject(ProjectEntity project) {
        return new MyProjectResponse(project.getId(), project.getProjectNumber(), project.getProjectName(),
                project.getDeviceName(), project.getGenerationMode(), project.getStatus());
    }

    private MyCaseResponse toCase(ProjectTestCaseEntity entity, ProjectTestCaseAssigneeEntity assignment) {
        boolean assigned = assignment != null;
        return new MyCaseResponse(entity.getId(), entity.getProject().getId(), entity.getProject().getProjectNumber(),
                entity.getMasterTestCase().getId(), entity.getTestCaseVersion().getId(), entity.getMasterTestCase().getCaseCode(),
                entity.getExecutionStatus(), entity.getRelationStatus(), entity.isRemoved(), assigned,
                assigned && assignment.getFirstViewedAt() == null, !assigned, assigned ? assignment.getFirstViewedAt() : null);
    }
}
