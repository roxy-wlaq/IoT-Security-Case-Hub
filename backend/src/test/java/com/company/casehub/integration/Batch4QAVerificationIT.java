package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.change.dto.CapabilityUpdateRequestPayload;
import com.company.casehub.change.dto.ReviewRequestPayload;
import com.company.casehub.change.dto.TestCaseChangeRequestPayload;
import com.company.casehub.change.entity.CapabilityUpdateRequestStatus;
import com.company.casehub.change.service.CapabilityUpdateRequestService;
import com.company.casehub.change.service.TestCaseChangeRequestService;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.customcase.dto.CustomDecisionPointRequest;
import com.company.casehub.customcase.dto.CustomStepRequest;
import com.company.casehub.customcase.dto.CustomTestCaseRequest;
import com.company.casehub.customcase.dto.CustomTestCaseResponse;
import com.company.casehub.customcase.service.CustomTestCaseService;
import com.company.casehub.execution.dto.AssigneeRequest;
import com.company.casehub.execution.dto.CompleteExecutionRequest;
import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.repository.BranchOutcomeRepository;
import com.company.casehub.execution.repository.ProjectDecisionSelectionRepository;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseSourceRepository;
import com.company.casehub.execution.service.ExecutionService;
import com.company.casehub.execution.service.ProjectTestPlanService;
import com.company.casehub.generation.dto.GenerationRuleRequest;
import com.company.casehub.generation.entity.ConditionTargetType;
import com.company.casehub.generation.entity.GenerationOperator;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GenerationTriggerType;
import com.company.casehub.generation.entity.GroupOperator;
import com.company.casehub.generation.repository.GenerationRunRepository;
import com.company.casehub.generation.service.GenerationRuleService;
import com.company.casehub.generation.service.GenerationRuntimeService;
import com.company.casehub.project.dto.ProjectCreateRequest;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import com.company.casehub.project.service.ProjectService;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.DecisionPointRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TransitionType;
import com.company.casehub.testcase.repository.DecisionPointRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.service.DecisionPointService;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.TestCaseLifecycleService;
import com.company.casehub.upgrade.service.VersionUpgradeService;
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

/**
 * Independent QA verification for Batch 4 (Phase 21-24).
 *
 * These scenarios deliberately target behaviours the DEV suite never asserts:
 * tester review/upgrade boundaries, capability approval not auto-adding
 * recommendations, custom-target project scoping, and historical execution
 * reference preservation across a version upgrade.
 */
class Batch4QAVerificationIT extends AbstractIntegrationTest {

    @Autowired private CustomTestCaseService customService;
    @Autowired private ExecutionService executionService;
    @Autowired private ProjectTestPlanService planService;
    @Autowired private ProjectService projectService;
    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseLifecycleService lifecycleService;
    @Autowired private DecisionPointService decisionPointService;
    @Autowired private CapabilityUpdateRequestService capabilityRequestService;
    @Autowired private TestCaseChangeRequestService changeRequestService;
    @Autowired private VersionUpgradeService versionUpgradeService;
    @Autowired private GenerationRuleService ruleService;
    @Autowired private GenerationRuntimeService runtimeService;
    @Autowired private ProjectTestCaseRepository ptcRepository;
    @Autowired private ProjectDecisionSelectionRepository selectionRepository;
    @Autowired private BranchOutcomeRepository outcomeRepository;
    @Autowired private ProjectTestCaseAssigneeRepository assigneeRepository;
    @Autowired private ProjectTestCaseSourceRepository sourceRepository;
    @Autowired private TestCaseVersionRepository versionRepository;
    @Autowired private DecisionPointRepository decisionPointRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private StandardTaskTypeRepository standardRepository;
    @Autowired private CapabilityRepository capabilityRepository;
    @Autowired private GenerationRunRepository generationRunRepository;

    private UserEntity coordinator;
    private UserEntity tester;
    private UserPrincipal coordinatorPrincipal;
    private UserPrincipal testerPrincipal;
    private UserPrincipal adminPrincipal;
    private CategoryEntity category;
    private StandardTaskTypeEntity standard;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        category = new CategoryEntity();
        category.setCode("q4-cat-" + suffix);
        category.setName("Q4 Category");
        category.setLevel(1);
        category = categoryRepository.save(category);

        standard = new StandardTaskTypeEntity();
        standard.setCode("Q4-STD-" + suffix);
        standard.setName("Q4 Standard");
        standard.setType("STANDARD");
        standard.setEnabled(true);
        standard = standardRepository.save(standard);

