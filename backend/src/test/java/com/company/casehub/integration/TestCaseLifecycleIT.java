package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.DecisionPointRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.dto.UpdateDraftRequest;
import com.company.casehub.testcase.dto.VersionSummaryResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TransitionType;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TestStepEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseReviewRecordRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.TestStepRepository;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.DecisionPointService;
import com.company.casehub.testcase.service.TestCaseLifecycleService;
import com.company.casehub.testcase.service.TestCaseQueryService;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * PostgreSQL Testcontainers integration test for the Phase 7 Test Case
 * Lifecycle. Exercises the full state machine against a real PostgreSQL 16
 * instance with Flyway migrations, covering every hard rule: lifecycle
 * transitions, Published Immutable, Reject-keeps-REVIEW, single current
 * published, version-number server control, review-record append-only, and
 * resource-level permissions.
 */
class TestCaseLifecycleIT extends AbstractIntegrationTest {

    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseLifecycleService lifecycleService;
    @Autowired private TestCaseQueryService queryService;
    @Autowired private DecisionPointService decisionPointService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MasterTestCaseRepository masterRepository;
    @Autowired private TestCaseVersionRepository versionRepository;
    @Autowired private TestStepRepository stepRepository;
    @Autowired private TestCaseReviewRecordRepository reviewRecordRepository;

