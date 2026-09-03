package com.company.casehub.execution.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.execution.dto.ProjectLogicGraphResponse;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseTriggerRepository;
import com.company.casehub.project.repository.ProjectCoordinatorRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectLogicGraphService {
    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final ProjectTestCaseTriggerRepository triggerRepository;
    private final ProjectCoordinatorRepository coordinatorRepository;

    public ProjectLogicGraphService(ProjectTestCaseRepository testCaseRepository,
                                    ProjectTestCaseAssigneeRepository assigneeRepository,
                                    ProjectTestCaseTriggerRepository triggerRepository,
                                    ProjectCoordinatorRepository coordinatorRepository) {
        this.testCaseRepository = testCaseRepository;
        this.assigneeRepository = assigneeRepository;
        this.triggerRepository = triggerRepository;
        this.coordinatorRepository = coordinatorRepository;
    }

    @Transactional(readOnly = true)
    public ProjectLogicGraphResponse graph(UUID projectId, UserPrincipal principal) {
        boolean member = principal.getRoles().contains("ADMIN")
                || coordinatorRepository.existsByProjectIdAndUserId(projectId, principal.getId())
                || assigneeRepository.existsByProjectTestCaseProjectIdAndUserId(projectId, principal.getId());
        if (!member) throw new ForbiddenOperationException(ErrorCode.PROJECT_ACCESS_FORBIDDEN, "You are not a member of this Project");
        var cases = testCaseRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .filter(c -> !c.isRemoved()).toList();
        var nodes = cases.stream().map(this::node).toList();
        var activeIds = cases.stream().map(ProjectTestCaseEntity::getId).collect(java.util.stream.Collectors.toSet());
        var edges = cases.stream().flatMap(c -> triggerRepository.findBySourceProjectTestCaseId(c.getId()).stream()
                .filter(t -> activeIds.contains(t.getTargetProjectTestCase().getId()))
                .map(t -> new ProjectLogicGraphResponse.Edge(t.getId(), c.getId(), t.getTargetProjectTestCase().getId(),
                        t.getSourceDecisionPoint().getId(), t.getSourceDecisionPoint().getName()))).toList();
        return new ProjectLogicGraphResponse(nodes, edges);
    }

    private ProjectLogicGraphResponse.Node node(ProjectTestCaseEntity c) {
        return new ProjectLogicGraphResponse.Node(c.getId(), c.getMasterTestCase().getId(), c.getMasterTestCase().getCaseCode(),
                c.getTestCaseVersion().getId(), c.getExecutionStatus(), c.getRelationStatus(), c.isRoot(),
                assigneeRepository.findByProjectTestCaseId(c.getId()).stream().map(a -> a.getUser().getDisplayName()).toList());
    }
}
