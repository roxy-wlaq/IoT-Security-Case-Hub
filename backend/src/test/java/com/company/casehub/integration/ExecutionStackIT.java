package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.execution.dto.AssigneeRequest;
import com.company.casehub.execution.dto.CompleteExecutionRequest;
import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.RelationStatus;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.repository.BranchOutcomeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseSourceRepository;
import com.company.casehub.execution.repository.ProjectTestCaseTriggerRepository;
import com.company.casehub.execution.service.ExecutionService;
import com.company.casehub.execution.service.ProjectTestPlanService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Batch 3 execution and progressive-runtime coverage against PostgreSQL 16. */
class ExecutionStackIT extends AbstractIntegrationTest {

    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseLifecycleService lifecycleService;
    @Autowired private DecisionPointService decisionPointService;
    @Autowired private ProjectService projectService;
    @Autowired private ProjectTestPlanService testPlanService;
    @Autowired private ExecutionService executionService;
    @Autowired private ProjectTestCaseRepository testCaseRepository;
    @Autowired private ProjectTestCaseSourceRepository sourceRepository;
    @Autowired private ProjectTestCaseTriggerRepository triggerRepository;
    @Autowired private BranchOutcomeRepository outcomeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private StandardTaskTypeRepository standardRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private UserEntity coordinator;
    private UserEntity tester;
    private UserPrincipal adminPrincipal;
    private CategoryEntity category;
    private StandardTaskTypeEntity standard;
    private UserPrincipal coordinatorPrincipal;
    private UserPrincipal testerPrincipal;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        category = new CategoryEntity();
        category.setCode("exec-cat-" + suffix);
        category.setName("Execution Category");
        category.setLevel(1);
        category = categoryRepository.save(category);

        standard = new StandardTaskTypeEntity();
        standard.setCode("EXEC-STD-" + suffix);
        standard.setName("Execution Standard");
        standard.setType("STANDARD");
        standard.setEnabled(true);
        standard = standardRepository.save(standard);

        coordinator = userRepository.save(new UserEntity("exec_coord_" + suffix, "Execution Coordinator", "hash"));
        tester = userRepository.save(new UserEntity("exec_tester_" + suffix, "Execution Tester", "hash"));
        UserEntity admin = userRepository.save(new UserEntity("exec_admin_" + suffix, "Execution Admin", "hash"));
        RoleEntity coordinatorRole = roleRepository.findByCode("TEST_COORDINATOR").orElseThrow();
        RoleEntity testerRole = roleRepository.findByCode("TESTER").orElseThrow();
        RoleEntity adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
        userRoleRepository.save(new UserRoleEntity(coordinator, coordinatorRole));
        userRoleRepository.save(new UserRoleEntity(tester, testerRole));
        userRoleRepository.save(new UserRoleEntity(admin, adminRole));