    private UserEntity coordinator;
    private UserEntity adminUser;
    private CategoryEntity category;
    private UserPrincipal coordinatorPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        coordinator = userRepository.save(new UserEntity("it_coord_" + suffix, "IT Coordinator", "hash"));
        adminUser = userRepository.save(new UserEntity("it_admin_" + suffix, "IT Admin", "hash"));
        category = new CategoryEntity();
        category.setCode("it-cat-" + suffix);
        category.setName("IT Category");
        category.setLevel(1);
        category = categoryRepository.save(category);
        coordinatorPrincipal = new UserPrincipal(coordinator.getId(), coordinator.getUsername(), "hash",
                coordinator.getDisplayName(), true, false, Set.of("TEST_COORDINATOR"),
                Set.of("test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review"));
        adminPrincipal = new UserPrincipal(adminUser.getId(), adminUser.getUsername(), "hash",
                adminUser.getDisplayName(), true, false, Set.of("ADMIN"),
                Set.of("test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review",
                        "test_case:review", "test_case:publish", "test_case:deprecate"));
    }

    // -------------------------------------------------------------------------
    // Phase 8 — Decision Point / Master Logic Graph
    // -------------------------------------------------------------------------

    @Test
    void decisionPointCrudAndMasterGraphUseMasterTargets() {
        TestCaseDetailResponse root = createDraft("P8-CRUD-" + UUID.randomUUID().toString().substring(0, 8), "Graph Root");
        TestCaseDetailResponse target = createDraft("P8-TARGET-" + UUID.randomUUID().toString().substring(0, 8), "Graph Target");
        UUID versionId = root.visibleVersion().id();

        var created = decisionPointService.create(root.id(), versionId,
                new DecisionPointRequest("Device reachable", "Branch when the device responds", 1,
                        TransitionType.NEXT_CASE, List.of(target.id())), coordinatorPrincipal);
        assertThat(created.name()).isEqualTo("Device reachable");
        assertThat(created.displayOrder()).isEqualTo(1);
        assertThat(created.transition().type()).isEqualTo(TransitionType.NEXT_CASE);
        assertThat(created.transition().targets()).singleElement().satisfies(link -> {
            assertThat(link.masterTestCaseId()).isEqualTo(target.id());
            assertThat(link.caseCode()).isEqualTo(target.caseCode());
        });

        var graph = decisionPointService.graph(root.id(), versionId, coordinatorPrincipal);
        assertThat(graph.rootMasterTestCaseId()).isEqualTo(root.id());
        assertThat(graph.nodes()).extracting("masterTestCaseId").contains(root.id(), target.id());
        assertThat(graph.edges()).singleElement().satisfies(edge -> {
            assertThat(edge.sourceMasterTestCaseId()).isEqualTo(root.id());
            assertThat(edge.targetMasterTestCaseId()).isEqualTo(target.id());
        });

        var updated = decisionPointService.update(root.id(), versionId, created.id(),
                new DecisionPointRequest("Device not reachable", null, 2, TransitionType.FAIL, List.of()), coordinatorPrincipal);
        assertThat(updated.displayOrder()).isEqualTo(2);
        assertThat(updated.transition().type()).isEqualTo(TransitionType.FAIL);
        assertThat(updated.transition().targets()).isEmpty();
        decisionPointService.delete(root.id(), versionId, created.id(), coordinatorPrincipal);
        assertThat(decisionPointService.list(root.id(), versionId, coordinatorPrincipal)).isEmpty();
    }

    @Test
    void dagRejectsTwoNodeAndThreeNodeCycles() {
        TestCaseDetailResponse a = createDraft("P8-CYCLE-A-" + UUID.randomUUID().toString().substring(0, 8), "Cycle A");
        TestCaseDetailResponse b = createDraft("P8-CYCLE-B-" + UUID.randomUUID().toString().substring(0, 8), "Cycle B");
        UUID av = a.visibleVersion().id();
        UUID bv = b.visibleVersion().id();
        decisionPointService.create(a.id(), av, new DecisionPointRequest("to B", null, 1, TransitionType.NEXT_CASE, List.of(b.id())), coordinatorPrincipal);
        assertThatThrownBy(() -> decisionPointService.create(b.id(), bv,
                new DecisionPointRequest("to A", null, 1, TransitionType.NEXT_CASE, List.of(a.id())), coordinatorPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DAG_CYCLE_DETECTED);

        TestCaseDetailResponse c = createDraft("P8-CYCLE-C-" + UUID.randomUUID().toString().substring(0, 8), "Cycle C");
        decisionPointService.create(b.id(), bv, new DecisionPointRequest("to C", null, 1, TransitionType.NEXT_CASE, List.of(c.id())), coordinatorPrincipal);
        assertThatThrownBy(() -> decisionPointService.create(c.id(), c.visibleVersion().id(),
                new DecisionPointRequest("to A", null, 1, TransitionType.NEXT_CASE, List.of(a.id())), coordinatorPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DAG_CYCLE_DETECTED);
    }

    @Test
    void publishedVersionLogicIsImmutable() {
        TestCaseDetailResponse root = createDraft("P8-IMMUTABLE-" + UUID.randomUUID().toString().substring(0, 8), "Published Logic");
        UUID versionId = root.visibleVersion().id();
        lifecycleService.submitReview(root.id(), new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(root.id(), versionId, new LifecycleActionRequest("publish"), adminPrincipal);

        assertThatThrownBy(() -> decisionPointService.create(root.id(), versionId,
                new DecisionPointRequest("forbidden", null, 1, TransitionType.PASS, List.of()), adminPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_VERSION_IMMUTABLE);
    }

    @Test
    void updateRedirectsAndRescalesTransitionTargetsWithoutUniqueViolation() {
        // HIGH-A regression: updating a decision point whose new and old transitions
        // both carry >=1 target must not collide on uq_transition_targets_order.
        TestCaseDetailResponse a = createDraft("P8-UPD-A-" + UUID.randomUUID().toString().substring(0, 8), "Upd A");
        TestCaseDetailResponse b = createDraft("P8-UPD-B-" + UUID.randomUUID().toString().substring(0, 8), "Upd B");
        TestCaseDetailResponse c = createDraft("P8-UPD-C-" + UUID.randomUUID().toString().substring(0, 8), "Upd C");
        UUID av = a.visibleVersion().id();
        UUID pointId = decisionPointService.create(a.id(), av,
                new DecisionPointRequest("branch", null, 1, TransitionType.NEXT_CASE, List.of(b.id())), coordinatorPrincipal).id();

        // Redirect NEXT_CASE target B -> C (both new and old have exactly 1 target).
        var redirected = decisionPointService.update(a.id(), av, pointId,
                new DecisionPointRequest("branch", null, 1, TransitionType.NEXT_CASE, List.of(c.id())), coordinatorPrincipal);
        assertThat(redirected.transition().targets()).singleElement()
                .satisfies(link -> assertThat(link.masterTestCaseId()).isEqualTo(c.id()));

        // Scale NEXT_CASE(1) -> NEXT_CASES(2): B and C as two targets.
        var scaledUp = decisionPointService.update(a.id(), av, pointId,
                new DecisionPointRequest("branch", null, 1, TransitionType.NEXT_CASES, List.of(b.id(), c.id())), coordinatorPrincipal);
        assertThat(scaledUp.transition().targets()).extracting("masterTestCaseId").containsExactlyInAnyOrder(b.id(), c.id());

        // Scale back NEXT_CASES(2) -> NEXT_CASE(1): only B.
        var scaledDown = decisionPointService.update(a.id(), av, pointId,
                new DecisionPointRequest("branch", null, 1, TransitionType.NEXT_CASE, List.of(b.id())), coordinatorPrincipal);
        assertThat(scaledDown.transition().targets()).singleElement()
                .satisfies(link -> assertThat(link.masterTestCaseId()).isEqualTo(b.id()));

        // Reduce to a terminal transition (0 targets) then back up again.
        decisionPointService.update(a.id(), av, pointId,
                new DecisionPointRequest("branch", null, 1, TransitionType.FAIL, List.of()), coordinatorPrincipal);
        var backUp = decisionPointService.update(a.id(), av, pointId,
                new DecisionPointRequest("branch", null, 1, TransitionType.NEXT_CASE, List.of(c.id())), coordinatorPrincipal);
        assertThat(backUp.transition().targets()).singleElement()
                .satisfies(link -> assertThat(link.masterTestCaseId()).isEqualTo(c.id()));

        // The logic graph must reflect ONLY the latest target after each update.
        var graph = decisionPointService.graph(a.id(), av, coordinatorPrincipal);
        assertThat(graph.edges()).extracting("targetMasterTestCaseId").containsExactly(c.id());
        assertThat(graph.edges()).extracting("targetMasterTestCaseId").doesNotContain(b.id());
    }

    // -------------------------------------------------------------------------
    // Full lifecycle: Draft → Review → Published → Deprecated
    // -------------------------------------------------------------------------

    @Test
    void fullLifecycleFromDraftToDeprecated() {
        TestCaseDetailResponse created = createDraft("LC-001", "Full Lifecycle Case");
        UUID masterId = created.id();
        UUID draftVersionId = created.visibleVersion().id();

        // Submit for review
        TestCaseDetailResponse afterSubmit = lifecycleService.submitReview(
                masterId, new LifecycleActionRequest("ready for review"), coordinatorPrincipal);
        assertThat(versionStatus(masterId, draftVersionId)).isEqualTo(TestCaseVersionStatus.REVIEW);

        // Publish (admin)
        lifecycleService.publish(masterId, draftVersionId, new LifecycleActionRequest("approved"), adminPrincipal);
        assertThat(versionStatus(masterId, draftVersionId)).isEqualTo(TestCaseVersionStatus.PUBLISHED);
        TestCaseVersionEntity published = versionRepository.findById(draftVersionId).orElseThrow();
        assertThat(published.isCurrentVersion()).isTrue();
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.isRevisionClosed()).isTrue();

        // Deprecate
        lifecycleService.deprecate(masterId, draftVersionId, new LifecycleActionRequest("obsolete"), adminPrincipal);
        assertThat(versionStatus(masterId, draftVersionId)).isEqualTo(TestCaseVersionStatus.DEPRECATED);
        TestCaseVersionEntity deprecated = versionRepository.findById(draftVersionId).orElseThrow();
        assertThat(deprecated.isCurrentVersion()).isFalse();
        assertThat(deprecated.getDeprecatedAt()).isNotNull();

        // Review records: SUBMIT, PUBLISH, DEPRECATE — in order
        var records = reviewRecordRepository.findByTestCaseVersionIdOrderByCreatedAtAscIdAsc(draftVersionId);
        assertThat(records).extracting("action").containsExactly(
                ReviewRecordAction.SUBMIT, ReviewRecordAction.PUBLISH, ReviewRecordAction.DEPRECATE);
    }

    // -------------------------------------------------------------------------
    // Single current published — publishing a new version clears the prior
    // -------------------------------------------------------------------------

    @Test
    void publishingNewVersionSwitchesCurrentFlag() {
        TestCaseDetailResponse created = createDraft("LC-002", "Current Switch");
        UUID masterId = created.id();
        UUID firstVersionId = created.visibleVersion().id();

        // Publish v1.0
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit 1"), coordinatorPrincipal);
        lifecycleService.publish(masterId, firstVersionId, new LifecycleActionRequest("publish 1"), adminPrincipal);
        assertThat(versionRepository.findById(firstVersionId).orElseThrow().isCurrentVersion()).isTrue();

        // Create revision → v1.1 draft
        TestCaseDetailResponse afterRevision = lifecycleService.createRevision(
                masterId, new CreateRevisionRequest(null, "minor update"), coordinatorPrincipal);
        UUID revisionVersionId = afterRevision.draftVersion() != null
                ? afterRevision.draftVersion().id()
                : findLatestDraft(masterId).getId();

        // Submit + publish v1.1 — v1.0 must lose current_version
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit revision"), coordinatorPrincipal);
        lifecycleService.publish(masterId, revisionVersionId, new LifecycleActionRequest("publish revision"), adminPrincipal);

        assertThat(versionRepository.findById(firstVersionId).orElseThrow().isCurrentVersion()).isFalse();
        assertThat(versionRepository.findById(revisionVersionId).orElseThrow().isCurrentVersion()).isTrue();
        assertThat(versionRepository.findById(revisionVersionId).orElseThrow().getStatus()).isEqualTo(TestCaseVersionStatus.PUBLISHED);

        // DB invariant: exactly one current published version
        List<TestCaseVersionEntity> currentPublished = versionRepository
                .findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(masterId).stream()
                .filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .toList();
        assertThat(currentPublished).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Version number server-controlled — major=source.major, minor=MAX+1
    // -------------------------------------------------------------------------

    @Test
    void createRevisionComputesNextMinorFromMaxOfSameMajor() {
        TestCaseDetailResponse created = createDraft("LC-003", "Version Numbering");
        UUID masterId = created.id();
        UUID v10 = created.visibleVersion().id();

        // Publish v1.0
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, v10, new LifecycleActionRequest("publish"), adminPrincipal);

        // Create revision → should be v1.1
        lifecycleService.createRevision(masterId, new CreateRevisionRequest(null, "rev 1"), coordinatorPrincipal);
        TestCaseVersionEntity revision1 = findLatestDraft(masterId);
        assertThat(revision1.getVersionMajor()).isEqualTo(1);
        assertThat(revision1.getVersionMinor()).isEqualTo(1);
        assertThat(revision1.getBasedOnVersion().getId()).isEqualTo(v10);
        assertThat(revision1.getChangeReason()).isEqualTo("rev 1");

        // Publish v1.1, then create another revision → v1.2
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit v1.1"), coordinatorPrincipal);
        lifecycleService.publish(masterId, revision1.getId(), new LifecycleActionRequest("publish v1.1"), adminPrincipal);
        lifecycleService.createRevision(masterId, new CreateRevisionRequest(null, "rev 2"), coordinatorPrincipal);
        TestCaseVersionEntity revision2 = findLatestDraft(masterId);
        assertThat(revision2.getVersionMajor()).isEqualTo(1);
        assertThat(revision2.getVersionMinor()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Published Immutable — published content is never mutated
    // -------------------------------------------------------------------------

    @Test
    void publishedVersionContentIsImmutableAfterRevision() {
        TestCaseDetailResponse created = createDraft("LC-004", "Immutable Content");
        UUID masterId = created.id();
        UUID v10 = created.visibleVersion().id();

        // Publish v1.0
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, v10, new LifecycleActionRequest("publish"), adminPrincipal);

        // Snapshot published content (load steps via an explicit query to avoid
        // lazy-loading the version's collection outside a transaction).
        TestCaseVersionEntity published = versionRepository.findById(v10).orElseThrow();
        String originalCaseName = published.getCaseName();
        List<TestStepEntity> originalSteps = stepRepository.findByTestCaseVersionId(v10);
        int originalStepCount = originalSteps.size();
        String originalFirstStepContent = originalSteps.stream()
                .min(Comparator.comparingInt(TestStepEntity::getSequenceNo))
                .map(TestStepEntity::getContent).orElseThrow();

        // Create revision and modify the revision's content
        TestCaseDetailResponse afterRevision = lifecycleService.createRevision(
                masterId, new CreateRevisionRequest(null, "content change"), coordinatorPrincipal);
        UUID revisionId = findLatestDraft(masterId).getId();

        // Published version content must remain unchanged
        TestCaseVersionEntity stillPublished = versionRepository.findById(v10).orElseThrow();
        assertThat(stillPublished.getCaseName()).isEqualTo(originalCaseName);
        assertThat(stepRepository.findByTestCaseVersionId(v10)).hasSize(originalStepCount);
        assertThat(stepRepository.findByTestCaseVersionId(v10).stream()
                .min(Comparator.comparingInt(TestStepEntity::getSequenceNo))
                .map(TestStepEntity::getContent).orElseThrow()).isEqualTo(originalFirstStepContent);
        assertThat(stillPublished.getStatus()).isEqualTo(TestCaseVersionStatus.PUBLISHED);
    }

    // -------------------------------------------------------------------------
    // Reject — keeps REVIEW, sets revision_closed=true
    // -------------------------------------------------------------------------

    @Test
    void rejectKeepsReviewStatusAndClosesRevision() {
        TestCaseDetailResponse created = createDraft("LC-005", "Reject Flow");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        assertThat(versionStatus(masterId, versionId)).isEqualTo(TestCaseVersionStatus.REVIEW);

        // Reject (admin)
        lifecycleService.reject(masterId, versionId, new LifecycleActionRequest("non-compliant steps"), adminPrincipal);

        // R2: status stays REVIEW — no REJECTED status exists
        TestCaseVersionEntity rejected = versionRepository.findById(versionId).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(TestCaseVersionStatus.REVIEW);
        assertThat(rejected.isRevisionClosed()).isTrue();

        // Review record: SUBMIT then REJECT
        var records = reviewRecordRepository.findByTestCaseVersionIdOrderByCreatedAtAscIdAsc(versionId);
        assertThat(records).extracting("action").containsExactly(ReviewRecordAction.SUBMIT, ReviewRecordAction.REJECT);
        assertThat(records.get(1).getComment()).isEqualTo("non-compliant steps");

        // Cannot submit the rejected version again — after Reject the version stays
        // REVIEW (no DRAFT exists), so submitReview finds no Draft to submit.
        assertThatThrownBy(() -> lifecycleService.submitReview(
                masterId, new LifecycleActionRequest("resubmit"), coordinatorPrincipal))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DRAFT_REQUIRED);
    }

    // -------------------------------------------------------------------------
    // Return — REVIEW → DRAFT, revision reopened
    // -------------------------------------------------------------------------

    @Test
    void returnReviewMovesBackToDraftAndReopensRevision() {
        TestCaseDetailResponse created = createDraft("LC-006", "Return Flow");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.returnReview(masterId, versionId, new LifecycleActionRequest("fix the steps"), adminPrincipal);

        TestCaseVersionEntity returned = versionRepository.findById(versionId).orElseThrow();
        assertThat(returned.getStatus()).isEqualTo(TestCaseVersionStatus.DRAFT);
        assertThat(returned.isRevisionClosed()).isFalse();

        // Can submit again after return
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("resubmit"), coordinatorPrincipal);
        assertThat(versionStatus(masterId, versionId)).isEqualTo(TestCaseVersionStatus.REVIEW);

        // Records: SUBMIT, RETURN, SUBMIT
        var records = reviewRecordRepository.findByTestCaseVersionIdOrderByCreatedAtAscIdAsc(versionId);
        assertThat(records).extracting("action").containsExactly(
                ReviewRecordAction.SUBMIT, ReviewRecordAction.RETURN, ReviewRecordAction.SUBMIT);
    }

    @Test
    void returnReviewRequiresComment() {
        TestCaseDetailResponse created = createDraft("LC-007", "Return No Comment");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        assertThatThrownBy(() -> lifecycleService.returnReview(
                masterId, versionId, new LifecycleActionRequest("   "), adminPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_REVIEW_COMMENT_REQUIRED);
    }

    // -------------------------------------------------------------------------
    // Deprecate — only PUBLISHED can be deprecated
    // -------------------------------------------------------------------------

    @Test
    void deprecateFailsOnNonPublished() {
        TestCaseDetailResponse created = createDraft("LC-008", "Deprecate Non-Published");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        // Still DRAFT
        assertThatThrownBy(() -> lifecycleService.deprecate(
                masterId, versionId, new LifecycleActionRequest(null), adminPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_TRANSITION_INVALID);
    }

    // -------------------------------------------------------------------------
    // Create Revision — source must be PUBLISHED
    // -------------------------------------------------------------------------

    @Test
    void createRevisionFailsWhenNoPublishedVersionExists() {
        TestCaseDetailResponse created = createDraft("LC-009", "No Published Source");
        UUID masterId = created.id();

        // Only a DRAFT exists — cannot revise
        assertThatThrownBy(() -> lifecycleService.createRevision(
                masterId, new CreateRevisionRequest(null, null), coordinatorPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_REVISION_SOURCE_INVALID);
    }

    // -------------------------------------------------------------------------
    // Permissions — coordinator can submit but not publish/reject/deprecate
    // -------------------------------------------------------------------------

    @Test
    void coordinatorCannotPublishRejectOrDeprecate() {
        TestCaseDetailResponse created = createDraft("LC-010", "Permission Boundaries");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);

        // Coordinator lacks test_case:publish — but the resource-level gate fires first:
        // accessPolicy.isAdmin returns false, and the REVIEW version is not visible
        // to a non-admin, so requireVisibleVersion throws NOT_FOUND.
        assertThatThrownBy(() -> lifecycleService.publish(
                masterId, versionId, new LifecycleActionRequest("publish"), coordinatorPrincipal))
                .isNotNull();
        assertThatThrownBy(() -> lifecycleService.reject(
                masterId, versionId, new LifecycleActionRequest("reject"), coordinatorPrincipal))
                .isNotNull();
        assertThatThrownBy(() -> lifecycleService.deprecate(
                masterId, versionId, new LifecycleActionRequest("deprecate"), coordinatorPrincipal))
                .isNotNull();
    }

    @Test
    void nonOwnerCannotSubmitOthersDraft() {
        TestCaseDetailResponse created = createDraft("LC-011", "Non-Owner Submit");
        UUID masterId = created.id();

        // Create a second coordinator who does NOT own the draft
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity other = userRepository.save(new UserEntity("it_other_" + suffix, "Other Coordinator", "hash"));
        UserPrincipal otherPrincipal = new UserPrincipal(other.getId(), other.getUsername(), "hash",
                other.getDisplayName(), true, false, Set.of("TEST_COORDINATOR"),
                Set.of("test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review"));

        // Other coordinator cannot submit a draft they don't own — the independent
        // Submit resource gate rejects it.
        assertThatThrownBy(() -> lifecycleService.submitReview(
                masterId, new LifecycleActionRequest("submit"), otherPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN);
    }

    // -------------------------------------------------------------------------
    // AllowedActions — computed server-side, drives the UI
    // -------------------------------------------------------------------------

    @Test
    void allowedActionsReflectLifecycleState() {
        TestCaseDetailResponse created = createDraft("LC-012", "AllowedActions");
        UUID masterId = created.id();

        // DRAFT state: coordinator can edit/submit, cannot publish/reject
        TestCaseDetailResponse draftDetail = queryService.detail(masterId, coordinatorPrincipal);
        assertThat(draftDetail.allowedActions().editDraft()).isTrue();
        assertThat(draftDetail.allowedActions().submitReview()).isTrue();
        assertThat(draftDetail.allowedActions().publish()).isFalse();
        assertThat(draftDetail.allowedActions().reject()).isFalse();

        // After submit: REVIEW state — coordinator loses edit/submit, admin gains publish/return/reject
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        TestCaseDetailResponse adminReviewDetail = queryService.detail(masterId, adminPrincipal);
        assertThat(adminReviewDetail.allowedActions().publish()).isTrue();
        assertThat(adminReviewDetail.allowedActions().returnReview()).isTrue();
        assertThat(adminReviewDetail.allowedActions().reject()).isTrue();

        // After publish: PUBLISHED state — admin can deprecate and create revision
        UUID versionId = adminReviewDetail.visibleVersion().id();
        lifecycleService.publish(masterId, versionId, new LifecycleActionRequest("publish"), adminPrincipal);
        TestCaseDetailResponse publishedDetail = queryService.detail(masterId, adminPrincipal);
        assertThat(publishedDetail.allowedActions().deprecate()).isTrue();
        assertThat(publishedDetail.allowedActions().createRevision()).isTrue();
        assertThat(publishedDetail.allowedActions().publish()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Review records — append-only, latestReviewAction drives UI label
    // -------------------------------------------------------------------------

    @Test
    void latestReviewActionReflectsMostRecentRecord() {
        TestCaseDetailResponse created = createDraft("LC-013", "Latest Action");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        TestCaseDetailResponse reviewDetail = queryService.detail(masterId, adminPrincipal);
        assertThat(reviewDetail.visibleVersion().latestReviewAction()).isEqualTo(ReviewRecordAction.SUBMIT);

        lifecycleService.reject(masterId, versionId, new LifecycleActionRequest("rejected"), adminPrincipal);
        TestCaseDetailResponse rejectedDetail = queryService.detail(masterId, adminPrincipal);
        // latestReviewAction is REJECT — frontend derives the "Rejected" UI label from this
        assertThat(rejectedDetail.visibleVersion().latestReviewAction()).isEqualTo(ReviewRecordAction.REJECT);
        // But status is still REVIEW (R2)
        assertThat(rejectedDetail.visibleVersion().status()).isEqualTo("REVIEW");
        assertThat(rejectedDetail.visibleVersion().revisionClosed()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Revision contributors — add / remove / list
    // -------------------------------------------------------------------------

    @Test
    void contributorLifecycle() {
        TestCaseDetailResponse created = createDraft("LC-014", "Contributors");
        UUID masterId = created.id();

        // Initially no contributors
        assertThat(lifecycleService.listContributors(masterId, coordinatorPrincipal)).isEmpty();

        // Add a contributor
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity contributor = userRepository.save(new UserEntity("it_contrib_" + suffix, "Contributor", "hash"));
        var afterAdd = lifecycleService.addContributor(
                masterId, new com.company.casehub.testcase.dto.AddContributorRequest(contributor.getId()), coordinatorPrincipal);
        assertThat(afterAdd).hasSize(1);
        assertThat(afterAdd.get(0).userId()).isEqualTo(contributor.getId());

        // Cannot add duplicate
        assertThatThrownBy(() -> lifecycleService.addContributor(
                masterId, new com.company.casehub.testcase.dto.AddContributorRequest(contributor.getId()), coordinatorPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_CONTRIBUTOR_INVALID);

        // Cannot add owner as contributor
        assertThatThrownBy(() -> lifecycleService.addContributor(
                masterId, new com.company.casehub.testcase.dto.AddContributorRequest(coordinator.getId()), coordinatorPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_CONTRIBUTOR_INVALID);

        // Remove contributor
        var afterRemove = lifecycleService.removeContributor(masterId, contributor.getId(), coordinatorPrincipal);
        assertThat(afterRemove).isEmpty();
    }

    @Test
    void contributorCanEditDraftTheyDoNotOwn() {
        TestCaseDetailResponse created = createDraft("LC-015", "Contributor Edit");
        UUID masterId = created.id();

        // Add a contributor
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity contributor = userRepository.save(new UserEntity("it_contrib2_" + suffix, "Contributor", "hash"));
        lifecycleService.addContributor(
                masterId, new com.company.casehub.testcase.dto.AddContributorRequest(contributor.getId()), coordinatorPrincipal);

        // Contributor can edit the draft they don't own, but contributor membership
        // does not grant Submit Review.
        UserPrincipal contributorPrincipal = new UserPrincipal(contributor.getId(), contributor.getUsername(), "hash",
                contributor.getDisplayName(), true, false, Set.of("TEST_COORDINATOR"),
                Set.of("test_case:read", "test_case:draft_edit", "test_case:submit_review"));

        TestCaseDetailResponse detail = queryService.detail(masterId, contributorPrincipal);
        assertThat(detail.allowedActions().editDraft()).isTrue();
        assertThat(detail.allowedActions().submitReview()).isFalse();

        assertThatThrownBy(() -> lifecycleService.submitReview(
                masterId, new LifecycleActionRequest("contributor submit"), contributorPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN);
        assertThat(versionStatus(masterId, created.visibleVersion().id())).isEqualTo(TestCaseVersionStatus.DRAFT);
    }

    // -------------------------------------------------------------------------
    // HIGH-01 — Deprecated / Historical Version Visibility (all logged-in users)
    // -------------------------------------------------------------------------

    @Test
    void normalUserCanReadOthersDeprecatedVersion() {
        TestCaseDetailResponse created = createDraft("H1-001", "Deprecated Visibility");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, versionId, new LifecycleActionRequest("publish"), adminPrincipal);
        lifecycleService.deprecate(masterId, versionId, new LifecycleActionRequest("obsolete"), adminPrincipal);

        UserPrincipal viewer = viewerPrincipal();
        // Detail surface shows the DEPRECATED version to an unrelated logged-in user.
        TestCaseDetailResponse detail = queryService.detail(masterId, viewer);
        assertThat(detail.visibleVersion().status()).isEqualTo("DEPRECATED");
        // The explicit version endpoint is also visible.
        assertThat(queryService.version(masterId, versionId, viewer).status()).isEqualTo("DEPRECATED");
        // And it appears in the library list.
        var listed = queryService.list(null, null, null, null, null, null, 0, 20, "updatedAt,desc", viewer);
        assertThat(listed.content()).extracting("id").contains(masterId);
    }

    @Test
    void historicalPublishedNotMasqueradedAsCurrent() {
        TestCaseDetailResponse created = createDraft("H1-002", "Current Switch Visibility");
        UUID masterId = created.id();
        UUID v10 = created.visibleVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, v10, new LifecycleActionRequest("publish"), adminPrincipal);

        // v1.1 published — v1.0 loses current flag.
        lifecycleService.createRevision(masterId, new CreateRevisionRequest(null, "rev"), coordinatorPrincipal);
        UUID v11 = findLatestDraft(masterId).getId();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, v11, new LifecycleActionRequest("publish"), adminPrincipal);

        UserPrincipal viewer = viewerPrincipal();
        // A normal user sees the CURRENT published (v1.1), not the historical v1.0.
        TestCaseDetailResponse detail = queryService.detail(masterId, viewer);
        assertThat(detail.visibleVersion().status()).isEqualTo("PUBLISHED");
        assertThat(detail.visibleVersion().id()).isEqualTo(v11);
        assertThat(detail.visibleVersion().isCurrentVersion()).isTrue();

        // Now deprecate the current version. The newest DEPRECATED v1.1 is the
        // primary/default version; historical v1.0 remains history only, and no
        // version may be reported as current.
        lifecycleService.deprecate(masterId, v11, new LifecycleActionRequest("obsolete"), adminPrincipal);
        TestCaseDetailResponse afterDeprecate = queryService.detail(masterId, viewer);
        assertThat(afterDeprecate.visibleVersion().status()).isEqualTo("DEPRECATED");
        assertThat(afterDeprecate.visibleVersion().id()).isEqualTo(v11);
        assertThat(afterDeprecate.visibleVersion().isCurrentVersion()).isFalse();
        assertThat(afterDeprecate.currentVersion()).isNull();
        assertThat(afterDeprecate.versions()).extracting("id").contains(v10, v11);

        var listed = queryService.list(null, null, null, null, null, null, 0, 20, "updatedAt,desc", viewer);
        assertThat(listed.content()).filteredOn(summary -> summary.id().equals(masterId))
                .singleElement().extracting("status").isEqualTo("DEPRECATED");
    }

    @Test
    void deprecatedVersionVisibleInVersionHistory() {
        TestCaseDetailResponse created = createDraft("H1-003", "Deprecated History");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, versionId, new LifecycleActionRequest("publish"), adminPrincipal);
        lifecycleService.deprecate(masterId, versionId, new LifecycleActionRequest("obsolete"), adminPrincipal);

        List<VersionSummaryResponse> history = queryService.versions(masterId, viewerPrincipal());
        assertThat(history).extracting("id").contains(versionId);
        assertThat(history).extracting("status").contains("DEPRECATED");
    }

    @Test
    void listDetailVersionVisibilityConsistentForDeprecated() {
        TestCaseDetailResponse created = createDraft("H1-004", "Consistency");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, versionId, new LifecycleActionRequest("publish"), adminPrincipal);
        lifecycleService.deprecate(masterId, versionId, new LifecycleActionRequest("obsolete"), adminPrincipal);

        UserPrincipal viewer = viewerPrincipal();
        // The three read APIs must agree on visibility for the same user.
        assertThat(queryService.detail(masterId, viewer).visibleVersion().status()).isEqualTo("DEPRECATED");
        assertThat(queryService.version(masterId, versionId, viewer).status()).isEqualTo("DEPRECATED");
        assertThat(queryService.list(null, null, null, null, null, null, 0, 20, "updatedAt,desc", viewer).content())
                .extracting("id").contains(masterId);
    }

    // -------------------------------------------------------------------------
    // HIGH-03 — Reject then Create Revision (never-published master escape hatch)
    // -------------------------------------------------------------------------

    private UUID[] rejectInitialDraft(String code, String name) {
        TestCaseDetailResponse created = createDraft(code, name);
        UUID masterId = created.id();
        UUID firstVersionId = created.visibleVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.reject(masterId, firstVersionId, new LifecycleActionRequest("non-compliant steps"), adminPrincipal);
        return new UUID[]{masterId, firstVersionId};
    }

    @Test
    void initialRejectedRevisionCanCreateNewDraft() {
        UUID[] ids = rejectInitialDraft("H3-001", "Rejected Revision");
        UUID masterId = ids[0];

        // No PUBLISHED exists; createRevision must fall back to the rejected REVIEW version.
        TestCaseDetailResponse afterRevision = lifecycleService.createRevision(
                masterId, new CreateRevisionRequest(null, "fix after reject"), coordinatorPrincipal);
        UUID newDraftId = afterRevision.draftVersion() != null
                ? afterRevision.draftVersion().id() : findLatestDraft(masterId).getId();
        TestCaseVersionEntity newDraft = versionRepository.findById(newDraftId).orElseThrow();
        assertThat(newDraft.getStatus()).isEqualTo(TestCaseVersionStatus.DRAFT);
        assertThat(newDraft.isRevisionClosed()).isFalse();
    }

    @Test
    void rejectedVersionRemainsClosed() {
        UUID[] ids = rejectInitialDraft("H3-002", "Rejected Closed");
        UUID firstVersionId = ids[1];
        lifecycleService.createRevision(ids[0], new CreateRevisionRequest(null, "rev"), coordinatorPrincipal);
        TestCaseVersionEntity rejected = versionRepository.findById(firstVersionId).orElseThrow();
        assertThat(rejected.isRevisionClosed()).isTrue();
        assertThat(rejected.getStatus()).isEqualTo(TestCaseVersionStatus.REVIEW);
    }

    @Test
    void newRevisionHasNewId() {
        UUID[] ids = rejectInitialDraft("H3-003", "New Id");
        UUID firstVersionId = ids[1];
        TestCaseDetailResponse afterRevision = lifecycleService.createRevision(
                ids[0], new CreateRevisionRequest(null, "rev"), coordinatorPrincipal);
        UUID newDraftId = afterRevision.draftVersion() != null
                ? afterRevision.draftVersion().id() : findLatestDraft(ids[0]).getId();
        assertThat(newDraftId).isNotEqualTo(firstVersionId);
    }

    @Test
    void newRevisionUsesNextVersionNumber() {
        UUID[] ids = rejectInitialDraft("H3-004", "Next Number");
        TestCaseDetailResponse afterRevision = lifecycleService.createRevision(
                ids[0], new CreateRevisionRequest(null, "rev"), coordinatorPrincipal);
        UUID newDraftId = afterRevision.draftVersion() != null
                ? afterRevision.draftVersion().id() : findLatestDraft(ids[0]).getId();
        TestCaseVersionEntity newDraft = versionRepository.findById(newDraftId).orElseThrow();
        // Source was v1.0 → new revision is v1.1.
        assertThat(newDraft.getVersionMajor()).isEqualTo(1);
        assertThat(newDraft.getVersionMinor()).isEqualTo(1);
    }

    @Test
    void newRevisionCopiesContent() {
        UUID[] ids = rejectInitialDraft("H3-005", "Copy Content");
        UUID firstVersionId = ids[1];
        TestCaseVersionEntity original = versionRepository.findById(firstVersionId).orElseThrow();
        TestCaseDetailResponse afterRevision = lifecycleService.createRevision(
                ids[0], new CreateRevisionRequest(null, "rev"), coordinatorPrincipal);
        UUID newDraftId = afterRevision.draftVersion() != null
                ? afterRevision.draftVersion().id() : findLatestDraft(ids[0]).getId();
        TestCaseVersionEntity newDraft = versionRepository.findById(newDraftId).orElseThrow();
        assertThat(newDraft.getCaseName()).isEqualTo(original.getCaseName());
        assertThat(newDraft.getBasedOnVersion().getId()).isEqualTo(firstVersionId);
        assertThat(stepRepository.findByTestCaseVersionId(newDraftId)).hasSize(
                stepRepository.findByTestCaseVersionId(firstVersionId).size());
    }

    @Test
    void oldRejectedRevisionRemainsReviewAndClosed() {
        UUID[] ids = rejectInitialDraft("H3-006", "Stays Rejected");
        UUID firstVersionId = ids[1];
        lifecycleService.createRevision(ids[0], new CreateRevisionRequest(null, "rev"), coordinatorPrincipal);
        TestCaseVersionEntity rejected = versionRepository.findById(firstVersionId).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(TestCaseVersionStatus.REVIEW);
        assertThat(rejected.isRevisionClosed()).isTrue();
        // After createRevision, visibleVersion() points at the new DRAFT, so the rejected version's
        // REJECT must be asserted against its own review records rather than the detail surface.
        var rejectedRecords = reviewRecordRepository.findByTestCaseVersionIdOrderByCreatedAtAscIdAsc(firstVersionId);
        assertThat(rejectedRecords).extracting("action")
                .containsExactly(ReviewRecordAction.SUBMIT, ReviewRecordAction.REJECT);
    }

    // -------------------------------------------------------------------------
    // MEDIUM-02 — Deprecate serialised under the Master PESSIMISTIC_WRITE lock
    // -------------------------------------------------------------------------

    @Test
    void lockedLifecycleOperationsSerialiseUnderMasterLock() {
        // Publish, Deprecate and Create-Revision all take the Master row lock; running the
        // full sequence confirms the locked deprecate() path functions end-to-end.
        TestCaseDetailResponse created = createDraft("M2-001", "Master Lock");
        UUID masterId = created.id();
        UUID v10 = created.visibleVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, v10, new LifecycleActionRequest("publish"), adminPrincipal);

        lifecycleService.createRevision(masterId, new CreateRevisionRequest(null, "rev"), coordinatorPrincipal);
        UUID v11 = findLatestDraft(masterId).getId();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, v11, new LifecycleActionRequest("publish"), adminPrincipal);

        // Deprecate now uses masterRepository.findByIdWithLock — must succeed and flip status.
        lifecycleService.deprecate(masterId, v11, new LifecycleActionRequest("obsolete"), adminPrincipal);
        assertThat(versionStatus(masterId, v11)).isEqualTo(TestCaseVersionStatus.DEPRECATED);
        // The historical v1.0 remains PUBLISHED and untouched.
        assertThat(versionStatus(masterId, v10)).isEqualTo(TestCaseVersionStatus.PUBLISHED);
        // Master row stays consistent after the lock is released.
        assertThat(masterRepository.findById(masterId)).isPresent();
    }

    // -------------------------------------------------------------------------
    // HIGH-02 — Real TESTER Revision Contributor edit permissions (no global draft_edit)
    // -------------------------------------------------------------------------

    private UserEntity saveTester(String username) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(new UserEntity("it_" + username + "_" + suffix, username, "hash"));
    }

    @Test
    void testerContributorCanEditAssignedDraft() {
        TestCaseDetailResponse created = createDraft("H2-001", "Contributor Edit");
        UUID masterId = created.id();

        UserEntity tester = saveTester("contrib_edit");
        lifecycleService.addContributor(masterId,
                new com.company.casehub.testcase.dto.AddContributorRequest(tester.getId()), coordinatorPrincipal);

        UserPrincipal testerPrincipal = new UserPrincipal(tester.getId(), tester.getUsername(), "hash",
                tester.getDisplayName(), true, false, Set.of("TESTER"),
                Set.of("test_case:read", "test_case:submit_review"));

        TestCaseDetailResponse updated = draftService.updateDraft(masterId,
                new UpdateDraftRequest("Edited By Tester", "purpose", "pre", SelectionMode.SINGLE, false,
                        null, null, null, List.of(new StepRequest("S1", "tester step")), List.of(), List.of(), List.of()),
                testerPrincipal);
        assertThat(updated.visibleVersion().caseName()).isEqualTo("Edited By Tester");
        assertThat(updated.allowedActions().editDraft()).isTrue();
        assertThat(updated.allowedActions().submitReview()).isFalse();
    }

    @Test
    void testerContributorCannotSubmitAssignedDraft() {
        TestCaseDetailResponse created = createDraft("H4-001", "Contributor Submit Denied");
        UUID masterId = created.id();

        UserEntity tester = saveTester("contrib_submit");
        lifecycleService.addContributor(masterId,
                new com.company.casehub.testcase.dto.AddContributorRequest(tester.getId()), coordinatorPrincipal);
        UserPrincipal testerPrincipal = new UserPrincipal(tester.getId(), tester.getUsername(), "hash",
                tester.getDisplayName(), true, false, Set.of("TESTER"),
                Set.of("test_case:read", "test_case:submit_review"));

        assertThatThrownBy(() -> lifecycleService.submitReview(masterId,
                new LifecycleActionRequest("tester submit"), testerPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN);
    }

    @Test
    void testerContributorCannotEditOtherDraft() {
        TestCaseDetailResponse created = createDraft("H2-002", "Other Draft");
        UUID masterId = created.id();

        UserEntity tester = saveTester("contrib_other");
        UserPrincipal testerPrincipal = new UserPrincipal(tester.getId(), tester.getUsername(), "hash",
                tester.getDisplayName(), true, false, Set.of("TESTER"), Set.of("test_case:read", "test_case:submit_review"));

        // Tester is NOT a contributor on this master → forbidden.
        assertThatThrownBy(() -> draftService.updateDraft(masterId,
                new UpdateDraftRequest("X", "p", "p", SelectionMode.SINGLE, false, null, null, null,
                        List.of(new StepRequest("S1", "c")), List.of(), List.of(), List.of()), testerPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DRAFT_EDIT_FORBIDDEN);
    }

    @Test
    void testerWithoutContributorCannotEditDraft() {
        TestCaseDetailResponse created = createDraft("H2-003", "No Contributor");
        UUID masterId = created.id();

        UserEntity tester = saveTester("no_contrib");
        UserPrincipal testerPrincipal = new UserPrincipal(tester.getId(), tester.getUsername(), "hash",
                tester.getDisplayName(), true, false, Set.of("TESTER"), Set.of("test_case:read", "test_case:submit_review"));

        assertThatThrownBy(() -> draftService.updateDraft(masterId,
                new UpdateDraftRequest("X", "p", "p", SelectionMode.SINGLE, false, null, null, null,
                        List.of(new StepRequest("S1", "c")), List.of(), List.of(), List.of()), testerPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DRAFT_EDIT_FORBIDDEN);
    }

    @Test
    void testerContributorCannotEditPublished() {
        TestCaseDetailResponse created = createDraft("H2-004", "Published Edit");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();

        // Add the tester as a contributor while the version is still an open DRAFT.
        UserEntity tester = saveTester("contrib_pub");
        lifecycleService.addContributor(masterId,
                new com.company.casehub.testcase.dto.AddContributorRequest(tester.getId()), coordinatorPrincipal);
        UserPrincipal testerPrincipal = new UserPrincipal(tester.getId(), tester.getUsername(), "hash",
                tester.getDisplayName(), true, false, Set.of("TESTER"), Set.of("test_case:read", "test_case:submit_review"));

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, versionId, new LifecycleActionRequest("publish"), adminPrincipal);

        // After publish there is no open DRAFT → editing is impossible (Published Immutable), even
        // for a contributor who could edit it before publication.
        assertThatThrownBy(() -> draftService.updateDraft(masterId,
                new UpdateDraftRequest("X", "p", "p", SelectionMode.SINGLE, false, null, null, null,
                        List.of(new StepRequest("S1", "c")), List.of(), List.of(), List.of()), testerPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DRAFT_REQUIRED);
        // And the published detail must NOT surface an edit action for the tester.
        assertThat(queryService.detail(masterId, testerPrincipal).allowedActions().editDraft()).isFalse();
    }

    @Test
    void testerContributorCannotManageContributors() {
        TestCaseDetailResponse created = createDraft("H2-005", "Manage Contributors");
        UUID masterId = created.id();

        UserEntity tester = saveTester("contrib_manage");
        lifecycleService.addContributor(masterId,
                new com.company.casehub.testcase.dto.AddContributorRequest(tester.getId()), coordinatorPrincipal);
        UserEntity other = saveTester("contrib_manage_target");
        UserPrincipal testerPrincipal = new UserPrincipal(tester.getId(), tester.getUsername(), "hash",
                tester.getDisplayName(), true, false, Set.of("TESTER"), Set.of("test_case:read", "test_case:submit_review"));

        // A contributor is NOT an owner/admin → cannot manage contributors.
        assertThatThrownBy(() -> lifecycleService.addContributor(masterId,
                new com.company.casehub.testcase.dto.AddContributorRequest(other.getId()), testerPrincipal))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN);
    }

    // -------------------------------------------------------------------------
    // HIGH-05 — Unrelated Coordinator cannot branch a revision by omitting the
    // source, and is denied an explicit rejected source
    // -------------------------------------------------------------------------

    @Test
    void unrelatedCoordinatorCanCreateRevisionWithOmittedCurrentPublishedSource() {
        TestCaseDetailResponse created = createDraft("H5-001", "Omitted Source");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.publish(masterId, versionId, new LifecycleActionRequest("publish"), adminPrincipal);

        UserPrincipal unrelated = unrelatedCoordinator();
        // No explicit sourceVersionId — current PUBLISHED is public, so omitted and
        // explicit current-PUBLISHED forms use the same source semantics.
        TestCaseDetailResponse revised = lifecycleService.createRevision(
                masterId, new CreateRevisionRequest(null, "unrelated branch"), unrelated);
        assertThat(revised.draftVersion()).isNotNull();
        assertThat(revised.draftVersion().basedOnVersionId()).isEqualTo(versionId);
    }

    @Test
    void unrelatedCoordinatorCannotCreateRevisionFromRejectedSource() {
        TestCaseDetailResponse created = createDraft("H5-002", "Rejected Source");
        UUID masterId = created.id();
        UUID versionId = created.visibleVersion().id();
        lifecycleService.submitReview(masterId, new LifecycleActionRequest("submit"), coordinatorPrincipal);
        lifecycleService.reject(masterId, versionId, new LifecycleActionRequest("non-compliant"), adminPrincipal);

        UserPrincipal unrelated = unrelatedCoordinator();
        // Explicit rejected (REVIEW + revision_closed) source must be denied for an
        // unrelated Coordinator (HIGH-05: explicit rejected source -> DENY). The version
        // is not visible to them, so requireVisibleVersion throws NOT_FOUND.
        assertThatThrownBy(() -> lifecycleService.createRevision(
                masterId, new CreateRevisionRequest(versionId, "unrelated branch"), unrelated))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private UserPrincipal unrelatedCoordinator() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity other = userRepository.save(new UserEntity("it_unrelated_" + suffix, "Unrelated Coordinator", "hash"));
        return new UserPrincipal(other.getId(), other.getUsername(), "hash", other.getDisplayName(), true, false,
                Set.of("TEST_COORDINATOR"),
                Set.of("test_case:read", "test_case:draft_create", "test_case:draft_edit", "test_case:submit_review"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TestCaseDetailResponse createDraft(String code, String name) {
        return draftService.createDraft(new CreateDraftRequest(code, category.getId(), name, "purpose",
                "preconditions", SelectionMode.SINGLE, false, null, "notes", null,
                List.of(new StepRequest("Step 1", "Do something"), new StepRequest("Step 2", "Do more")),
                List.of(), List.of(), List.of()), coordinatorPrincipal);
    }

    private TestCaseVersionStatus versionStatus(UUID masterId, UUID versionId) {
        return versionRepository.findById(versionId)
                .filter(v -> v.getMasterTestCase().getId().equals(masterId))
                .map(TestCaseVersionEntity::getStatus)
                .orElseThrow();
    }

    private TestCaseVersionEntity findLatestDraft(UUID masterId) {
        return versionRepository.findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(masterId).stream()
                .filter(v -> v.getStatus() == TestCaseVersionStatus.DRAFT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No DRAFT found for master " + masterId));
    }

    /** A regular logged-in user (TESTER) with read only — no ownership of the test case. */
    private UserPrincipal viewerPrincipal() {
        UserEntity viewer = saveTester("viewer");
        return new UserPrincipal(viewer.getId(), viewer.getUsername(), "hash", viewer.getDisplayName(), true, false,
                Set.of("TESTER"), Set.of("test_case:read"));
    }
}
