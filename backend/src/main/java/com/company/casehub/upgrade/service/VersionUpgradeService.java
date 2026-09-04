package com.company.casehub.upgrade.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.BranchOutcomeRepository;
import com.company.casehub.execution.repository.ProjectDecisionSelectionRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseTriggerRepository;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TransitionTargetEntity;
import com.company.casehub.testcase.repository.DecisionPointRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.upgrade.dto.VersionAvailabilityResponse;
import com.company.casehub.upgrade.dto.VersionDiffResponse;
import com.company.casehub.upgrade.dto.VersionUpgradeResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionUpgradeService {
    private final ProjectTestCaseRepository ptcRepository;
    private final TestCaseVersionRepository versionRepository;
    private final DecisionPointRepository decisionPointRepository;
    private final ProjectDecisionSelectionRepository selectionRepository;
    private final BranchOutcomeRepository outcomeRepository;
    private final ProjectTestCaseTriggerRepository triggerRepository;
    private final ProjectAccessPolicy accessPolicy;

    public VersionUpgradeService(ProjectTestCaseRepository ptcRepository, TestCaseVersionRepository versionRepository,
                                 DecisionPointRepository decisionPointRepository, ProjectDecisionSelectionRepository selectionRepository,
                                 BranchOutcomeRepository outcomeRepository, ProjectTestCaseTriggerRepository triggerRepository,
                                 ProjectAccessPolicy accessPolicy) {
        this.ptcRepository = ptcRepository; this.versionRepository = versionRepository; this.decisionPointRepository = decisionPointRepository;
        this.selectionRepository = selectionRepository; this.outcomeRepository = outcomeRepository; this.triggerRepository = triggerRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public VersionAvailabilityResponse availability(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = requirePtc(ptcId);
        accessPolicy.requireView(ptc.getProject().getId(), principal);
        if (ptc.getMasterTestCase() == null) return new VersionAvailabilityResponse(ptcId, null, null, null, false, ptc.getExecutionStatus(), new VersionDiffResponse(List.of(), false, true, null));
        TestCaseVersionEntity current = current(ptc.getMasterTestCase().getId());
        VersionDiffResponse diff = diff(ptc.getTestCaseVersion(), current);
        return new VersionAvailabilityResponse(ptcId, ptc.getMasterTestCase().getId(), ptc.getTestCaseVersion().getId(), current.getId(), !current.getId().equals(ptc.getTestCaseVersion().getId()), ptc.getExecutionStatus(), diff);
    }

    @Transactional
    public VersionUpgradeResponse keep(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = requirePtc(ptcId);
        accessPolicy.requireManage(ptc.getProject().getId(), principal);
        VersionAvailabilityResponse available = availability(ptcId, principal);
        return new VersionUpgradeResponse(ptcId, available.boundVersionId(), available.boundVersionId(), false, available.diff());
    }

    @Transactional
    public VersionUpgradeResponse upgrade(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = ptcRepository.findByIdForUpdate(ptcId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        accessPolicy.requireManage(ptc.getProject().getId(), principal);
        if (ptc.getMasterTestCase() == null) throw new ConflictException(ErrorCode.VERSION_UPGRADE_INCOMPATIBLE, "Custom Test Cases do not use library versions");
        TestCaseVersionEntity oldVersion = ptc.getTestCaseVersion();
        TestCaseVersionEntity current = current(ptc.getMasterTestCase().getId());
        VersionDiffResponse diff = diff(oldVersion, current);
        if (oldVersion.getId().equals(current.getId())) return new VersionUpgradeResponse(ptcId, oldVersion.getId(), current.getId(), false, diff);
        ensureReferencesRemainValid(ptc);
        ptc.setTestCaseVersion(current);
        ptcRepository.saveAndFlush(ptc);
        return new VersionUpgradeResponse(ptcId, oldVersion.getId(), current.getId(), true, diff);
    }

    private void ensureReferencesRemainValid(ProjectTestCaseEntity ptc) {
        // Historical selections/outcomes/triggers retain their old DecisionPoint FKs.
        // Upgrade never rewrites them to the new version; the old version remains immutable,
        // so these rows cannot become dangling. Future execution reads the new bound version.
        selectionRepository.findByProjectTestCaseId(ptc.getId()).forEach(selection -> {
            if (selection.getDecisionPoint() == null) throw new ConflictException(ErrorCode.VERSION_UPGRADE_INCOMPATIBLE, "Selection reference is invalid");
        });
        outcomeRepository.findByProjectTestCaseId(ptc.getId()).forEach(outcome -> {
            if (outcome.getDecisionPoint() == null) throw new ConflictException(ErrorCode.VERSION_UPGRADE_INCOMPATIBLE, "Outcome reference is invalid");
        });
        triggerRepository.findBySourceProjectTestCaseId(ptc.getId()).forEach(trigger -> {
            if (trigger.getSourceDecisionPoint() == null) throw new ConflictException(ErrorCode.VERSION_UPGRADE_INCOMPATIBLE, "Trigger reference is invalid");
        });
    }

    private VersionDiffResponse diff(TestCaseVersionEntity oldVersion, TestCaseVersionEntity current) {
        if (oldVersion == null || current == null) return new VersionDiffResponse(List.of(), false, true, null);
        List<String> fields = new ArrayList<>();
        if (!java.util.Objects.equals(oldVersion.getCaseName(), current.getCaseName())) fields.add("caseName");
        if (!java.util.Objects.equals(oldVersion.getTestPurpose(), current.getTestPurpose())) fields.add("purpose");
        if (!java.util.Objects.equals(oldVersion.getPreconditions(), current.getPreconditions())) fields.add("preconditions");
        if (oldVersion.getSelectionMode() != current.getSelectionMode()) fields.add("selectionMode");
        if (oldVersion.isEvidenceRequired() != current.isEvidenceRequired() || !java.util.Objects.equals(oldVersion.getEvidenceRequirement(), current.getEvidenceRequirement()) || !java.util.Objects.equals(oldVersion.getRemarkRequirement(), current.getRemarkRequirement())) fields.add("evidence/remark requirements");
        List<DecisionPointEntity> oldPoints = decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(oldVersion.getId());
        List<DecisionPointEntity> newPoints = decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(current.getId());
        boolean logicChanged = !signature(oldPoints).equals(signature(newPoints));
        if (logicChanged) fields.add("Decision Points / Transitions / Targets");
        return new VersionDiffResponse(fields, logicChanged, true, logicChanged ? "Logic-affecting upgrade: review Decision Points, Transitions and Targets before upgrading." : null);
    }

    private String signature(List<DecisionPointEntity> points) { return points.stream().sorted(Comparator.comparingInt(DecisionPointEntity::getDisplayOrder)).map(p -> p.getDisplayOrder() + ":" + p.getName() + ":" + (p.getTransition() == null ? "" : p.getTransition().getType() + ":" + p.getTransition().getTargets().stream().sorted(Comparator.comparingInt(TransitionTargetEntity::getTargetOrder)).map(t -> String.valueOf(t.getTargetMasterTestCase() == null ? t.getTargetCustomTestCase().getId() : t.getTargetMasterTestCase().getId())).toList())).toList().toString(); }
    private TestCaseVersionEntity current(UUID masterId) { return versionRepository.findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(masterId).stream().filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED).findFirst().orElseThrow(() -> new ConflictException(ErrorCode.PROJECT_TEST_CASE_VERSION_INVALID, "No current Published Version")); }
    private ProjectTestCaseEntity requirePtc(UUID id) { return ptcRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found")); }
}
