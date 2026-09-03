package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.execution.dto.AssigneeRequest;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.service.MyTestQueryService;
import com.company.casehub.execution.service.ProjectTestPlanService;
import com.company.casehub.generation.dto.GenerationRuleRequest;
import com.company.casehub.generation.dto.GenerationRunRequest;
import com.company.casehub.generation.entity.ConditionTargetType;
import com.company.casehub.generation.entity.GenerationOperator;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GenerationRunMode;
import com.company.casehub.generation.entity.GroupOperator;
import com.company.casehub.generation.entity.RecommendationStatus;
import com.company.casehub.generation.service.GenerationRuleService;
import com.company.casehub.generation.service.GenerationRuntimeService;
import com.company.casehub.project.dto.ProjectCapabilityRequest;
import com.company.casehub.project.dto.ProjectCreateRequest;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectCapabilitySource;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import com.company.casehub.project.service.CapabilityEngine;
import com.company.casehub.project.service.ProjectCapabilityService;
import com.company.casehub.project.service.ProjectService;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
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

/**
 * Batch 2 (Phase 9-14) vertical-slice integration test against real PostgreSQL.
 *
 * <p>Regression coverage added with the HIGH-A / HIGH-B fixes. The single
 * ordered test method provisions its own data so global GenerationRule/master
 * state from earlier phases never skews per-step assertions. HIGH-A: a
 * Generation run previously failed ("column created_at ... does not exist").
 * HIGH-B: capability-derived parents previously failed (updated_by NOT NULL).
 * Scenarios: derived capability parents, FULL run + reasons, Add (version
 * bound), Assign -> My Cases NEW -> markViewed, Remove/Restore row reuse,
 * Tester access-control, Ignore isolation across projects, UNKNOWN / Parent-NO
 * gate.
 */
class ProjectGenerationVerticalIT extends AbstractIntegrationTest {

    @Autowired private ProjectService projectService;
    @Autowired private ProjectCapabilityService capabilityService;
    @Autowired private CapabilityEngine capabilityEngine;
    @Autowired private GenerationRuleService ruleService;
    @Autowired private GenerationRuntimeService runtimeService;
    @Autowired private ProjectTestPlanService testPlanService;
    @Autowired private MyTestQueryService myTests;
    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseLifecycleService lifecycleService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CapabilityRepository capabilityRepository;
    @Autowired private StandardTaskTypeRepository standardRepository;
    @Autowired private TestCaseVersionRepository versionRepository;

