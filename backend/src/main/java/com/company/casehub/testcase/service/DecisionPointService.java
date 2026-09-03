package com.company.casehub.testcase.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.common.exception.ValidationException;
import com.company.casehub.testcase.dto.DecisionPointRequest;
import com.company.casehub.testcase.dto.DecisionPointResponse;
import com.company.casehub.testcase.dto.LogicGraphEdgeResponse;
import com.company.casehub.testcase.dto.LogicGraphNodeResponse;
import com.company.casehub.testcase.dto.MasterLogicGraphResponse;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TransitionEntity;
import com.company.casehub.testcase.entity.TransitionTargetEntity;
import com.company.casehub.testcase.repository.DecisionPointRepository;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.TransitionTargetRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DecisionPointService {

    private final MasterTestCaseRepository masterRepository;
    private final TestCaseVersionRepository versionRepository;
    private final DecisionPointRepository decisionPointRepository;
    private final TransitionTargetRepository transitionTargetRepository;
    private final TestCaseAccessPolicy accessPolicy;
    private final DagValidationService dagValidationService;

    public DecisionPointService(MasterTestCaseRepository masterRepository, TestCaseVersionRepository versionRepository,
                                DecisionPointRepository decisionPointRepository,
                                TransitionTargetRepository transitionTargetRepository,
                                TestCaseAccessPolicy accessPolicy, DagValidationService dagValidationService) {
        this.masterRepository = masterRepository;
        this.versionRepository = versionRepository;
        this.decisionPointRepository = decisionPointRepository;
        this.transitionTargetRepository = transitionTargetRepository;
        this.accessPolicy = accessPolicy;
        this.dagValidationService = dagValidationService;
    }

    @Transactional(readOnly = true)
    public List<DecisionPointResponse> list(UUID masterId, UUID versionId, UserPrincipal principal) {
        TestCaseVersionEntity version = requireVisibleVersion(masterId, versionId, principal);
        return decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(version.getId()).stream()
                .map(DecisionPointResponse::from).toList();
    }

    @Transactional
    public DecisionPointResponse create(UUID masterId, UUID versionId, DecisionPointRequest request, UserPrincipal principal) {
        TestCaseVersionEntity version = requireEditableVersion(masterId, versionId, principal);
        ensureSequenceAvailable(versionId, request.displayOrder(), null);
        DecisionPointEntity point = new DecisionPointEntity();
        point.setTestCaseVersion(version);
        point.setDisplayOrder(request.displayOrder());
        point.setName(request.name().trim());
        point.setDescription(trimToNull(request.description()));
        point.setTransition(buildTransition(point, request));
        DecisionPointEntity saved = decisionPointRepository.save(point);
        dagValidationService.validateVersion(version);
        return DecisionPointResponse.from(saved);
    }

    @Transactional
    public DecisionPointResponse update(UUID masterId, UUID versionId, UUID pointId, DecisionPointRequest request,
                                        UserPrincipal principal) {
        TestCaseVersionEntity version = requireEditableVersion(masterId, versionId, principal);
        DecisionPointEntity point = requirePoint(versionId, pointId);
        ensureSequenceAvailable(versionId, request.displayOrder(), pointId);
        point.setDisplayOrder(request.displayOrder());
        point.setName(request.name().trim());
        point.setDescription(trimToNull(request.description()));
        replaceTransition(point, request);
        DecisionPointEntity saved = decisionPointRepository.save(point);
        dagValidationService.validateVersion(version);
        return DecisionPointResponse.from(saved);
    }

    @Transactional
    public void delete(UUID masterId, UUID versionId, UUID pointId, UserPrincipal principal) {
        TestCaseVersionEntity version = requireEditableVersion(masterId, versionId, principal);
        DecisionPointEntity point = requirePoint(versionId, pointId);
        decisionPointRepository.delete(point);
        dagValidationService.validateVersion(version);
    }

    @Transactional(readOnly = true)
    public MasterLogicGraphResponse graph(UUID masterId, UUID versionId, UserPrincipal principal) {
        TestCaseVersionEntity version = requireVisibleVersion(masterId, versionId, principal);
        List<DecisionPointEntity> points = decisionPointRepository.findAllWithGraphBy();
        UUID root = version.getMasterTestCase().getId();
        Map<UUID, List<DecisionPointEntity>> bySource = points.stream()
                .filter(point -> point.getTestCaseVersion() != null && point.getTestCaseVersion().getMasterTestCase() != null)
                .collect(java.util.stream.Collectors.groupingBy(point -> point.getTestCaseVersion().getMasterTestCase().getId()));
        Set<UUID> reachable = new HashSet<>();
        List<LogicGraphEdgeResponse> edges = new ArrayList<>();
        collectGraph(root, bySource, reachable, edges);
        Map<UUID, MasterTestCaseEntity> masters = masterRepository.findAllById(reachable).stream()
                .collect(java.util.stream.Collectors.toMap(MasterTestCaseEntity::getId, value -> value));
        List<LogicGraphNodeResponse> nodes = reachable.stream().sorted(Comparator.comparing(UUID::toString))
                .map(id -> {
                    MasterTestCaseEntity master = masters.get(id);
                    return new LogicGraphNodeResponse(id, master == null ? "" : master.getCaseCode(),
                            master == null ? id.toString() : master.getCaseCode());
                }).toList();
        return new MasterLogicGraphResponse(versionId, root, nodes, edges);
    }

    private void collectGraph(UUID source, Map<UUID, List<DecisionPointEntity>> bySource,
                              Set<UUID> visited, List<LogicGraphEdgeResponse> edges) {
        if (!visited.add(source)) return;
        for (DecisionPointEntity point : bySource.getOrDefault(source, List.of())) {
            if (point.getTransition() == null) continue;
            for (TransitionTargetEntity target : point.getTransition().getTargets().stream()
                    .sorted(Comparator.comparingInt(TransitionTargetEntity::getTargetOrder)).toList()) {
                UUID targetId = target.getTargetMasterTestCase().getId();
                edges.add(new LogicGraphEdgeResponse(target.getId(), source, targetId,
                        point.getTransition().getType().name(), point.getName()));
                collectGraph(targetId, bySource, visited, edges);
            }
        }
    }

    private TransitionEntity buildTransition(DecisionPointEntity point, DecisionPointRequest request) {
        List<UUID> ids = distinctTargetIds(request.targetMasterTestCaseIds());
        dagValidationService.validateTransitionTargets(request.transitionType(), ids);
        TransitionEntity transition = new TransitionEntity();
        transition.setDecisionPoint(point);
        transition.setType(request.transitionType());
        transition.setTargets(buildTargets(transition, ids));
        return transition;
    }

    private void replaceTransition(DecisionPointEntity point, DecisionPointRequest request) {
        TransitionEntity transition = point.getTransition();
        if (transition == null) {
            point.setTransition(buildTransition(point, request));
            return;
        }
        List<UUID> ids = distinctTargetIds(request.targetMasterTestCaseIds());
        dagValidationService.validateTransitionTargets(request.transitionType(), ids);
        transition.setType(request.transitionType());
        // Remove the previous targets at the DB level BEFORE inserting replacements.
        // A clear()+addAll() on an orphan-removal collection flushes the new INSERTs
        // (reusing target_order = 1..n) ahead of the orphan DELETEs, which violates
        // uq_transition_targets_order(transition_id, target_order). Bulk-delete first.
        if (!transition.getTargets().isEmpty()) {
            List<TransitionTargetEntity> existing = new ArrayList<>(transition.getTargets());
            transition.getTargets().clear();
            transitionTargetRepository.deleteAllInBatch(existing);
            transitionTargetRepository.flush();
        }
        transition.getTargets().addAll(buildTargets(transition, ids));
    }

    private List<TransitionTargetEntity> buildTargets(TransitionEntity transition, List<UUID> ids) {
        if (ids.isEmpty()) return new ArrayList<>();
        List<MasterTestCaseEntity> masters = masterRepository.findAllById(ids);
        Map<UUID, MasterTestCaseEntity> byId = masters.stream().collect(java.util.stream.Collectors.toMap(MasterTestCaseEntity::getId, value -> value));
        if (byId.size() != ids.size()) {
            throw new ValidationException(ErrorCode.TEST_CASE_TRANSITION_TARGET_INVALID,
                    "Every transition target must reference an existing Master Test Case.");
        }
        List<TransitionTargetEntity> targets = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            TransitionTargetEntity target = new TransitionTargetEntity();
            target.setTransition(transition);
            target.setTargetOrder(i + 1);
            target.setTargetMasterTestCase(byId.get(ids.get(i)));
            targets.add(target);
        }
        return targets;
    }

    private List<UUID> distinctTargetIds(List<UUID> ids) {
        List<UUID> distinct = new ArrayList<>();
        List<UUID> supplied = ids == null ? List.of() : ids;
        for (UUID id : supplied) {
            if (id != null && !distinct.contains(id)) distinct.add(id);
        }
        if (distinct.size() != supplied.stream().filter(Objects::nonNull).count()) {
            throw new ValidationException(ErrorCode.TEST_CASE_TRANSITION_TARGET_INVALID,
                    "A transition cannot contain duplicate target Master Test Cases.");
        }
        return distinct;
    }

    private void ensureSequenceAvailable(UUID versionId, int sequenceNo, UUID ignoredId) {
        if (sequenceNo < 1) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "Decision point sequenceNo must be at least 1.");
        }
        boolean duplicate = decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(versionId).stream()
                .anyMatch(point -> point.getDisplayOrder() == sequenceNo && !Objects.equals(point.getId(), ignoredId));
        if (duplicate) {
            throw new ConflictException(ErrorCode.CONFLICT, "Decision point sequenceNo is already used by this version.");
        }
    }

    private TestCaseVersionEntity requireEditableVersion(UUID masterId, UUID versionId, UserPrincipal principal) {
        TestCaseVersionEntity version = requireVersion(masterId, versionId);
        if (version.getStatus() != TestCaseVersionStatus.DRAFT || version.isRevisionClosed()) {
            throw new ConflictException(ErrorCode.TEST_CASE_VERSION_IMMUTABLE,
                    "Decision points can only be changed on an open Draft version.");
        }
        if (!accessPolicy.canEditDraft(version, principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_DRAFT_EDIT_FORBIDDEN,
                    "Only the Draft owner, a contributor or an administrator may edit this logic graph.");
        }
        return version;
    }

    private TestCaseVersionEntity requireVisibleVersion(UUID masterId, UUID versionId, UserPrincipal principal) {
        TestCaseVersionEntity version = requireVersion(masterId, versionId);
        MasterTestCaseEntity master = version.getMasterTestCase();
        if (!accessPolicy.isVersionVisible(master, version, principal)) {
            throw new ResourceNotFoundException(ErrorCode.TEST_CASE_VERSION_NOT_FOUND, "Test case version not found: " + versionId);
        }
        return version;
    }

    private TestCaseVersionEntity requireVersion(UUID masterId, UUID versionId) {
        MasterTestCaseEntity master = masterRepository.findById(masterId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + masterId));
        return versionRepository.findByIdAndMasterTestCaseId(versionId, master.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_VERSION_NOT_FOUND, "Test case version not found: " + versionId));
    }

    private DecisionPointEntity requirePoint(UUID versionId, UUID pointId) {
        return decisionPointRepository.findByIdAndTestCaseVersionId(pointId, versionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Decision point not found: " + pointId));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
