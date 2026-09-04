package com.company.casehub.execution.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.customcase.repository.ProjectCustomTestCaseRepository;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.execution.dto.ExecutionResponse;
import com.company.casehub.execution.entity.BranchOutcomeEntity;
import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.ProjectDecisionSelectionEntity;
import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.entity.ProjectTestCaseTriggerEntity;
import com.company.casehub.execution.entity.RelationStatus;
import com.company.casehub.execution.repository.BranchOutcomeRepository;
import com.company.casehub.execution.repository.ProjectDecisionSelectionRepository;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseSourceRepository;
import com.company.casehub.execution.repository.ProjectTestCaseTriggerRepository;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TransitionEntity;
import com.company.casehub.testcase.entity.TransitionTargetEntity;
import com.company.casehub.testcase.entity.TransitionType;
import com.company.casehub.testcase.repository.DecisionPointRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.service.DagValidationService;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressiveRuntimeService {
    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseSourceRepository sourceRepository;
    private final ProjectTestCaseTriggerRepository triggerRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final ProjectDecisionSelectionRepository selectionRepository;
    private final BranchOutcomeRepository outcomeRepository;
    private final DecisionPointRepository decisionPointRepository;
    private final TestCaseVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final ProjectCustomTestCaseRepository customCaseRepository;
    private final DagValidationService dagValidationService;

    public ProgressiveRuntimeService(ProjectTestCaseRepository testCaseRepository,
                                     ProjectTestCaseSourceRepository sourceRepository,
                                     ProjectTestCaseTriggerRepository triggerRepository,
                                     ProjectTestCaseAssigneeRepository assigneeRepository,
                                     ProjectDecisionSelectionRepository selectionRepository,
                                     BranchOutcomeRepository outcomeRepository,
                                     DecisionPointRepository decisionPointRepository,
                                     TestCaseVersionRepository versionRepository,
                                     UserRepository userRepository,
                                     ProjectCustomTestCaseRepository customCaseRepository,
                                     DagValidationService dagValidationService) {
        this.testCaseRepository = testCaseRepository;
        this.sourceRepository = sourceRepository;
        this.triggerRepository = triggerRepository;
        this.assigneeRepository = assigneeRepository;
        this.selectionRepository = selectionRepository;
        this.outcomeRepository = outcomeRepository;
        this.decisionPointRepository = decisionPointRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.customCaseRepository = customCaseRepository;
        this.dagValidationService = dagValidationService;
    }

    @Transactional
    public ExecutionResponse complete(ProjectTestCaseEntity source, List<UUID> selectedIds, UserPrincipal principal) {
        TestCaseVersionEntity version = source.getTestCaseVersion();
        List<DecisionPointEntity> points = version != null
                ? decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(version.getId())
                : decisionPointRepository.findByCustomTestCaseIdOrderByDisplayOrderAscIdAsc(source.getCustomTestCase().getId());
        validateSelections(version != null ? version.getSelectionMode() : source.getCustomTestCase().getSelectionMode(), points, selectedIds);
        Set<UUID> selected = new HashSet<>(selectedIds);
        List<UUID> affected = new ArrayList<>();
        for (ProjectTestCaseTriggerEntity trigger : new ArrayList<>(triggerRepository.findBySourceProjectTestCaseId(source.getId()))) {
            affected.add(trigger.getTargetProjectTestCase().getId());
        }
        triggerRepository.findBySourceProjectTestCaseId(source.getId()).forEach(trigger -> triggerRepository.delete(trigger));
        selectionRepository.deleteByProjectTestCaseId(source.getId());
        outcomeRepository.deleteByProjectTestCaseId(source.getId());
        List<ExecutionResponse.BranchOutcomeResponse> responses = new ArrayList<>();
        for (DecisionPointEntity point : points) {
            if (!selected.contains(point.getId())) continue;
            ProjectDecisionSelectionEntity selection = new ProjectDecisionSelectionEntity();
            selection.setProjectTestCase(source); selection.setDecisionPoint(point); selectionRepository.save(selection);
            TransitionEntity transition = point.getTransition();
            if (transition == null) continue;
            dagValidationService.validateTransitionTargets(transition.getType(), transition.getTargets().stream().map(target ->
                    target.getTargetMasterTestCase() != null ? target.getTargetMasterTestCase().getId() :
                            target.getTargetCustomTestCase() == null ? null : target.getTargetCustomTestCase().getId()).toList());
            if (transition.getType() == TransitionType.NEXT_CASE && transition.getTargets().size() != 1) {
                throw new BusinessRuleException(ErrorCode.EXECUTION_SELECTION_INVALID, "NEXT_CASE must have exactly one target");
            }
            if (transition.getType() == TransitionType.NEXT_CASES && transition.getTargets().isEmpty()) {
                throw new BusinessRuleException(ErrorCode.EXECUTION_SELECTION_INVALID, "NEXT_CASES must have at least one target");
            }
            for (TransitionTargetEntity target : transition.getTargets()) {
                BranchOutcomeEntity outcome = new BranchOutcomeEntity();
                outcome.setProjectTestCase(source); outcome.setDecisionPoint(point); outcome.setTransitionType(transition.getType());
                outcome.setTargetMasterTestCase(target.getTargetMasterTestCase());
                outcome.setTargetCustomTestCase(target.getTargetCustomTestCase()); outcomeRepository.save(outcome);
                ProjectTestCaseEntity targetPtc = ensureTarget(source, point, target, principal);
                affected.add(targetPtc.getId());
                responses.add(new ExecutionResponse.BranchOutcomeResponse(point.getId(), transition.getType(),
                        target.getTargetMasterTestCase() == null ? null : target.getTargetMasterTestCase().getId(),
                        target.getTargetCustomTestCase() == null ? null : target.getTargetCustomTestCase().getId()));
            }
            if (transition.getTargets().isEmpty()) {
                BranchOutcomeEntity outcome = new BranchOutcomeEntity();
                outcome.setProjectTestCase(source); outcome.setDecisionPoint(point); outcome.setTransitionType(transition.getType()); outcomeRepository.save(outcome);
                responses.add(new ExecutionResponse.BranchOutcomeResponse(point.getId(), transition.getType(), null, null));
            }
        }
        source.setExecutionStatus(ExecutionStatus.COMPLETED);
        source.setLastModifiedBy(userRepository.findById(principal.getId()).orElseThrow());
        source.setLastModifiedAt(Instant.now());
        testCaseRepository.save(source);
        recalculate(affected);
        return new ExecutionResponse(source.getId(), source.getExecutionStatus(), selectedIds, responses, affected.stream().distinct().toList());
    }

    private ProjectTestCaseEntity ensureTarget(ProjectTestCaseEntity source, DecisionPointEntity point, TransitionTargetEntity targetLink, UserPrincipal principal) {
        UUID masterId = targetLink.getTargetMasterTestCase() == null ? null : targetLink.getTargetMasterTestCase().getId();
        UUID customId = targetLink.getTargetCustomTestCase() == null ? null : targetLink.getTargetCustomTestCase().getId();
        ProjectTestCaseEntity target = masterId != null
                ? testCaseRepository.findByProjectIdAndMasterTestCaseId(source.getProject().getId(), masterId).orElse(null)
                : testCaseRepository.findByProjectIdAndCustomTestCaseId(source.getProject().getId(), customId).orElse(null);
        if (target == null) {
            UserEntity actor = userRepository.findById(principal.getId()).orElseThrow();
            if (masterId != null) {
                TestCaseVersionEntity targetVersion = versionRepository.findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(masterId).stream()
                        .filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED).findFirst()
                        .orElseThrow(() -> new ConflictException(ErrorCode.PROJECT_TEST_CASE_VERSION_INVALID, "No current Published Version for target"));
                testCaseRepository.insertRuntimeTargetIfAbsent(UUID.randomUUID(), source.getProject().getId(), masterId, targetVersion.getId(), actor.getId());
                target = testCaseRepository.findByProjectIdAndMasterTestCaseId(source.getProject().getId(), masterId).orElseThrow();
            } else {
                customCaseRepository.findByIdAndProjectId(customId, source.getProject().getId()).orElseThrow(() -> new ConflictException(ErrorCode.CUSTOM_CASE_TARGET_INVALID, "Custom target is not in the source Project"));
                testCaseRepository.insertCustomRuntimeTargetIfAbsent(UUID.randomUUID(), source.getProject().getId(), customId, actor.getId());
                target = testCaseRepository.findByProjectIdAndCustomTestCaseId(source.getProject().getId(), customId).orElseThrow();
            }
        }
        target.setRemoved(false);
        if (!sourceRepository.existsByProjectTestCaseIdAndSourceType(target.getId(), ProjectTestCaseSourceType.PROGRESSIVE)) {
            ProjectTestCaseSourceEntity sourceEntity = new ProjectTestCaseSourceEntity(); sourceEntity.setProjectTestCase(target); sourceEntity.setSourceType(ProjectTestCaseSourceType.PROGRESSIVE); sourceRepository.save(sourceEntity);
        }
        for (ProjectTestCaseAssigneeEntity assignment : assigneeRepository.findByProjectTestCaseId(source.getId())) {
            if (!assigneeRepository.existsByProjectTestCaseIdAndUserId(target.getId(), assignment.getUser().getId())) {
                ProjectTestCaseAssigneeEntity inherited = new ProjectTestCaseAssigneeEntity(); inherited.setProjectTestCase(target); inherited.setUser(assignment.getUser()); inherited.setAssignedAt(Instant.now()); assigneeRepository.save(inherited);
            }
        }
        if (!triggerRepository.existsBySourceProjectTestCaseIdAndSourceDecisionPointIdAndTargetProjectTestCaseId(source.getId(), point.getId(), target.getId())) {
            ProjectTestCaseTriggerEntity trigger = new ProjectTestCaseTriggerEntity(); trigger.setSourceProjectTestCase(source); trigger.setSourceTestCaseVersion(source.getTestCaseVersion()); trigger.setSourceDecisionPoint(point); trigger.setTargetProjectTestCase(target); triggerRepository.save(trigger);
        }
        return target;
    }

    private void validateSelections(SelectionMode mode, List<DecisionPointEntity> points, List<UUID> ids) {
        if (ids == null || ids.isEmpty() || new HashSet<>(ids).size() != ids.size()) throw new BusinessRuleException(ErrorCode.EXECUTION_SELECTION_INVALID, "At least one unique Decision Point is required");
        Set<UUID> known = points.stream().map(DecisionPointEntity::getId).collect(java.util.stream.Collectors.toSet());
        if (!known.containsAll(ids) || (mode == SelectionMode.SINGLE && ids.size() != 1)) throw new BusinessRuleException(ErrorCode.EXECUTION_SELECTION_INVALID, "Decision Point selection does not match Selection Mode");
    }

    private void recalculate(List<UUID> affectedTargetIds) {
        affectedTargetIds.stream().distinct()
                .map(id -> testCaseRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .forEach(ptc -> {
                    boolean connected = ptc.isRoot() || !triggerRepository.findByTargetProjectTestCaseId(ptc.getId()).isEmpty();
                    ptc.setRelationStatus(connected ? RelationStatus.CONNECTED : RelationStatus.FLOATING);
                });
    }
}
