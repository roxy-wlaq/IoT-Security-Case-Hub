package com.company.casehub.testcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ValidationException;
import com.company.casehub.testcase.dto.AddContributorRequest;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.RevisionContributorEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseReviewRecordEntity;
import com.company.casehub.testcase.entity.TestCaseToolEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TestStepEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.RevisionContributorRepository;
import com.company.casehub.testcase.repository.TestCaseReviewRecordRepository;
import com.company.casehub.testcase.repository.TestCaseStandardMappingRepository;
import com.company.casehub.testcase.repository.TestCaseToolRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.TestStepRepository;
import com.company.casehub.tool.entity.ToolEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mockito unit tests for the Test Case Lifecycle Service (Phase 7). Covers the
 * frozen hard rules: lifecycle transitions, Published Immutable (no mutation of
 * PUBLISHED content), Reject keeps REVIEW + revision_closed, single current
 * published switch, version-number server control, and review-record append-only.
 */
@ExtendWith(MockitoExtension.class)
class TestCaseLifecycleServiceTest {

    @Mock private MasterTestCaseRepository masterRepository;
    @Mock private TestCaseVersionRepository versionRepository;
    @Mock private TestCaseReviewRecordRepository reviewRecordRepository;
    @Mock private RevisionContributorRepository contributorRepository;
    @Mock private TestStepRepository stepRepository;
    @Mock private TestCaseToolRepository toolRepository;
    @Mock private TestCaseStandardMappingRepository mappingRepository;
    @Mock private UserRepository userRepository;
    @Mock private TestCaseAccessPolicy accessPolicy;
    @Mock private TestCaseQueryService queryService;

    @InjectMocks private TestCaseLifecycleService service;

    // -------------------------------------------------------------------------
    // Submit Review
    // -------------------------------------------------------------------------

