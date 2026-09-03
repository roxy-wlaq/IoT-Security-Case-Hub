package com.company.casehub.execution.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.execution.dto.RelationUpdateRequest;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseTriggerEntity;
import com.company.casehub.execution.entity.RelationStatus;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseTriggerRepository;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.repository.DecisionPointRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelationUpdateService {
    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final ProjectTestCaseTriggerRepository triggerRepository;
    private final DecisionPointRepository decisionPointRepository;

    public RelationUpdateService(ProjectTestCaseRepository testCaseRepository,
                                 ProjectTestCaseAssigneeRepository assigneeRepository,
                                 ProjectTestCaseTriggerRepository triggerRepository,
                                 DecisionPointRepository decisionPointRepository) {
        this.testCaseRepository = testCaseRepository;
        this.assigneeRepository = assigneeRepository;
        this.triggerRepository = triggerRepository;
        this.decisionPointRepository = decisionPointRepository;
    }

    @Transactional
    public void update(UUID sourceId, RelationUpdateRequest request, UserPrincipal principal) {
        ProjectTestCaseEntity source = testCaseRepository.findByIdForUpdate(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        if (!assigneeRepository.existsByProjectTestCaseIdAndUserId(sourceId, principal.getId())) {
            throw new ForbiddenOperationException(ErrorCode.EXECUTION_FORBIDDEN, "The case is not assigned to you");
        }
        if (request.action() == com.company.casehub.execution.entity.RelationUpdateAction.KEEP_ORIGINAL_TARGET) return;
        if (request.sourceDecisionPointId() == null || request.targetProjectTestCaseId() == null) {
            throw new com.company.casehub.common.exception.ValidationException(ErrorCode.RELATION_ACTION_INVALID, "A Decision Point and target are required");
        }
        DecisionPointEntity point = decisionPointRepository.findByIdAndTestCaseVersionId(request.sourceDecisionPointId(), source.getTestCaseVersion().getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EXECUTION_DECISION_NOT_FOUND, "Decision Point not found"));
        ProjectTestCaseEntity target = testCaseRepository.findById(request.targetProjectTestCaseId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Target Project Test Case not found"));
        if (!target.getProject().getId().equals(source.getProject().getId())) {
            throw new com.company.casehub.common.exception.ValidationException(ErrorCode.RELATION_ACTION_INVALID, "Target must be in the same Project");
        }
        var action = request.action();
        if (action == com.company.casehub.execution.entity.RelationUpdateAction.DETACH_TARGET) {
            triggerRepository.findBySourceProjectTestCaseId(sourceId).stream()
                    .filter(t -> t.getSourceDecisionPoint().getId().equals(point.getId()) && t.getTargetProjectTestCase().getId().equals(target.getId()))
                    .forEach(triggerRepository::delete);
        } else {
            if (!triggerRepository.existsBySourceProjectTestCaseIdAndSourceDecisionPointIdAndTargetProjectTestCaseId(sourceId, point.getId(), target.getId())) {
                ProjectTestCaseTriggerEntity trigger = new ProjectTestCaseTriggerEntity(); trigger.setSourceProjectTestCase(source); trigger.setSourceTestCaseVersion(source.getTestCaseVersion()); trigger.setSourceDecisionPoint(point); trigger.setTargetProjectTestCase(target); triggerRepository.save(trigger);
            }
        }
        boolean connected = target.isRoot() || !triggerRepository.findByTargetProjectTestCaseId(target.getId()).isEmpty();
        target.setRelationStatus(connected ? RelationStatus.CONNECTED : RelationStatus.FLOATING);
    }
}
