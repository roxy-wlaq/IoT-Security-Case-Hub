package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.execution.dto.AssigneeRequest;
import com.company.casehub.execution.dto.CompleteExecutionRequest;
import com.company.casehub.execution.dto.RelationUpdateRequest;
import com.company.casehub.execution.entity.RelationUpdateAction;
import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.entity.RelationStatus;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseTriggerRepository;
import com.company.casehub.execution.service.ExecutionService;
import com.company.casehub.execution.service.ProjectTestPlanService;
import com.company.casehub.execution.service.RelationUpdateService;
import com.company.casehub.project.dto.ProjectCreateRequest;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.service.ProjectService;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.DecisionPointRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TransitionType;
import com.company.casehub.testcase.service.DecisionPointService;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.TestCaseLifecycleService;
import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** QA Batch 3 supplement: MULTIPLE/NEXT_CASES/FAIL/N_A, version freeze, relation detach, security regression. */
class Batch3SupplementQAIT extends AbstractIntegrationTest {

    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseLifecycleService lifecycleService;
    @Autowired private DecisionPointService decisionPointService;
    @Autowired private ProjectService projectService;
    @Autowired private ProjectTestPlanService testPlanService;
    @Autowired private ExecutionService executionService;
    @Autowired private RelationUpdateService relationUpdateService;
    @Autowired private ProjectTestCaseRepository testCaseRepository;
    @Autowired private ProjectTestCaseTriggerRepository triggerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private StandardTaskTypeRepository standardRepository;

    private UserEntity coordinator;
    private UserEntity tester;
    private UserPrincipal adminPrincipal;
    private UserPrincipal coordinatorPrincipal;
    private UserPrincipal testerPrincipal;
    private CategoryEntity category;
    private StandardTaskTypeEntity standard;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        category = new CategoryEntity();
        category.setCode("qa3-cat-" + suffix);
        category.setName("QA3 Category");
        category.setLevel(1);
        category = categoryRepository.save(category);

        standard = new StandardTaskTypeEntity();
        standard.setCode("QA3-STD-" + suffix);
        standard.setName("QA3 Standard");
        standard.setType("STANDARD");
        standard.setEnabled(true);
        standard = standardRepository.save(standard);

        coordinator = userRepository.save(new UserEntity("qa3_coord_" + suffix, "QA3 Coord", "hash"));
        tester = userRepository.save(new UserEntity("qa3_tester_" + suffix, "QA3 Tester", "hash"));
        UserEntity admin = userRepository.save(new UserEntity("qa3_admin_" + suffix, "QA3 Admin", "hash"));
        RoleEntity coordRole = roleRepository.findByCode("TEST_COORDINATOR").orElseThrow();
        RoleEntity testerRole = roleRepository.findByCode("TESTER").orElseThrow();
        RoleEntity adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
        userRoleRepository.save(new UserRoleEntity(coordinator, coordRole));
        userRoleRepository.save(new UserRoleEntity(tester, testerRole));
        userRoleRepository.save(new UserRoleEntity(admin, adminRole));

        coordinatorPrincipal = principal(coordinator, "TEST_COORDINATOR", false);
        testerPrincipal = principal(tester, "TESTER", false);
        adminPrincipal = principal(admin, "ADMIN", true);