    @Test
    void submitReviewMovesDraftToReviewAndAppendsRecord() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        MasterTestCaseEntity master = master(masterId, owner);
        TestCaseVersionEntity draft = draft(owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(versionRepository.findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(
                masterId, TestCaseVersionStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(accessPolicy.canEditOrSubmit(eq(draft), any())).thenReturn(true);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.submitReview(masterId, new LifecycleActionRequest("looks good"), principal(owner.getId()));

        assertThat(draft.getStatus()).isEqualTo(TestCaseVersionStatus.REVIEW);
        ArgumentCaptor<TestCaseReviewRecordEntity> recordCaptor = ArgumentCaptor.forClass(TestCaseReviewRecordEntity.class);
        verify(reviewRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAction()).isEqualTo(ReviewRecordAction.SUBMIT);
        assertThat(recordCaptor.getValue().getComment()).isEqualTo("looks good");
    }

    @Test
    void submitReviewFailsWhenNoDraftExists() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        MasterTestCaseEntity master = master(masterId, owner);
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(versionRepository.findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(
                masterId, TestCaseVersionStatus.DRAFT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitReview(masterId, new LifecycleActionRequest(null), principal(owner.getId())))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DRAFT_REQUIRED);
        verify(reviewRecordRepository, never()).save(any());
    }

    @Test
    void submitReviewFailsWhenRevisionClosed() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        draft.setRevisionClosed(true);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(versionRepository.findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(
                masterId, TestCaseVersionStatus.DRAFT)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.submitReview(masterId, new LifecycleActionRequest(null), principal(owner.getId())))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_REVISION_CLOSED);
    }

    @Test
    void submitReviewFailsForNonOwnerNonAdminNonContributor() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(versionRepository.findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(
                masterId, TestCaseVersionStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(accessPolicy.canEditOrSubmit(eq(draft), any())).thenReturn(false);

        assertThatThrownBy(() -> service.submitReview(masterId, new LifecycleActionRequest(null),
                principal(UUID.randomUUID())))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN);
    }

    @Test
    void submitReviewFailsWhenDraftHasNoSteps() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        draft.setSteps(new ArrayList<>());
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(versionRepository.findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(
                masterId, TestCaseVersionStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(accessPolicy.canEditOrSubmit(eq(draft), any())).thenReturn(true);

        assertThatThrownBy(() -> service.submitReview(masterId, new LifecycleActionRequest(null), principal(owner.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DRAFT_INCOMPLETE);
    }

    // -------------------------------------------------------------------------
    // Publish
    // -------------------------------------------------------------------------

    @Test
    void publishMovesReviewToPublishedAndSwitchesCurrent() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity priorPublished = version(owner, TestCaseVersionStatus.PUBLISHED, 1, 0);
        priorPublished.setCurrentVersion(true);
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW, 1, 1);
        review.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(priorPublished, review)));
        when(masterRepository.findByIdWithLock(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.publish(masterId, review.getId(), new LifecycleActionRequest("approved"), principal(owner.getId()));

        assertThat(review.getStatus()).isEqualTo(TestCaseVersionStatus.PUBLISHED);
        assertThat(review.isCurrentVersion()).isTrue();
        assertThat(priorPublished.isCurrentVersion()).isFalse();
        assertThat(review.isRevisionClosed()).isTrue();
        assertThat(review.getPublishedAt()).isNotNull();
        verifyRecord(ReviewRecordAction.PUBLISH);
    }

    @Test
    void publishFailsWhenVersionIsNotInReview() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findByIdWithLock(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);

        assertThatThrownBy(() -> service.publish(masterId, draft.getId(), new LifecycleActionRequest(null),
                principal(owner.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_TRANSITION_INVALID);
    }

    @Test
    void publishFailsWhenRevisionIsClosed() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW, 1, 1);
        review.setRevisionClosed(true);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(review)));
        when(masterRepository.findByIdWithLock(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);

        assertThatThrownBy(() -> service.publish(masterId, review.getId(), new LifecycleActionRequest(null),
                principal(owner.getId())))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_REVISION_CLOSED);
    }

    // -------------------------------------------------------------------------
    // Return
    // -------------------------------------------------------------------------

    @Test
    void returnReviewMovesReviewBackToDraftAndAppendsRecord() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW, 1, 1);
        review.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(review)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.returnReview(masterId, review.getId(), new LifecycleActionRequest("fix steps"), principal(owner.getId()));

        assertThat(review.getStatus()).isEqualTo(TestCaseVersionStatus.DRAFT);
        assertThat(review.isRevisionClosed()).isFalse();
        verifyRecord(ReviewRecordAction.RETURN);
    }

    @Test
    void returnReviewRequiresComment() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW, 1, 1);
        review.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(review)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);

        assertThatThrownBy(() -> service.returnReview(masterId, review.getId(), new LifecycleActionRequest("  "),
                principal(owner.getId())))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_REVIEW_COMMENT_REQUIRED);
        verify(reviewRecordRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Reject — R2: status stays REVIEW, revision_closed = true
    // -------------------------------------------------------------------------

    @Test
    void rejectKeepsReviewStatusButClosesRevision() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW, 1, 1);
        review.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(review)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.reject(masterId, review.getId(), new LifecycleActionRequest("non-compliant"), principal(owner.getId()));

        // R2 hard rule: status stays REVIEW — there is no REJECTED status.
        assertThat(review.getStatus()).isEqualTo(TestCaseVersionStatus.REVIEW);
        assertThat(review.isRevisionClosed()).isTrue();
        verifyRecord(ReviewRecordAction.REJECT);
    }

    @Test
    void rejectRequiresComment() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW, 1, 1);
        review.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(review)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);

        assertThatThrownBy(() -> service.reject(masterId, review.getId(), new LifecycleActionRequest(null),
                principal(owner.getId())))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_REVIEW_COMMENT_REQUIRED);
    }

    // -------------------------------------------------------------------------
    // Deprecate
    // -------------------------------------------------------------------------

    @Test
    void deprecateMovesPublishedToDeprecated() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity published = version(owner, TestCaseVersionStatus.PUBLISHED, 1, 0);
        published.setCurrentVersion(true);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(published)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.deprecate(masterId, published.getId(), new LifecycleActionRequest("obsolete"), principal(owner.getId()));

        assertThat(published.getStatus()).isEqualTo(TestCaseVersionStatus.DEPRECATED);
        assertThat(published.isCurrentVersion()).isFalse();
        assertThat(published.getDeprecatedAt()).isNotNull();
        assertThat(published.isRevisionClosed()).isTrue();
        verifyRecord(ReviewRecordAction.DEPRECATE);
    }

    @Test
    void deprecateFailsWhenVersionIsNotPublished() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);

        assertThatThrownBy(() -> service.deprecate(masterId, draft.getId(), new LifecycleActionRequest(null),
                principal(owner.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_LIFECYCLE_TRANSITION_INVALID);
    }

    // -------------------------------------------------------------------------
    // Create Revision — version number server-controlled, copies content
    // -------------------------------------------------------------------------

    @Test
    void createRevisionComputesNextMinorFromMaxOfSameMajor() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity published1_0 = version(owner, TestCaseVersionStatus.PUBLISHED, 1, 0);
        published1_0.setCurrentVersion(true);
        TestCaseVersionEntity published1_3 = version(owner, TestCaseVersionStatus.PUBLISHED, 1, 3);
        published1_3.setCurrentVersion(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(published1_0, published1_3)));
        when(masterRepository.findByIdWithLock(masterId)).thenReturn(Optional.of(master));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.createRevision(masterId, new CreateRevisionRequest(null, "minor fix"), principal(owner.getId()));

        ArgumentCaptor<TestCaseVersionEntity> versionCaptor = ArgumentCaptor.forClass(TestCaseVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());
        TestCaseVersionEntity revision = versionCaptor.getValue();
        assertThat(revision.getVersionMajor()).isEqualTo(1);
        // nextMinor = MAX(0, 3) + 1 = 4
        assertThat(revision.getVersionMinor()).isEqualTo(4);
        assertThat(revision.getStatus()).isEqualTo(TestCaseVersionStatus.DRAFT);
        assertThat(revision.isCurrentVersion()).isFalse();
        assertThat(revision.isRevisionClosed()).isFalse();
        assertThat(revision.getChangeReason()).isEqualTo("minor fix");
        assertThat(revision.getBasedOnVersion()).isEqualTo(published1_0);
    }

    @Test
    void createRevisionCopiesStepsAndTools() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity published = version(owner, TestCaseVersionStatus.PUBLISHED, 2, 0);
        published.setCurrentVersion(true);
        published.getSteps().clear();
        published.getSteps().add(step(published, 1, "Prepare", "power on"));
        published.getSteps().add(step(published, 2, "Execute", "run test"));
        ToolEntity tool = new ToolEntity();
        tool.setId(UUID.randomUUID());
        tool.setCode("T1");
        tool.setName("Tool 1");
        TestCaseToolEntity toolRelation = new TestCaseToolEntity();
        toolRelation.setTestCaseVersion(published);
        toolRelation.setTool(tool);
        toolRelation.setSortOrder(0);
        published.getTools().add(toolRelation);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(published)));
        when(masterRepository.findByIdWithLock(masterId)).thenReturn(Optional.of(master));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.createRevision(masterId, new CreateRevisionRequest(null, null), principal(owner.getId()));

        ArgumentCaptor<TestCaseVersionEntity> versionCaptor = ArgumentCaptor.forClass(TestCaseVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());
        TestCaseVersionEntity revision = versionCaptor.getValue();
        assertThat(revision.getSteps()).hasSize(2);
        assertThat(revision.getSteps()).extracting("title").containsExactly("Prepare", "Execute");
        assertThat(revision.getTools()).hasSize(1);
        assertThat(revision.getTools().get(0).getTool()).isEqualTo(tool);
    }

    @Test
    void createRevisionFailsWhenSourceIsNotPublished() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findByIdWithLock(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.isAdmin(any())).thenReturn(true);

        assertThatThrownBy(() -> service.createRevision(masterId, new CreateRevisionRequest(draft.getId(), null),
                principal(owner.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_REVISION_SOURCE_INVALID);
    }

    // -------------------------------------------------------------------------
    // Revision Contributors
    // -------------------------------------------------------------------------

    @Test
    void addContributorAppendsAndReturnsList() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        UserEntity contributor = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        draft.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.canManageContributors(eq(draft), any())).thenReturn(true);
        when(userRepository.findById(contributor.getId())).thenReturn(Optional.of(contributor));
        when(contributorRepository.existsByTestCaseVersionIdAndUserId(draft.getId(), contributor.getId())).thenReturn(false);
        when(contributorRepository.findByTestCaseVersionIdOrderByCreatedAtAsc(draft.getId())).thenReturn(List.of());
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        service.addContributor(masterId, new AddContributorRequest(contributor.getId()), principal(owner.getId()));

        verify(contributorRepository).save(any(RevisionContributorEntity.class));
    }

    @Test
    void addContributorRejectsOwnerAsContributor() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        draft.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.canManageContributors(eq(draft), any())).thenReturn(true);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.addContributor(masterId, new AddContributorRequest(owner.getId()),
                principal(owner.getId())))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_CONTRIBUTOR_INVALID);
        verify(contributorRepository, never()).save(any());
    }

    @Test
    void addContributorRejectsDuplicate() {
        UUID masterId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        UserEntity contributor = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        draft.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.canManageContributors(eq(draft), any())).thenReturn(true);
        when(userRepository.findById(contributor.getId())).thenReturn(Optional.of(contributor));
        when(contributorRepository.existsByTestCaseVersionIdAndUserId(draft.getId(), contributor.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.addContributor(masterId, new AddContributorRequest(contributor.getId()),
                principal(owner.getId())))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_CONTRIBUTOR_INVALID);
    }

    @Test
    void removeContributorDeletesAndReturnsRemaining() {
        UUID masterId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(UUID.randomUUID());
        TestCaseVersionEntity draft = draft(owner);
        draft.setRevisionClosed(false);
        MasterTestCaseEntity master = master(masterId, owner);
        master.setVersions(new ArrayList<>(List.of(draft)));
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(accessPolicy.canManageContributors(eq(draft), any())).thenReturn(true);
        when(contributorRepository.findByTestCaseVersionIdOrderByCreatedAtAsc(draft.getId())).thenReturn(List.of());

        service.removeContributor(masterId, contributorId, principal(owner.getId()));

        verify(contributorRepository).deleteByTestCaseVersionIdAndUserId(draft.getId(), contributorId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void verifyRecord(ReviewRecordAction action) {
        ArgumentCaptor<TestCaseReviewRecordEntity> captor = ArgumentCaptor.forClass(TestCaseReviewRecordEntity.class);
        verify(reviewRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(action);
    }

    private MasterTestCaseEntity master(UUID id, UserEntity owner) {
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setId(id);
        master.setCreatedBy(owner);
        return master;
    }

    private TestCaseVersionEntity draft(UserEntity owner) {
        return version(owner, TestCaseVersionStatus.DRAFT, 1, 0);
    }

    private TestCaseVersionEntity version(UserEntity owner, TestCaseVersionStatus status, int major, int minor) {
        TestCaseVersionEntity version = new TestCaseVersionEntity();
        version.setId(UUID.randomUUID());
        version.setStatus(status);
        version.setVersionMajor(major);
        version.setVersionMinor(minor);
        version.setCaseName("Case " + major + "." + minor);
        version.setSelectionMode(SelectionMode.SINGLE);
        version.setEvidenceRequired(false);
        version.setCreatedBy(owner);
        version.setRevisionClosed(status != TestCaseVersionStatus.DRAFT);
        version.setSteps(new ArrayList<>(List.of(step(version, 1, "Step 1", "do something"))));
        return version;
    }

    private TestStepEntity step(TestCaseVersionEntity version, int sequence, String title, String content) {
        TestStepEntity step = new TestStepEntity();
        step.setTestCaseVersion(version);
        step.setSequenceNo(sequence);
        step.setTitle(title);
        step.setContent(content);
        return step;
    }

    private UserEntity user(UUID id) {
        UserEntity user = new UserEntity("user" + id, "User " + id, "hash");
        user.setId(id);
        return user;
    }

    private UserPrincipal principal(UUID id) {
        return new UserPrincipal(id, "user" + id, "hash", "User", true, false, Set.of("ADMIN"),
                Set.of("test_case:draft_create", "test_case:draft_edit", "test_case:submit_review",
                        "test_case:review", "test_case:publish", "test_case:deprecate", "test_case:read"));
    }
}