        coordinatorPrincipal = principal(coordinator, "TEST_COORDINATOR");
        testerPrincipal = principal(tester, "TESTER");
        adminPrincipal = principal(admin, "ADMIN");
    }

    @Test
    void passTerminalCompletesAndStoresOutcomeWithoutCreatingTarget() {
        UUID masterId = publishDraft("EXEC-PASS");
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(
                "EXEC-PASS-DP-" + UUID.randomUUID().toString().substring(0, 8), category.getId(), "Pass DP",
                "purpose", "pre", SelectionMode.SINGLE, false, null, null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        UUID pointId = decisionPointService.create(draft.id(), draft.visibleVersion().id(),
                new DecisionPointRequest("Pass", "terminal", 1, TransitionType.PASS, List.of()), coordinatorPrincipal).id();
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"),
                adminPrincipal);

        UUID projectId = createProject();
        UUID ptcId = testPlanService.addMasterCase(projectId, draft.id(), ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        testPlanService.assign(ptcId, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        executionService.start(ptcId, testerPrincipal);
        var result = executionService.complete(ptcId, new CompleteExecutionRequest(List.of(pointId)), testerPrincipal);

        assertThat(result.executionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(result.branchOutcomes()).singleElement().satisfies(outcome -> {
            assertThat(outcome.transitionType()).isEqualTo(TransitionType.PASS);
            assertThat(outcome.targetMasterTestCaseId()).isNull();
        });
        assertThat(testCaseRepository.findByProjectIdOrderByCreatedAtAsc(projectId)).hasSize(1);
        assertThat(outcomeRepository.findByProjectTestCaseId(ptcId)).singleElement()
                .extracting("transitionType").isEqualTo(TransitionType.PASS);
    }

    @Test
    void nextCaseCreatesProgressiveTargetAndReopenRecalculatesFloatingRelation() {
        UUID targetMasterId = publishDraft("EXEC-TARGET");
        TestCaseDetailResponse sourceDraft = draftService.createDraft(new CreateDraftRequest(
                "EXEC-SOURCE-" + UUID.randomUUID().toString().substring(0, 8), category.getId(), "Source",
                "purpose", "pre", SelectionMode.SINGLE, false, null, null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        UUID pointId = decisionPointService.create(sourceDraft.id(), sourceDraft.visibleVersion().id(),
                new DecisionPointRequest("Continue", "next", 1, TransitionType.NEXT_CASE, List.of(targetMasterId)), coordinatorPrincipal).id();
        UUID terminalPointId = decisionPointService.create(sourceDraft.id(), sourceDraft.visibleVersion().id(),
                new DecisionPointRequest("Stop", "terminal", 2, TransitionType.PASS, List.of()), coordinatorPrincipal).id();
        lifecycleService.submitReview(sourceDraft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(sourceDraft.id(), sourceDraft.visibleVersion().id(), new LifecycleActionRequest("publish"),
                adminPrincipal);

        UUID projectId = createProject();
        UUID sourcePtcId = testPlanService.addMasterCase(projectId, sourceDraft.id(), ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        testPlanService.assign(sourcePtcId, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        executionService.start(sourcePtcId, testerPrincipal);
        var completed = executionService.complete(sourcePtcId, new CompleteExecutionRequest(List.of(pointId)), testerPrincipal);

        var target = testCaseRepository.findByProjectIdAndMasterTestCaseId(projectId, targetMasterId).orElseThrow();
        assertThat(completed.affectedTargetProjectTestCaseIds()).containsExactly(target.getId());
        assertThat(target.getExecutionStatus()).isEqualTo(ExecutionStatus.NOT_STARTED);
        assertThat(target.getRelationStatus()).isEqualTo(RelationStatus.CONNECTED);
        assertThat(target.getTestCaseVersion().getId()).isNotNull();
        assertThat(sourceRepository.findByProjectTestCaseId(target.getId())).extracting("sourceType")
                .contains(ProjectTestCaseSourceType.PROGRESSIVE);
        assertThat(triggerRepository.findBySourceProjectTestCaseId(sourcePtcId)).singleElement().satisfies(trigger -> {
            assertThat(trigger.getSourceTestCaseVersion().getId()).isEqualTo(sourceDraft.visibleVersion().id());
            assertThat(trigger.getTargetProjectTestCase().getId()).isEqualTo(target.getId());
        });
        assertThat(testPlanService.list(projectId, coordinatorPrincipal)).flatExtracting("assignees")
                .extracting("userId").contains(tester.getId());

        executionService.reopen(sourcePtcId, testerPrincipal);
        executionService.complete(sourcePtcId, new CompleteExecutionRequest(List.of(terminalPointId)), testerPrincipal);
        assertThat(testCaseRepository.findById(target.getId()).orElseThrow().getRelationStatus())
                .isEqualTo(RelationStatus.FLOATING);
    }

    @Test
    void evidenceRequiredAndUnassignedMutationAreDenied() {
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(
                "EXEC-REQUIRED-" + UUID.randomUUID().toString().substring(0, 8), category.getId(), "Evidence required",
                "purpose", "pre", SelectionMode.SINGLE, true, "proof", null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        UUID pointId = decisionPointService.create(draft.id(), draft.visibleVersion().id(),
                new DecisionPointRequest("Pass", "terminal", 1, TransitionType.PASS, List.of()), coordinatorPrincipal).id();
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"), adminPrincipal);
        UUID projectId = createProject();
        UUID ptcId = testPlanService.addMasterCase(projectId, draft.id(), ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        assertThatThrownBy(() -> executionService.start(ptcId, testerPrincipal))
                .isInstanceOf(com.company.casehub.common.exception.ForbiddenOperationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXECUTION_FORBIDDEN);
        testPlanService.assign(ptcId, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        executionService.start(ptcId, testerPrincipal);
        assertThatThrownBy(() -> executionService.complete(ptcId, new CompleteExecutionRequest(List.of(pointId)), testerPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVIDENCE_REQUIRED);
    }

    @Test
    void concurrentPredecessorsCreateOneTargetAndTwoDistinctTriggers() throws Exception {
        UUID targetMasterId = publishDraft("EXEC-CONCURRENT-TARGET");
        var sourceA = publishSource("EXEC-CONCURRENT-A", targetMasterId);
        var sourceB = publishSource("EXEC-CONCURRENT-B", targetMasterId);
        UUID projectId = createProject();
        UUID ptcA = addAndAssign(projectId, sourceA.id());
        UUID ptcB = addAndAssign(projectId, sourceB.id());
        UUID dpA = decisionPointService.list(sourceA.id(), sourceA.visibleVersion().id(), coordinatorPrincipal)
                .get(0).id();
        UUID dpB = decisionPointService.list(sourceB.id(), sourceB.visibleVersion().id(), coordinatorPrincipal)
                .get(0).id();
        executionService.start(ptcA, testerPrincipal);
        executionService.start(ptcB, testerPrincipal);

        CountDownLatch ready = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> completeAfterBarrier(ptcA, dpA, ready));
            var second = pool.submit(() -> completeAfterBarrier(ptcB, dpB, ready));
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }

        var targets = testCaseRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .filter(c -> c.getMasterTestCase().getId().equals(targetMasterId)).toList();
        assertThat(targets).hasSize(1);
        assertThat(triggerRepository.findByTargetProjectTestCaseId(targets.get(0).getId())).hasSize(2);
    }

    private void completeAfterBarrier(UUID ptcId, UUID pointId, CountDownLatch ready) {
        ready.countDown();
        try {
            ready.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency test barrier interrupted", ex);
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                executionService.complete(ptcId, new CompleteExecutionRequest(List.of(pointId)), testerPrincipal));
    }

    private UUID addAndAssign(UUID projectId, UUID masterId) {
        UUID ptcId = testPlanService.addMasterCase(projectId, masterId, ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        testPlanService.assign(ptcId, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        return ptcId;
    }

    private TestCaseDetailResponse publishSource(String codePrefix, UUID targetMasterId) {
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(
                codePrefix + "-" + UUID.randomUUID().toString().substring(0, 8), category.getId(), codePrefix,
                "purpose", "pre", SelectionMode.SINGLE, false, null, null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        decisionPointService.create(draft.id(), draft.visibleVersion().id(),
                new DecisionPointRequest("Continue", "next", 1, TransitionType.NEXT_CASE, List.of(targetMasterId)), coordinatorPrincipal);
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"), adminPrincipal);
        return draft;
    }

    private UUID publishDraft(String codePrefix) {
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(
                codePrefix + "-" + UUID.randomUUID().toString().substring(0, 8), category.getId(), codePrefix,
                "purpose", "pre", SelectionMode.SINGLE, false, null, null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"), adminPrincipal);
        return draft.id();
    }

    private UUID createProject() {
        return projectService.create(new ProjectCreateRequest("Execution Project " + UUID.randomUUID(), "Device",
                GenerationMode.FULL, List.of(standard.getId()), coordinator.getId()), coordinatorPrincipal).id();
    }

    private UserPrincipal principal(UserEntity user, String role) {
        return new UserPrincipal(user.getId(), user.getUsername(), "hash", user.getDisplayName(), true, false,
                Set.of(role), Set.of("project:read", "project_test_case:read", "project_test_case:execute",
                        "test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review"));
    }
}