        projectId = projectService.create(new ProjectCreateRequest("QA3 Project " + suffix, "Device",
                GenerationMode.FULL, List.of(standard.getId()), coordinator.getId()), coordinatorPrincipal).id();
    }

    @Test
    void multipleNextCasesAndFailTerminalStoreOutcomesAndTargets() {
        UUID mTarget1 = publishDraft("QA3-T1");
        UUID mTarget2 = publishDraft("QA3-T2");
        // source is MULTIPLE; one NEXT_CASES DP pointing at two targets, one FAIL terminal DP
        TestCaseDetailResponse src = createDraft("QA3-SRC", SelectionMode.MULTIPLE);
        UUID dpNext = decisionPointService.create(src.id(), src.visibleVersion().id(),
                new DecisionPointRequest("Branch", "next", 1, TransitionType.NEXT_CASES, List.of(mTarget1, mTarget2)),
                coordinatorPrincipal).id();
        UUID dpFail = decisionPointService.create(src.id(), src.visibleVersion().id(),
                new DecisionPointRequest("Fail", "terminal", 2, TransitionType.FAIL, List.of()),
                coordinatorPrincipal).id();
        publish(src);

        UUID sourcePtc = addAndAssign(src.id());
        executionService.start(sourcePtc, testerPrincipal);
        var completed = executionService.complete(sourcePtc,
                new CompleteExecutionRequest(List.of(dpNext, dpFail)), testerPrincipal);

        assertThat(completed.executionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        // NEXT_CASES produces two targets; FAIL produces a terminal outcome
        assertThat(completed.branchOutcomes()).hasSize(3);
        long terminalCount = completed.branchOutcomes().stream().filter(o -> o.transitionType() == TransitionType.FAIL).count();
        long nextCount = completed.branchOutcomes().stream().filter(o -> o.transitionType() == TransitionType.NEXT_CASES).count();
        assertThat(terminalCount).isEqualTo(1);
        assertThat(nextCount).isEqualTo(2);
        assertThat(testCaseRepository.findByProjectIdAndMasterTestCaseId(projectId, mTarget1)).isPresent();
        assertThat(testCaseRepository.findByProjectIdAndMasterTestCaseId(projectId, mTarget2)).isPresent();
        var targets = testCaseRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .filter(c -> !c.getMasterTestCase().getId().equals(src.id())).toList();
        // each runtime target has a PROGRESSIVE source and one trigger from sourcePtc
        assertThat(targets).hasSize(2);
        assertThat(triggerRepository.findBySourceProjectTestCaseId(sourcePtc)).hasSize(2);
    }

    @Test
    void existingRemovedTargetIsReusedWithFrozenVersionOnRuntimeDerivation() {
        // master M has version v1 published. Create PTC at v1 via plan add (INITIAL) then remove it (soft).
        UUID masterM = publishDraft("QA3-M");
        UUID ptcV1 = testPlanService.addMasterCase(projectId, masterM, ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        testPlanService.remove(ptcV1, coordinatorPrincipal);
        UUID boundV1 = testCaseRepository.findById(ptcV1).orElseThrow().getTestCaseVersion().getId();

        // Now a runtime NEXT_CASE from another source points at master M.
        UUID srcMaster = publishDraft("QA3-SRC2");
        TestCaseDetailResponse src = createDraftWithNext("QA3-SRC3", masterM);
        publish(src);
        UUID sourcePtc = addAndAssign(src.id());
        executionService.start(sourcePtc, testerPrincipal);
        executionService.complete(sourcePtc, new CompleteExecutionRequest(List.of(dpOf(src.id(), src.visibleVersion().id()))), testerPrincipal);

        // reused the removed row; version is frozen at v1 (unchanged)
        var reused = testCaseRepository.findByProjectIdAndMasterTestCaseId(projectId, masterM).orElseThrow();
        assertThat(reused.getTestCaseVersion().getId()).isEqualTo(boundV1);
        assertThat(reused.isRemoved()).isFalse();
        assertThat(reused.getRelationStatus()).isEqualTo(RelationStatus.CONNECTED);
    }

    @Test
    void detachTargetMakesTargetFloatingAndAttachReconnects() {
        UUID targetMaster = publishDraft("QA3-DT");
        TestCaseDetailResponse src = createDraftWithNext("QA3-DSRC", targetMaster);
        publish(src);
        UUID sourcePtc = addAndAssign(src.id());
        executionService.start(sourcePtc, testerPrincipal);
        executionService.complete(sourcePtc, new CompleteExecutionRequest(List.of(dpOf(src.id(), src.visibleVersion().id()))), testerPrincipal);
        UUID targetPtc = testCaseRepository.findByProjectIdAndMasterTestCaseId(projectId, targetMaster).orElseThrow().getId();
        assertThat(testCaseRepository.findById(targetPtc).orElseThrow().getRelationStatus())
                .isEqualTo(RelationStatus.CONNECTED);

        // DETACH via relation endpoint -> target loses the trigger -> FLOATING
        relationUpdateService.update(sourcePtc, new RelationUpdateRequest(RelationUpdateAction.DETACH_TARGET,
                dpOf(src.id(), src.visibleVersion().id()), targetPtc), testerPrincipal);
        assertThat(testCaseRepository.findById(targetPtc).orElseThrow().getRelationStatus())
                .isEqualTo(RelationStatus.FLOATING);
        assertThat(triggerRepository.findByTargetProjectTestCaseId(targetPtc)).isEmpty();

        // re-attach -> CONNECTED again with a trigger
        relationUpdateService.update(sourcePtc, new RelationUpdateRequest(RelationUpdateAction.ADD_TARGET,
                dpOf(src.id(), src.visibleVersion().id()), targetPtc), testerPrincipal);
        assertThat(testCaseRepository.findById(targetPtc).orElseThrow().getRelationStatus())
                .isEqualTo(RelationStatus.CONNECTED);
        assertThat(triggerRepository.findBySourceProjectTestCaseId(sourcePtc)).hasSize(1);
    }

    @Test
    void testerWithGlobalAddPermissionStillCannotManagePlan() {
        // tester carries project_test_case:add authority but is not a coordinator/admin of the project
        UserPrincipal testerWithAdd = new UserPrincipal(tester.getId(), tester.getUsername(), "hash",
                tester.getDisplayName(), true, false, Set.of("TESTER"),
                Set.of("project:read", "project_test_case:read", "project_test_case:execute", "project_test_case:add",
                        "evidence:read", "note:read"));
        UUID master = publishDraft("QA3-SEC");
        assertThatThrownBy(() -> testPlanService.addMasterCase(projectId, master,
                ProjectTestCaseSourceType.INITIAL, testerWithAdd))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROJECT_ACCESS_FORBIDDEN);
        // manage permission does not imply execute: a coordinator (non-assignee) cannot start execution
        UUID ptc = testPlanService.addMasterCase(projectId, master, ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        assertThatThrownBy(() -> executionService.start(ptc, coordinatorPrincipal))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXECUTION_FORBIDDEN);
    }

    // ---- helpers ----
    private UUID dpOf(UUID masterId, UUID versionId) {
        return decisionPointService.list(masterId, versionId, coordinatorPrincipal).get(0).id();
    }

    private UUID addAndAssign(UUID masterId) {
        UUID ptc = testPlanService.addMasterCase(projectId, masterId, ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        testPlanService.assign(ptc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        return ptc;
    }

    private TestCaseDetailResponse createDraft(String prefix, SelectionMode mode) {
        return draftService.createDraft(new CreateDraftRequest(
                prefix + "-" + UUID.randomUUID().toString().substring(0, 8), category.getId(), prefix,
                "purpose", "pre", mode, false, null, null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
    }

    private TestCaseDetailResponse createDraftWithNext(String prefix, UUID targetMasterId) {
        TestCaseDetailResponse d = createDraft(prefix, SelectionMode.SINGLE);
        decisionPointService.create(d.id(), d.visibleVersion().id(),
                new DecisionPointRequest("Continue", "next", 1, TransitionType.NEXT_CASE, List.of(targetMasterId)),
                coordinatorPrincipal);
        return d;
    }

    private void publish(TestCaseDetailResponse draft) {
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"), adminPrincipal);
    }

    private UUID publishDraft(String codePrefix) {
        TestCaseDetailResponse draft = createDraft(codePrefix, SelectionMode.SINGLE);
        publish(draft);
        return draft.id();
    }

    private UserPrincipal principal(UserEntity user, String role, boolean isAdmin) {
        Set<String> auths = isAdmin
                ? Set.of("project:read", "project_test_case:read", "project_test_case:add", "project_test_case:assign",
                        "project_test_case:execute", "test_case:read", "test_case:review", "test_case:publish",
                        "test_case:submit_review", "test_case:draft_create", "test_case:draft_edit")
                : Set.of("project:read", "project_test_case:read", "project_test_case:execute",
                        "test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review");
        return new UserPrincipal(user.getId(), user.getUsername(), "hash", user.getDisplayName(), true, false,
                Set.of(role), auths);
    }
}