        RoleEntity coordRole = roleRepository.findByCode("TEST_COORDINATOR").orElseThrow();
        RoleEntity testerRole = roleRepository.findByCode("TESTER").orElseThrow();
        RoleEntity adminRole = roleRepository.findByCode("ADMIN").orElseThrow();

        coordinator = userRepository.save(new UserEntity("q4_coord_" + suffix, "Q4 Coordinator", "hash"));
        tester = userRepository.save(new UserEntity("q4_tester_" + suffix, "Q4 Tester", "hash"));
        UserEntity admin = userRepository.save(new UserEntity("q4_admin_" + suffix, "Q4 Admin", "hash"));
        userRoleRepository.save(new UserRoleEntity(coordinator, coordRole));
        userRoleRepository.save(new UserRoleEntity(tester, testerRole));
        userRoleRepository.save(new UserRoleEntity(admin, adminRole));

        coordinatorPrincipal = principal(coordinator, "TEST_COORDINATOR");
        testerPrincipal = principal(tester, "TESTER");
        adminPrincipal = principal(admin, "ADMIN");
    }

    // ------------------------------------------------------------------
    // Suite 11: Tester cannot review or rebind versions
    // ------------------------------------------------------------------

    @Test
    void testerCannotReviewCapabilityRequestReviewChangeRequestOrRebindVersion() {
        UUID projectId = createProject();
        UUID membershipCase = publishMaster("Q4-MEMBER", false);
        UUID membershipPtc = planService.addMasterCase(projectId, membershipCase,
                ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        planService.assign(membershipPtc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);

        CapabilityEntity capability = capability("Q4-CAP");
        var capabilityRequest = capabilityRequestService.submit(projectId, capability.getId(),
                new CapabilityUpdateRequestPayload(ProjectCapabilityValue.YES, "Observed", "evidence"), testerPrincipal);

        // A Tester may submit but must never review.
        assertThatThrownBy(() -> capabilityRequestService.review(capabilityRequest.id(), true,
                new ReviewRequestPayload("self approve"), testerPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);

        // A Tester must never rebind a Project Test Case to another version.
        assertThatThrownBy(() -> versionUpgradeService.upgrade(membershipPtc, testerPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);

        UUID changeSource = publishMaster("Q4-CHANGE", false);
        UUID sourceVersionId = versionRepository
                .findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(changeSource).get(0).getId();
        var changeRequest = changeRequestService.submit(changeSource,
                new TestCaseChangeRequestPayload(sourceVersionId, "clarify step"), testerPrincipal);

        assertThatThrownBy(() -> changeRequestService.review(changeRequest.id(), true,
                new ReviewRequestPayload("self approve"), testerPrincipal))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHANGE_REQUEST_REVIEW_FORBIDDEN);

        // The request must remain PENDING after the rejected attempt.
        assertThat(changeRequestService.list(changeSource, coordinatorPrincipal).stream()
                .filter(r -> r.id().equals(changeRequest.id())).findFirst().orElseThrow().status().name())
                .isEqualTo("PENDING");
    }

    // ------------------------------------------------------------------
    // Suite 5 / Must Verify: capability approval must not auto-add cases
    // ------------------------------------------------------------------

    @Test
    void capabilityApprovalProducesRecommendationsWithoutAutoAddingThemToThePlan() {
        UUID projectId = createProject();
        UUID membershipCase = publishMaster("Q4-CAP-MEMBER", false);
        UUID membershipPtc = planService.addMasterCase(projectId, membershipCase,
                ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        planService.assign(membershipPtc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);

        UUID targetCase = publishMaster("Q4-CAP-TARGET", false);
        CapabilityEntity capability = capability("Q4-CAP-GEN");
        ruleService.create(new GenerationRuleRequest(
                "Q4-RULE-" + UUID.randomUUID().toString().substring(0, 6), "Rule", null,
                GenerationRuleMode.FULL, GenerationRuleStatus.ENABLED,
                List.of(new GenerationRuleRequest.GroupRequest(null, GroupOperator.AND, 0,
                        List.of(new GenerationRuleRequest.ConditionRequest(ConditionTargetType.CAPABILITY,
                                capability.getId(), null, GenerationOperator.EQ_YES, 0)))),
                List.of(targetCase)), adminPrincipal);

        int planSizeBefore = planService.list(projectId, coordinatorPrincipal).size();

        var request = capabilityRequestService.submit(projectId, capability.getId(),
                new CapabilityUpdateRequestPayload(ProjectCapabilityValue.YES, "Observed", "evidence"), testerPrincipal);
        var approved = capabilityRequestService.review(request.id(), true, new ReviewRequestPayload("ok"), coordinatorPrincipal);
        assertThat(approved.status()).isEqualTo(CapabilityUpdateRequestStatus.APPROVED);

        var run = generationRunRepository.findByProjectIdOrderByExecutedAtDesc(projectId).stream()
                .filter(r -> r.getTriggerType() == GenerationTriggerType.CAPABILITY_UPDATE)
                .findFirst().orElseThrow(() -> new AssertionError("No CAPABILITY_UPDATE generation run was created"));

        // The approval really did produce recommendations ...
        assertThat(runtimeService.listRecommendations(run.getId(), coordinatorPrincipal)).isNotEmpty();
        // ... yet the plan is untouched: recommendations stay manual.
        assertThat(planService.list(projectId, coordinatorPrincipal)).hasSize(planSizeBefore);
        assertThat(planService.list(projectId, coordinatorPrincipal))
                .noneMatch(p -> p.masterTestCaseId().equals(targetCase));
        assertThat(ptcRepository.findById(membershipPtc).orElseThrow().isRemoved()).isFalse();
    }

    // ------------------------------------------------------------------
    // Suite 1-3 / Must Verify: custom targets stay project-scoped
    // ------------------------------------------------------------------

    @Test
    void customTargetsStayProjectScopedAndNeverEnterTheMasterDag() {
        UUID projectA = createProject();
        UUID projectB = createProject();
        joinAsTester(projectA);
        joinAsTester(projectB);

        // A Custom Case in project B ...
        CustomTestCaseResponse inProjectB = customService.create(projectB, customRequest("Q4-SCOPED-B", List.of(), List.of()),
                testerPrincipal);

        // ... must NOT be referenceable as a target from project A.
        assertThatThrownBy(() -> customService.create(projectA,
                customRequest("Q4-SCOPED-A", List.of(), List.of(inProjectB.id())), testerPrincipal))
                .isInstanceOf(com.company.casehub.common.exception.ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_CASE_NOT_FOUND);

        // Same-project custom targets are allowed ...
        CustomTestCaseResponse first = customService.create(projectA, customRequest("Q4-SCOPED-1", List.of(), List.of()),
                testerPrincipal);
        CustomTestCaseResponse linked = customService.create(projectA,
                customRequest("Q4-SCOPED-2", List.of(), List.of(first.id())), testerPrincipal);
        assertThat(linked.decisionPoints().get(0).targets().get(0).customTestCaseId()).isEqualTo(first.id());

        // ... but a case carrying a custom target can never be submitted to the library.
        assertThatThrownBy(() -> customService.submitToLibrary(projectA, linked.id(), testerPrincipal))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_CASE_LIBRARY_TARGET_INVALID);

        // A master-backed target submits cleanly and lands in the library without custom targets.
        UUID masterTarget = publishMaster("Q4-LIB-TARGET", false);
        CustomTestCaseResponse submitable = customService.create(projectA,
                customRequest("Q4-SCOPED-3", List.of(masterTarget), List.of()), testerPrincipal);
        var submission = customService.submitToLibrary(projectA, submitable.id(), testerPrincipal);
        var libraryVersion = versionRepository.findById(submission.draftVersionId()).orElseThrow();

        assertThat(decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(libraryVersion.getId()))
                .allSatisfy(point -> assertThat(point.getCustomTestCase()).isNull());
        assertThat(decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(libraryVersion.getId()))
                .flatExtracting(point -> point.getTransition().getTargets())
                .allSatisfy(target -> {
                    assertThat(target.getTargetCustomTestCase()).isNull();
                    assertThat(target.getTargetMasterTestCase()).isNotNull();
                });
        // The project-scoped custom case itself is untouched by submission.
        assertThat(customService.list(projectA, testerPrincipal)).extracting(CustomTestCaseResponse::id)
                .contains(first.id(), linked.id(), submitable.id());
        assertThat(customService.list(projectB, testerPrincipal)).extracting(CustomTestCaseResponse::id)
                .containsExactly(inProjectB.id());
    }

    // ------------------------------------------------------------------
    // Suite 9-10 / Must Verify: upgrade preserves execution history
    // ------------------------------------------------------------------

    @Test
    void upgradeKeepsHistoricalSelectionAndOutcomeReferencesIntact() {
        UUID projectId = createProject();
        UUID masterId = publishMaster("Q4-UPGRADE", true);
        UUID ptcId = planService.addMasterCase(projectId, masterId,
                ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        planService.assign(ptcId, new AssigneeRequest(tester.getId()), coordinatorPrincipal);

        UUID oldVersionId = ptcRepository.findById(ptcId).orElseThrow().getTestCaseVersion().getId();
        UUID oldDecisionPointId = decisionPointRepository
                .findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(oldVersionId).get(0).getId();

        executionService.start(ptcId, testerPrincipal);
        var completed = executionService.complete(ptcId, new CompleteExecutionRequest(List.of(oldDecisionPointId)),
                testerPrincipal);
        assertThat(completed.executionStatus()).isEqualTo(ExecutionStatus.COMPLETED);

        int selectionsBefore = selectionRepository.findByProjectTestCaseId(ptcId).size();
        int outcomesBefore = outcomeRepository.findByProjectTestCaseId(ptcId).size();
        assertThat(selectionsBefore).isPositive();

        // Publish v2 with deliberately different decision-point logic.
        TestCaseDetailResponse revision = lifecycleService.createRevision(masterId,
                new CreateRevisionRequest(oldVersionId, "v2 logic change"), coordinatorPrincipal);
        // createRevision copies v1 decision points into the draft, so the logic change is
        // expressed as an additional decision point at the next display order.
        decisionPointService.create(masterId, revision.draftVersion().id(),
                new DecisionPointRequest("Changed decision", "different logic", 2, TransitionType.FAIL, List.of()),
                coordinatorPrincipal);
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit v2"), coordinatorPrincipal);
        lifecycleService.publish(masterId, revision.draftVersion().id(), new LifecycleActionRequest("publish v2"),
                adminPrincipal);
        UUID newVersionId = revision.draftVersion().id();

        var available = versionUpgradeService.availability(ptcId, testerPrincipal);
        assertThat(available.newVersionAvailable()).isTrue();
        assertThat(available.currentPublishedVersionId()).isEqualTo(newVersionId);
        assertThat(available.diff().logicChanged()).isTrue();
        assertThat(available.diff().warning()).isNotBlank();
        // Detection alone must never rebind the version.
        assertThat(ptcRepository.findById(ptcId).orElseThrow().getTestCaseVersion().getId()).isEqualTo(oldVersionId);

        var upgraded = versionUpgradeService.upgrade(ptcId, coordinatorPrincipal);
        assertThat(upgraded.upgraded()).isTrue();
        assertThat(upgraded.projectTestCaseId()).isEqualTo(ptcId);

        var after = ptcRepository.findById(ptcId).orElseThrow();
        assertThat(after.getTestCaseVersion().getId()).isEqualTo(newVersionId);
        assertThat(after.getId()).isEqualTo(ptcId);
        assertThat(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptcId, tester.getId())).isTrue();
        assertThat(sourceRepository.findByProjectTestCaseId(ptcId)).extracting(source -> source.getSourceType())
                .contains(ProjectTestCaseSourceType.INITIAL);

        // Historical execution data keeps pointing at the immutable old version's decision points.
        List<UUID> oldDecisionPointIds = decisionPointRepository
                .findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(oldVersionId).stream()
                .map(com.company.casehub.testcase.entity.DecisionPointEntity::getId).toList();
        assertThat(oldDecisionPointIds).contains(oldDecisionPointId).isNotEmpty();

        assertThat(selectionRepository.findByProjectTestCaseId(ptcId)).hasSize(selectionsBefore)
                .allSatisfy(selection -> {
                    assertThat(selection.getDecisionPoint()).isNotNull();
                    assertThat(selection.getDecisionPoint().getId()).isIn(oldDecisionPointIds);
                });
        assertThat(outcomeRepository.findByProjectTestCaseId(ptcId)).hasSize(outcomesBefore)
                .allSatisfy(outcome -> {
                    assertThat(outcome.getDecisionPoint()).isNotNull();
                    assertThat(outcome.getDecisionPoint().getId()).isIn(oldDecisionPointIds);
                });
        // The new version must not have silently inherited the recorded selections.
        assertThat(decisionPointRepository.findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(newVersionId))
                .extracting(com.company.casehub.testcase.entity.DecisionPointEntity::getId)
                .doesNotContainAnyElementsOf(selectionRepository.findByProjectTestCaseId(ptcId).stream()
                        .map(selection -> selection.getDecisionPoint().getId()).toList());
    }

    // ------------------------------------------------------------------
    // Suite 6: listing change requests must stay deterministic for a Master
    // that has no Published version (fresh Draft, or deprecated-only)
    // ------------------------------------------------------------------

    @Test
    void changeRequestListIsDeterministicForMastersWithoutAPublishedVersion() {
        // Exactly the state Submit-to-Library produces: a Master holding only a DRAFT v1.0.
        TestCaseDetailResponse draftOnly = draftService.createDraft(
                new CreateDraftRequest("Q4-NEVER-" + UUID.randomUUID().toString().substring(0, 6), category.getId(),
                        "Never published", "purpose", "pre", SelectionMode.SINGLE, false, null, null, null,
                        List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()),
                coordinatorPrincipal);
        assertThat(changeRequestService.list(draftOnly.id(), coordinatorPrincipal)).isEmpty();
        assertThat(changeRequestService.list(draftOnly.id(), testerPrincipal)).isEmpty();

        // Publishing restores the normal path.
        lifecycleService.submitReview(draftOnly.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draftOnly.id(), draftOnly.visibleVersion().id(), new LifecycleActionRequest("publish"),
                adminPrincipal);
        assertThat(changeRequestService.list(draftOnly.id(), coordinatorPrincipal)).isEmpty();

        // Deprecating the only Published version must not reintroduce the failure.
        lifecycleService.deprecate(draftOnly.id(), draftOnly.visibleVersion().id(),
                new LifecycleActionRequest("deprecate"), adminPrincipal);
        assertThat(changeRequestService.list(draftOnly.id(), coordinatorPrincipal)).isEmpty();
        assertThat(changeRequestService.list(draftOnly.id(), testerPrincipal)).isEmpty();
    }

    // ------------------------------------------------------------------ helpers

    private void joinAsTester(UUID projectId) {
        UUID membershipCase = publishMaster("Q4-JOIN", false);
        UUID ptc = planService.addMasterCase(projectId, membershipCase, ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        planService.assign(ptc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
    }

    private CustomTestCaseRequest customRequest(String code, List<UUID> masterTargets, List<UUID> customTargets) {
        return new CustomTestCaseRequest(code + "-" + UUID.randomUUID().toString().substring(0, 6), "Custom case",
                "purpose", "pre", SelectionMode.SINGLE, false, null, null,
                List.of(new CustomStepRequest("step", "do it")),
                List.of(new CustomDecisionPointRequest("Done", "terminal", 1, TransitionType.PASS,
                        masterTargets, customTargets)));
    }

    private CapabilityEntity capability(String prefix) {
        CapabilityEntity capability = new CapabilityEntity();
        capability.setCode(prefix + "-" + UUID.randomUUID().toString().substring(0, 8));
        capability.setName(prefix);
        capability.setEnabled(true);
        return capabilityRepository.save(capability);
    }

    private UUID createProject() {
        return projectService.create(new ProjectCreateRequest("Q4 Project " + UUID.randomUUID(), "Device",
                GenerationMode.FULL, List.of(standard.getId()), coordinator.getId()), coordinatorPrincipal).id();
    }

    private UUID publishMaster(String prefix, boolean withDecisionPoint) {
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(
                prefix + "-" + UUID.randomUUID().toString().substring(0, 6), category.getId(), prefix, "purpose",
                "pre", SelectionMode.SINGLE, false, null, null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        if (withDecisionPoint) {
            decisionPointService.create(draft.id(), draft.visibleVersion().id(),
                    new DecisionPointRequest("Original decision", "original logic", 1, TransitionType.PASS, List.of()),
                    coordinatorPrincipal);
        }
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"),
                adminPrincipal);
        return draft.id();
    }

    private UserPrincipal principal(UserEntity user, String role) {
        return new UserPrincipal(user.getId(), user.getUsername(), "hash", user.getDisplayName(), true, false,
                Set.of(role), Set.of(
                "project:read", "project:create", "project:update", "project_capability:read",
                "project_capability:update", "capability_request:create", "capability_request:review",
                "project_test_case:read", "project_test_case:add", "project_test_case:assign",
                "project_test_case:execute", "project_test_case:remove", "project_test_case:restore",
                "generation_rule:manage", "generation_rule:read", "generation:run",
                "generation:review_recommendation",
                "test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review",
                "test_case:review", "test_case:publish", "test_case:deprecate", "change_request:create", "change_request:review",
                "project_custom_test_case:create", "project_custom_test_case:edit_own_or_assigned"));
    }
}
