package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.customcase.dto.CustomDecisionPointRequest;
import com.company.casehub.customcase.dto.CustomStepRequest;
import com.company.casehub.customcase.dto.CustomTestCaseRequest;
import com.company.casehub.customcase.dto.CustomTestCaseResponse;
import com.company.casehub.customcase.service.CustomTestCaseService;
import com.company.casehub.execution.dto.AssigneeRequest;
import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.service.ProjectTestPlanService;
import com.company.casehub.project.dto.ProjectCreateRequest;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.service.ProjectService;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TransitionType;
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
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Independent QA verification of the Batch 4 Static Review fixes (commit 7983bf6).
 *
 * These scenarios deliberately target blind spots the DEV regression suite leaves open:
 *  - HIGH-B: the tester self-assignment that CustomTestCaseService.create() performs is
 *    actually persisted to project_test_case_assignees (DEV only exercised the
 *    coordinator-driven assign() gate, never the auto self-assignment path).
 *  - MEDIUM-A: the two UNIQUE constraints V017 adds really exist in PostgreSQL (DEV only
 *    proved service-layer interception, never the DB backstop). Also proves the service
 *    rejects a duplicate custom transition target on update.
 *
 * Independent of the DEV Batch4QAVerificationIT scenarios.
 */
class StaticReviewQAVerificationIT extends AbstractIntegrationTest {

    @Autowired private CustomTestCaseService customService;
    @Autowired private ProjectTestPlanService planService;
    @Autowired private ProjectService projectService;
    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseLifecycleService lifecycleService;
    @Autowired private ProjectTestCaseAssigneeRepository assigneeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private StandardTaskTypeRepository standardRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DataSource dataSource;

    private UserEntity coordinator;
    private UserEntity tester;
    private UserPrincipal coordinatorPrincipal;
    private UserPrincipal testerPrincipal;
    private UserPrincipal adminPrincipal;
    private StandardTaskTypeEntity standard;
    private CategoryEntity category;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        standard = new StandardTaskTypeEntity();
        standard.setCode("sr-std-" + suffix);
        standard.setName("SR Standard");
        standard.setType("STANDARD");
        standard.setEnabled(true);
        standard = standardRepository.save(standard);

        category = new CategoryEntity();
        category.setCode("sr-cat-" + suffix);
        category.setName("SR Category");
        category.setLevel(1);
        category = categoryRepository.save(category);

        RoleEntity coordRole = roleRepository.findByCode("TEST_COORDINATOR").orElseThrow();
        RoleEntity testerRole = roleRepository.findByCode("TESTER").orElseThrow();
        RoleEntity adminRole = roleRepository.findByCode("ADMIN").orElseThrow();

        coordinator = userRepository.save(new UserEntity("sr_coord_" + suffix, "SR Coordinator", "hash"));
        tester = userRepository.save(new UserEntity("sr_tester_" + suffix, "SR Tester", "hash"));
        UserEntity admin = userRepository.save(new UserEntity("sr_admin_" + suffix, "SR Admin", "hash"));
        userRoleRepository.save(new UserRoleEntity(coordinator, coordRole));
        userRoleRepository.save(new UserRoleEntity(tester, testerRole));
        userRoleRepository.save(new UserRoleEntity(admin, adminRole));

