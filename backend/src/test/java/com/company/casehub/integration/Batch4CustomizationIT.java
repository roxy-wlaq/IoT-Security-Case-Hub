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
import com.company.casehub.change.entity.TestCaseChangeRequestStatus;
import com.company.casehub.change.service.CapabilityUpdateRequestService;
import com.company.casehub.change.service.TestCaseChangeRequestService;
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
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.service.ExecutionService;
import com.company.casehub.execution.service.ProjectTestPlanService;
import com.company.casehub.generation.entity.GenerationTriggerType;
import com.company.casehub.generation.repository.GenerationRunRepository;
import com.company.casehub.project.dto.ProjectCreateRequest;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.service.ProjectService;
import com.company.casehub.project.repository.ProjectCapabilityRepository;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.DecisionPointRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TransitionType;
import com.company.casehub.testcase.service.DecisionPointService;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.TestCaseLifecycleService;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.RevisionContributorRepository;
import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import com.company.casehub.upgrade.service.VersionUpgradeService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Batch4CustomizationIT extends AbstractIntegrationTest {
    @Autowired private CustomTestCaseService customService;
    @Autowired private ExecutionService executionService;
    @Autowired private ProjectTestPlanService planService;
    @Autowired private ProjectService projectService;
    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseLifecycleService lifecycleService;
    @Autowired private DecisionPointService decisionPointService;
    @Autowired private CapabilityUpdateRequestService capabilityRequestService;
    @Autowired private TestCaseChangeRequestService changeRequestService;
    @Autowired private ProjectTestCaseRepository ptcRepository;
    @Autowired private TestCaseVersionRepository versionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private StandardTaskTypeRepository standardRepository;
    @Autowired private CapabilityRepository capabilityRepository;
    @Autowired private ProjectCapabilityRepository projectCapabilityRepository;
    @Autowired private VersionUpgradeService versionUpgradeService;
    @Autowired private GenerationRunRepository generationRunRepository;
    @Autowired private RevisionContributorRepository contributorRepository;

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
        category = new CategoryEntity(); category.setCode("b4-cat-" + suffix); category.setName("B4 Category"); category.setLevel(1); category = categoryRepository.save(category);
        standard = new StandardTaskTypeEntity(); standard.setCode("B4-STD-" + suffix); standard.setName("B4 Standard"); standard.setType("STANDARD"); standard.setEnabled(true); standard = standardRepository.save(standard);
        coordinator = userRepository.save(new UserEntity("b4_coord_" + suffix, "B4 Coordinator", "hash"));
        tester = userRepository.save(new UserEntity("b4_tester_" + suffix, "B4 Tester", "hash"));
        UserEntity admin = userRepository.save(new UserEntity("b4_admin_" + suffix, "B4 Admin", "hash"));
        userRoleRepository.save(new UserRoleEntity(coordinator, roleRepository.findByCode("TEST_COORDINATOR").orElseThrow()));
        userRoleRepository.save(new UserRoleEntity(tester, roleRepository.findByCode("TESTER").orElseThrow()));
        userRoleRepository.save(new UserRoleEntity(admin, roleRepository.findByCode("ADMIN").orElseThrow()));
        coordinatorPrincipal = principal(coordinator, "TEST_COORDINATOR");
        testerPrincipal = principal(tester, "TESTER");
        adminPrincipal = principal(admin, "ADMIN");
    }

    @Test
    void testerCreatesAndExecutesCustomCaseAndCannotAssignAnotherUser() {
        UUID projectId = createProject();
        UUID membershipCase = publishMaster("B4-MEMBER");
        UUID membershipPtc = planService.addMasterCase(projectId, membershipCase, ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        planService.assign(membershipPtc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        CustomTestCaseResponse custom = customService.create(projectId, request("B4-CUSTOM"), testerPrincipal);

        assertThat(custom.id()).isNotNull();
        assertThat(custom.projectTestCaseId()).isNotNull();
        assertThat(ptcRepository.findById(custom.projectTestCaseId()).orElseThrow().getCustomTestCase()).isNotNull();
        assertThatThrownBy(() -> customService.assign(projectId, custom.id(), coordinator.getId(), testerPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);

        UUID pointId = custom.decisionPoints().get(0).id();
        executionService.start(custom.projectTestCaseId(), testerPrincipal);
        var result = executionService.complete(custom.projectTestCaseId(), new CompleteExecutionRequest(List.of(pointId)), testerPrincipal);
        assertThat(result.executionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
    }

    @Test
    void customCaseSubmissionCreatesDraftAndTesterIsEditOnlyContributor() {
        UUID projectId = createProject();
        UUID membershipCase = publishMaster("B4-SUBMIT-MEMBER");
        UUID membershipPtc = planService.addMasterCase(projectId, membershipCase, ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        planService.assign(membershipPtc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        CustomTestCaseResponse custom = customService.create(projectId, request("B4-LIBRARY"), testerPrincipal);

        var submission = customService.submitToLibrary(projectId, custom.id(), testerPrincipal);
        assertThat(submission.draftVersionId()).isNotNull();
        assertThat(versionRepository.findById(submission.draftVersionId()).orElseThrow().getStatus().name()).isEqualTo("DRAFT");
        assertThatThrownBy(() -> lifecycleService.submitReview(submission.masterTestCaseId(), new LifecycleActionRequest("tester"), testerPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void capabilityApprovalUpdatesValueRunsGenerationAndRejectLeavesValueUnchanged() {
        UUID projectId = createProject();
        UUID membershipCase = publishMaster("B4-CAP-MEMBER");
        UUID membershipPtc = planService.addMasterCase(projectId, membershipCase, ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        planService.assign(membershipPtc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        CapabilityEntity capability = new CapabilityEntity(); capability.setCode("B4-CAP-" + UUID.randomUUID().toString().substring(0, 8)); capability.setName("BLE"); capability.setEnabled(true); capability = capabilityRepository.save(capability);

        var request = capabilityRequestService.submit(projectId, capability.getId(), new CapabilityUpdateRequestPayload(com.company.casehub.project.entity.ProjectCapabilityValue.YES, "Observed", "evidence-1"), testerPrincipal);
        assertThat(request.currentValue()).isEqualTo(com.company.casehub.project.entity.ProjectCapabilityValue.UNKNOWN);
        var approved = capabilityRequestService.review(request.id(), true, new ReviewRequestPayload("approved"), coordinatorPrincipal);
        assertThat(approved.status()).isEqualTo(CapabilityUpdateRequestStatus.APPROVED);
        assertThat(generationRunRepository.findByProjectIdOrderByExecutedAtDesc(projectId)).anyMatch(run -> run.getTriggerType() == GenerationTriggerType.CAPABILITY_UPDATE);
        var second = capabilityRequestService.submit(projectId, capability.getId(), new CapabilityUpdateRequestPayload(com.company.casehub.project.entity.ProjectCapabilityValue.NO, "Correction", null), testerPrincipal);
        var rejected = capabilityRequestService.review(second.id(), false, new ReviewRequestPayload("not enough proof"), coordinatorPrincipal);
        assertThat(rejected.status()).isEqualTo(CapabilityUpdateRequestStatus.REJECTED);
        assertThat(projectCapabilityRepository.findByProjectIdAndCapabilityId(projectId, capability.getId()).orElseThrow().getValue())
                .isEqualTo(com.company.casehub.project.entity.ProjectCapabilityValue.YES);
    }

    @Test
    void changeRequestApprovalCreatesLinkedRevisionAndContributor() {
        createProject();
        UUID masterId = publishMaster("B4-CHANGE");
        UUID sourceVersionId = versionRepository.findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(masterId).get(0).getId();
        var request = changeRequestService.submit(masterId, new TestCaseChangeRequestPayload(sourceVersionId, "clarify step"), testerPrincipal);
        var approved = changeRequestService.review(request.id(), true, new ReviewRequestPayload("create draft"), coordinatorPrincipal);
        assertThat(approved.status()).isEqualTo(TestCaseChangeRequestStatus.APPROVED);
        assertThat(approved.revisionDraftVersionId()).isNotNull();
        assertThat(versionRepository.findById(approved.revisionDraftVersionId()).orElseThrow().getChangeRequestId()).isEqualTo(request.id());
        assertThat(contributorRepository.existsByTestCaseVersionIdAndUserId(approved.revisionDraftVersionId(), tester.getId())).isTrue();
    }

    @Test
    void publishedVersionIsAvailableWithoutAutoUpgradeAndUpgradeKeepsPtcIdentity() {
        UUID projectId = createProject();
        UUID masterId = publishMaster("B4-UPGRADE");
        UUID ptcId = planService.addMasterCase(projectId, masterId, ProjectTestCaseSourceType.INITIAL, coordinatorPrincipal).id();
        planService.assign(ptcId, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
        UUID oldVersionId = ptcRepository.findById(ptcId).orElseThrow().getTestCaseVersion().getId();
        TestCaseDetailResponse revision = lifecycleService.createRevision(masterId, new com.company.casehub.testcase.dto.CreateRevisionRequest(oldVersionId, "v2"), coordinatorPrincipal);
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit v2"), coordinatorPrincipal);
        lifecycleService.publish(masterId, revision.draftVersion().id(), new LifecycleActionRequest("publish v2"), adminPrincipal);

        var available = versionUpgradeService.availability(ptcId, testerPrincipal);
        assertThat(available.newVersionAvailable()).isTrue();
        assertThat(ptcRepository.findById(ptcId).orElseThrow().getTestCaseVersion().getId()).isEqualTo(oldVersionId);
        var upgraded = versionUpgradeService.upgrade(ptcId, coordinatorPrincipal);
        assertThat(upgraded.upgraded()).isTrue();
        assertThat(upgraded.projectTestCaseId()).isEqualTo(ptcId);
        assertThat(ptcRepository.findById(ptcId).orElseThrow().getId()).isEqualTo(ptcId);
    }

    private CustomTestCaseRequest request(String code) {
        return new CustomTestCaseRequest(code + "-" + UUID.randomUUID().toString().substring(0, 6), "Custom case", "purpose", "pre", SelectionMode.SINGLE, false, null, null,
                List.of(new CustomStepRequest("step", "do it")), List.of(new CustomDecisionPointRequest("Done", "terminal", 1, TransitionType.PASS, List.of(), List.of())));
    }

    private UUID createProject() {
        return projectService.create(new ProjectCreateRequest("B4 Project " + UUID.randomUUID(), "Device", GenerationMode.FULL, List.of(standard.getId()), coordinator.getId()), coordinatorPrincipal).id();
    }

    private UUID publishMaster(String code) {
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(code + "-" + UUID.randomUUID().toString().substring(0, 6), category.getId(), code, "purpose", "pre", SelectionMode.SINGLE, false, null, null, null, List.of(new com.company.casehub.testcase.dto.StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"), adminPrincipal);
        return draft.id();
    }

    private UserPrincipal principal(UserEntity user, String role) {
        return new UserPrincipal(user.getId(), user.getUsername(), "hash", user.getDisplayName(), true, false, Set.of(role), Set.of(
                "project:read", "project:create", "project:update", "project_capability:read", "project_capability:update", "capability_request:create", "capability_request:review",
                "project_test_case:read", "project_test_case:add", "project_test_case:assign", "project_test_case:execute", "test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review", "test_case:review", "test_case:publish", "change_request:create", "change_request:review", "project_custom_test_case:create", "project_custom_test_case:edit_own_or_assigned"));
    }
}