    private UserPrincipal coordP;
    private UserPrincipal adminP;
    private UserPrincipal testerP;
    private UserEntity testerUser;
    private UserEntity coordUser;
    private CategoryEntity category;
    private StandardTaskTypeEntity standard;
    private CapabilityEntity capRoot;
    private CapabilityEntity capMid;
    private CapabilityEntity capGrand;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 6);
        RoleEntity coordRole = roleRepository.findByCode("TEST_COORDINATOR").orElseThrow();
        RoleEntity testerRole = roleRepository.findByCode("TESTER").orElseThrow();
        RoleEntity adminRole = roleRepository.findByCode("ADMIN").orElseThrow();

        category = new CategoryEntity();
        category.setCode("pg-cat-" + s);
        category.setName("PG Category");
        category.setLevel(1);
        category = categoryRepository.save(category);

        standard = new StandardTaskTypeEntity();
        standard.setCode("PG-STD-" + s);
        standard.setName("PG Standard");
        standard.setType("STANDARD");
        standard.setEnabled(true);
        standard = standardRepository.save(standard);

        capRoot = saveCap("pg-root-" + s, "Root", null, 1);
        capMid = saveCap("pg-mid-" + s, "Mid", capRoot.getId(), 2);
        capGrand = saveCap("pg-grand-" + s, "Grand", capMid.getId(), 3);

        coordUser = userRepository.save(new UserEntity("pg_coord_" + s, "PG Coord", "h"));
        testerUser = userRepository.save(new UserEntity("pg_tester_" + s, "PG Tester", "h"));
        UserEntity adminUser = userRepository.save(new UserEntity("pg_admin_" + s, "PG Admin", "h"));
        userRoleRepository.save(new UserRoleEntity(coordUser, coordRole));
        userRoleRepository.save(new UserRoleEntity(testerUser, testerRole));
        userRoleRepository.save(new UserRoleEntity(adminUser, adminRole));

        coordP = principal(coordUser, "TEST_COORDINATOR", "project:read", "project:create", "project:update",
                "project_capability:read", "project_capability:update", "project_test_case:read",
                "project_test_case:add", "project_test_case:remove", "project_test_case:restore",
                "project_test_case:assign", "generation_rule:read", "generation:run",
                "generation:review_recommendation", "test_case:read", "test_case:draft_create",
                "test_case:draft_edit", "test_case:submit_review");
        adminP = principal(adminUser, "ADMIN", "project:read", "project:create", "project_capability:update",
                "generation_rule:manage", "generation_rule:read", "project_test_case:add",
                "project_test_case:assign", "generation:run", "generation:review_recommendation",
                "test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review",
                "test_case:review", "test_case:publish", "test_case:deprecate");
        testerP = principal(testerUser, "TESTER", "project:read", "project_test_case:read");
    }

    private UserPrincipal principal(UserEntity user, String role, String... authorities) {
        return new UserPrincipal(user.getId(), user.getUsername(), "h", user.getDisplayName(),
                true, false, Set.of(role), Set.of(authorities));
    }

    private CapabilityEntity saveCap(String code, String name, UUID parentId, int order) {
        CapabilityEntity c = new CapabilityEntity();
        c.setCode(code);
        c.setName(name);
        c.setParentId(parentId);
        c.setSortOrder(order);
        c.setEnabled(true);
        return capabilityRepository.save(c);
    }

    private UUID masterPublished(String code) {
        TestCaseDetailResponse draft = draftService.createDraft(new CreateDraftRequest(code, category.getId(),
                "Case " + code, "purpose", "pre", SelectionMode.SINGLE, false, null, "notes", null,
                List.of(new StepRequest("Step", "act")), List.of(), List.of(), List.of()), coordP);
        UUID masterId = draft.id();
        UUID draftVersionId = draft.draftVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordP);
        lifecycleService.publish(masterId, draftVersionId, new LifecycleActionRequest("ok"), adminP);
        return masterId;
    }

    private UUID newProject(GenerationMode mode) {
        return projectService.create(new ProjectCreateRequest(
                "PG Project " + UUID.randomUUID().toString().substring(0, 6),
                "Device", mode, List.of(standard.getId()), coordUser.getId()), coordP).id();
    }

    @Test
    void batch2VerticalAcceptanceOrdered() {
        // ---- Scenario A: FULL vertical (capability derivation + generation + plan + my tests).
        UUID m1 = masterPublished("PG-M1-" + UUID.randomUUID().toString().substring(0, 6));
        UUID m2 = masterPublished("PG-M2-" + UUID.randomUUID().toString().substring(0, 6));
        UUID projectA = newProject(GenerationMode.FULL);

        // HIGH-B regression: setting capMid YES derives capRoot YES/derived without updated_by NULL.
        capabilityService.setValue(projectA, capMid.getId(),
                new ProjectCapabilityRequest(ProjectCapabilityValue.YES, ProjectCapabilitySource.COORDINATOR_INPUT, "qa"),
                coordP);
        assertThat(capabilityService.list(projectA, coordP).stream()
                .filter(r -> r.capabilityId().equals(capRoot.getId())).findFirst().orElseThrow().value())
                .isEqualTo(ProjectCapabilityValue.YES);

        ruleService.create(new GenerationRuleRequest(
                "PG-RULE-A-" + UUID.randomUUID().toString().substring(0, 6), "Rule A", null,
                GenerationRuleMode.FULL, GenerationRuleStatus.ENABLED,
                List.of(new GenerationRuleRequest.GroupRequest(null, GroupOperator.AND, 0,
                        List.of(new GenerationRuleRequest.ConditionRequest(ConditionTargetType.CAPABILITY,
                                capMid.getId(), null, GenerationOperator.EQ_YES, 0)))),
                List.of(m1, m2)), adminP);

        // HIGH-A regression: run must succeed.
        var runA = runtimeService.run(projectA, new GenerationRunRequest(GenerationRunMode.FULL, null), coordP);
        var recsA = runtimeService.listRecommendations(runA.id(), coordP);
        assertThat(recsA).hasSize(2);
        assertThat(recsA).allSatisfy(r -> assertThat(r.recommendedBecause()).isNotEmpty());

        var addM1 = recsA.stream().filter(r -> r.masterTestCaseId().equals(m1)).findFirst().orElseThrow();
        assertThat(runtimeService.add(addM1.id(), coordP).status()).isEqualTo(RecommendationStatus.ADDED);
        var plan = testPlanService.list(projectA, coordP);
        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).masterTestCaseId()).isEqualTo(m1);
        assertThat(plan.get(0).sources()).contains(ProjectTestCaseSourceType.GENERATED);

        UUID ptcId = plan.get(0).id();
        testPlanService.assign(ptcId, new AssigneeRequest(testerUser.getId()), coordP);
        var my = myTests.listMyCases(testerP);
        assertThat(my).hasSize(1);
        assertThat(my.get(0).newCase()).isTrue();
        assertThat(my.get(0).readOnly()).isFalse();
        assertThat(myTests.markViewed(ptcId, testerP).newCase()).isFalse();

        assertThat(testPlanService.remove(ptcId, coordP).removed()).isTrue();
        assertThat(testPlanService.restore(ptcId, coordP).removed()).isFalse();

        var allCases = myTests.listProjectCases(projectA, testerP);
        assertThat(allCases).hasSize(1);
        assertThat(allCases.get(0).assignedToMe()).isTrue();
        assertThatThrownBy(() -> testPlanService.remove(ptcId, testerP))
                .isInstanceOf(ForbiddenOperationException.class);

        // ---- Scenario B: Ignore isolation across projects (independent project B/C).
        UUID m3 = masterPublished("PG-M3-" + UUID.randomUUID().toString().substring(0, 6));
        UUID projectB = newProject(GenerationMode.FULL);
        UUID projectC = newProject(GenerationMode.FULL);
        // A rule conditioned on capRoot EQ_UNKNOWN (true for fresh project B/C; capRoot is per-project).
        ruleService.create(new GenerationRuleRequest(
                "PG-RULE-B-" + UUID.randomUUID().toString().substring(0, 6), "Rule B", null,
                GenerationRuleMode.FULL, GenerationRuleStatus.ENABLED,
                List.of(new GenerationRuleRequest.GroupRequest(null, GroupOperator.AND, 0,
                        List.of(new GenerationRuleRequest.ConditionRequest(ConditionTargetType.CAPABILITY,
                                capRoot.getId(), null, GenerationOperator.EQ_UNKNOWN, 0)))),
                List.of(m3)), adminP);

        var runB = runtimeService.run(projectB, new GenerationRunRequest(GenerationRunMode.FULL, null), coordP);
        runtimeService.ignore(runB.recommendations().get(0).id(), true, coordP);
        var runB2 = runtimeService.run(projectB, new GenerationRunRequest(GenerationRunMode.FULL, null), coordP);
        assertThat(runtimeService.listRecommendations(runB2.id(), coordP).get(0).status())
                .isEqualTo(RecommendationStatus.IGNORED);

        var runC = runtimeService.run(projectC, new GenerationRunRequest(GenerationRunMode.FULL, null), coordP);
        assertThat(runtimeService.listRecommendations(runC.id(), coordP).get(0).status())
                .isEqualTo(RecommendationStatus.NEW);

        runtimeService.ignore(runB2.recommendations().get(0).id(), false, coordP);
        var runB3 = runtimeService.run(projectB, new GenerationRunRequest(GenerationRunMode.FULL, null), coordP);
        assertThat(runtimeService.listRecommendations(runB3.id(), coordP).get(0).status())
                .isEqualTo(RecommendationStatus.NEW);

        // ---- Scenario C: UNKNOWN default + Parent-NO gate.
        UUID projectD = newProject(GenerationMode.FULL);
        assertThat(capabilityEngine.resolveEffectiveValue(projectD, capRoot.getId()).value())
                .isEqualTo(ProjectCapabilityValue.UNKNOWN);
        capabilityService.setValue(projectD, capMid.getId(),
                new ProjectCapabilityRequest(ProjectCapabilityValue.NO, ProjectCapabilitySource.COORDINATOR_INPUT, null),
                coordP);
        var mid = capabilityEngine.resolveEffectiveValue(projectD, capMid.getId());
        assertThat(mid.value()).isEqualTo(ProjectCapabilityValue.NO);
        assertThat(mid.applicable()).isTrue();
        // Grandchild of a NO parent is not applicable during rule matching.
        var gate = capabilityEngine.resolveEffectiveValue(projectD, capGrand.getId());
        assertThat(gate.value()).isEqualTo(ProjectCapabilityValue.UNKNOWN);
        assertThat(gate.applicable()).isFalse();
    }
}
