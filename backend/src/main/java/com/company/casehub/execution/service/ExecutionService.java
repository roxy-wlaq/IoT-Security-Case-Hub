package com.company.casehub.execution.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.evidence.repository.EvidenceRepository;
import com.company.casehub.execution.dto.CompleteExecutionRequest;
import com.company.casehub.execution.dto.ExecutionResponse;
import com.company.casehub.execution.dto.ExecutionDetailResponse;
import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.testcase.repository.DecisionPointRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionService {
    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final EvidenceRepository evidenceRepository;
    private final ProgressiveRuntimeService progressiveRuntimeService;
    private final DecisionPointRepository decisionPointRepository;
    private final ProjectAccessPolicy accessPolicy;

    public ExecutionService(ProjectTestCaseRepository testCaseRepository,
                            ProjectTestCaseAssigneeRepository assigneeRepository,
                            EvidenceRepository evidenceRepository,
                            ProgressiveRuntimeService progressiveRuntimeService,
                            DecisionPointRepository decisionPointRepository,
                            ProjectAccessPolicy accessPolicy) {
        this.testCaseRepository = testCaseRepository;
        this.assigneeRepository = assigneeRepository;
        this.evidenceRepository = evidenceRepository;
        this.progressiveRuntimeService = progressiveRuntimeService;
        this.decisionPointRepository = decisionPointRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public ExecutionDetailResponse detail(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = testCaseRepository.findById(ptcId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        if (!accessPolicy.canView(ptc.getProject().getId(), principal)) {
            throw new ForbiddenOperationException(ErrorCode.PROJECT_ACCESS_FORBIDDEN, "You are not a member of this Project");
        }
        var decisionPoints = ptc.getTestCaseVersion() != null
                ? decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(ptc.getTestCaseVersion().getId())
                : decisionPointRepository.findByCustomTestCaseIdOrderByDisplayOrderAscIdAsc(ptc.getCustomTestCase().getId());
        var decisions = decisionPoints.stream()
                .map(d -> new ExecutionDetailResponse.DecisionResponse(d.getId(), d.getDisplayOrder(), d.getName(),
                        d.getTransition() == null ? null : d.getTransition().getType().name(),
                        d.getTransition() == null ? java.util.List.of() : d.getTransition().getTargets().stream().filter(t -> t.getTargetMasterTestCase() != null).map(t -> t.getTargetMasterTestCase().getId()).toList(),
                        d.getTransition() == null ? java.util.List.of() : d.getTransition().getTargets().stream().filter(t -> t.getTargetCustomTestCase() != null).map(t -> t.getTargetCustomTestCase().getId()).toList()))
                .toList();
        return new ExecutionDetailResponse(ptc.getId(), ptc.getProject().getId(), ptc.getMasterTestCase() == null ? null : ptc.getMasterTestCase().getId(),
                ptc.getCustomTestCase() == null ? null : ptc.getCustomTestCase().getId(), ptc.getTestCaseVersion() == null ? null : ptc.getTestCaseVersion().getId(),
                ptc.getExecutionStatus(), ptc.getTestCaseVersion() != null ? ptc.getTestCaseVersion().getSelectionMode() : ptc.getCustomTestCase().getSelectionMode(),
                ptc.getTestCaseVersion() != null ? ptc.getTestCaseVersion().isEvidenceRequired() : ptc.getCustomTestCase().isEvidenceRequired(), decisions);
    }

    @Transactional
    public ProjectTestCaseEntity start(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = requireAssignedForUpdate(ptcId, principal);
        if (ptc.getExecutionStatus() == ExecutionStatus.COMPLETED) {
            throw new com.company.casehub.common.exception.ConflictException(ErrorCode.EXECUTION_INVALID_STATE, "Completed case must be reopened first");
        }
        ptc.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        return testCaseRepository.save(ptc);
    }

    @Transactional
    public ExecutionResponse complete(UUID ptcId, CompleteExecutionRequest request, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = requireAssignedForUpdate(ptcId, principal);
        if (ptc.getExecutionStatus() != ExecutionStatus.IN_PROGRESS) {
            throw new com.company.casehub.common.exception.ConflictException(ErrorCode.EXECUTION_INVALID_STATE, "Case must be IN_PROGRESS before completion");
        }
        if ((ptc.getTestCaseVersion() != null ? ptc.getTestCaseVersion().isEvidenceRequired() : ptc.getCustomTestCase().isEvidenceRequired())
                && evidenceRepository.countByProjectTestCaseId(ptcId) == 0) {
            throw new com.company.casehub.common.exception.BusinessRuleException(ErrorCode.EVIDENCE_REQUIRED, "Evidence is required before completion");
        }
        return progressiveRuntimeService.complete(ptc, request.selectedDecisionPointIds(), principal);
    }

    @Transactional
    public ProjectTestCaseEntity reopen(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = requireAssignedForUpdate(ptcId, principal);
        if (ptc.getExecutionStatus() != ExecutionStatus.COMPLETED) {
            throw new com.company.casehub.common.exception.ConflictException(ErrorCode.EXECUTION_INVALID_STATE, "Only a completed case can be reopened");
        }
        ptc.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        return testCaseRepository.save(ptc);
    }

    private ProjectTestCaseEntity requireAssignedForUpdate(UUID id, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = testCaseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        if (ptc.isRemoved() || !assigneeRepository.existsByProjectTestCaseIdAndUserId(id, principal.getId())) {
            throw new ForbiddenOperationException(ErrorCode.EXECUTION_FORBIDDEN, "The case is not assigned to you");
        }
        return ptc;
    }
}
