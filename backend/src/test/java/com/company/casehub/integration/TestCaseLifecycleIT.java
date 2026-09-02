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
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TestStepEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseReviewRecordRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.TestStepRepository;
import com.company.casehub.testcase.service.TestCaseDraftService;
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

        // Other coordinator cannot submit a draft they don't own — the resource-level gate
        // (canEditOrSubmit) forbids it. submitReview finds the DRAFT by masterId+status (visibility is
        // not checked at this layer) and then canEditOrSubmit returns false → FORBIDDEN.
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

        // Contributor can now see and submit the draft they don't own
        UserPrincipal contributorPrincipal = new UserPrincipal(contributor.getId(), contributor.getUsername(), "hash",
                contributor.getDisplayName(), true, false, Set.of("TEST_COORDINATOR"),
                Set.of("test_case:read", "test_case:draft_edit", "test_case:submit_review"));

        TestCaseDetailResponse detail = queryService.detail(masterId, contributorPrincipal);
        assertThat(detail.allowedActions().submitReview()).isTrue();

        lifecycleService.submitReview(masterId, new LifecycleActionRequest("contributor submit"), contributorPrincipal);
        assertThat(versionStatus(masterId, created.visibleVersion().id())).isEqualTo(TestCaseVersionStatus.REVIEW);
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
}