        coordinatorPrincipal = principal(coordinator, "TEST_COORDINATOR");
        testerPrincipal = principal(tester, "TESTER");
        adminPrincipal = principal(admin, "ADMIN");
    }

    @Test
    void testerCreatePersistsAutoSelfAssignment() {
        UUID projectId = newProject();
        joinAsTester(projectId);

        CustomTestCaseResponse custom = customService.create(projectId, customRequest("SR-SELFASSIGN"),
                testerPrincipal);

        // HIGH-B: a TESTER creating a Custom Case is auto-self-assigned to its Project Test Case.
        boolean selfAssigned = assigneeRepository.existsByProjectTestCaseIdAndUserId(
                custom.projectTestCaseId(), tester.getId());
        assertThat(selfAssigned)
                .as("TESTER-created Custom Case must be auto-self-assigned")
                .isTrue();

        List<ProjectTestCaseAssigneeEntity> assignees = assigneeRepository
                .findByProjectTestCaseId(custom.projectTestCaseId());
        assertThat(assignees).singleElement()
                .satisfies(a -> assertThat(a.getUser().getId()).isEqualTo(tester.getId()));
    }

    @Test
    void updateExistingCustomCaseWithDefinitionsDoesNotViolateSequenceOrOrderUnique() {
        // Minimal repro of a HIGH defect introduced by the Static Review fix (7983bf6):
        // CustomTestCaseService.update() -> replaceDefinition() clears the step and decision
        // point collections (orphanRemoval) and re-inserts rows with sequence_no/display_order
        // = 1..n. It does NOT bulk-delete + flush first (unlike DecisionPointService.
        // replaceTransition). The Static Review added UNIQUE(custom_test_case_id, display_order)
        // and the existing uq_custom_test_steps_sequence make the single flush fail when the new
        // INSERT is ordered ahead of the orphan DELETE -> DataIntegrityViolationException -> 500.
        // Any custom case that already has a step or a decision point cannot be updated.
        UUID projectId = newProject();
        joinAsTester(projectId);

        CustomTestCaseResponse created = customService.create(projectId,
                new CustomTestCaseRequest("SR-UPD-" + UUID.randomUUID().toString().substring(0, 6), "Custom case",
                        "purpose", "pre", SelectionMode.SINGLE, false, null, null,
                        List.of(new CustomStepRequest("step-1", "content one")),
                        List.of(new CustomDecisionPointRequest("DP", "desc", 1, TransitionType.PASS,
                                List.of(), List.of()))),
                testerPrincipal);

        CustomTestCaseRequest changed = new CustomTestCaseRequest(
                "SR-UPD-" + UUID.randomUUID().toString().substring(0, 6), "Custom case v2",
                "purpose2", "pre2", SelectionMode.SINGLE, false, null, null,
                List.of(new CustomStepRequest("step-1", "content one CHANGED")),
                List.of(new CustomDecisionPointRequest("DP2", "desc2", 1, TransitionType.PASS,
                        List.of(), List.of())));

        // This throws DataIntegrityViolationException on uq_decision_points_custom_order
        // (or uq_custom_test_steps_sequence): update() cannot replace a case definition that
        // already carries steps / decision points.
        CustomTestCaseResponse updated = customService.update(projectId, created.id(), changed, testerPrincipal);
        assertThat(updated.id()).isEqualTo(created.id());

        CustomTestCaseResponse persisted = customService.list(projectId, testerPrincipal).stream()
                .filter(item -> item.id().equals(created.id()))
                .findFirst().orElseThrow();
        assertThat(persisted.steps()).singleElement()
                .satisfies(step -> {
                    assertThat(step.id()).isNotEqualTo(created.steps().get(0).id());
                    assertThat(step.content()).isEqualTo("content one CHANGED");
                });
        assertThat(persisted.decisionPoints()).singleElement()
                .satisfies(point -> {
                    assertThat(point.id()).isNotEqualTo(created.decisionPoints().get(0).id());
                    assertThat(point.name()).isEqualTo("DP2");
                });
    }

    @Test
    void v017UniqueConstraintsExistInPostgres() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> customOrder = jdbc.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE table_schema = 'casehub' AND table_name = 'decision_points' " +
                        "AND constraint_name = 'uq_decision_points_custom_order'", String.class);
        assertThat(customOrder)
                .as("V017 must add uq_decision_points_custom_order on decision_points")
                .containsExactly("uq_decision_points_custom_order");

        List<String> transitionTarget = jdbc.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE table_schema = 'casehub' AND table_name = 'transition_targets' " +
                        "AND constraint_name = 'uq_transition_targets_custom'", String.class);
        assertThat(transitionTarget)
                .as("V017 must add uq_transition_targets_custom on transition_targets")
                .containsExactly("uq_transition_targets_custom");
    }

    // ------------------------------------------------------------------ helpers

    private UUID newProject() {
        return projectService.create(new ProjectCreateRequest("SR Project " + UUID.randomUUID(), "Device",
                GenerationMode.FULL, List.of(standard.getId()), coordinator.getId()), coordinatorPrincipal).id();
    }

    private void joinAsTester(UUID projectId) {
        UUID membership = publishMaster("SR-JOIN");
        UUID ptc = planService.addMasterCase(projectId, membership, ProjectTestCaseSourceType.INITIAL,
                coordinatorPrincipal).id();
        planService.assign(ptc, new AssigneeRequest(tester.getId()), coordinatorPrincipal);
    }

    private UUID publishMaster(String prefix) {
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(
                prefix + "-" + UUID.randomUUID().toString().substring(0, 6), category.getId(), prefix, "purpose",
                "pre", SelectionMode.SINGLE, false, null, null, null,
                List.of(new StepRequest("step", "act")), List.of(), List.of(), List.of()), coordinatorPrincipal);
        lifecycleService.submitReview(draft.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(draft.id(), draft.visibleVersion().id(), new LifecycleActionRequest("publish"),
                adminPrincipal);
        return draft.id();
    }

    private CustomTestCaseRequest customRequest(String code) {
        return new CustomTestCaseRequest(code + "-" + UUID.randomUUID().toString().substring(0, 6), "Custom case",
                "purpose", "pre", SelectionMode.SINGLE, false, null, null,
                List.of(new CustomStepRequest("step", "do it")),
                List.of(new CustomDecisionPointRequest("Done", "transition", 1, TransitionType.PASS,
                        List.of(), List.of())));
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
                "test_case:review", "test_case:publish", "test_case:deprecate", "change_request:create",
                "change_request:review", "project_custom_test_case:create",
                "project_custom_test_case:edit_own_or_assigned"));
    }
}
