package com.company.casehub.testcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.AllowedActions;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.RevisionContributorRepository;
import com.company.casehub.user.entity.UserEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the resource-level permission gate. Verifies the second
 * authorization layer (ownership / contributor / admin) that sits behind the
 * permission-code check, and the 9-field {@link AllowedActions} computation.
 */
@ExtendWith(MockitoExtension.class)
class TestCaseAccessPolicyTest {

    @Mock private RevisionContributorRepository contributorRepository;
    @Mock private MasterTestCaseRepository masterRepository;
    @InjectMocks private TestCaseAccessPolicy policy;

    @Test
    void adminIsAlwaysRecognised() {
        assertThat(policy.isAdmin(admin())).isTrue();
        assertThat(policy.isAdmin(coordinator())).isFalse();
    }

    @Test
    void ownerIsIdentifiedByVersionCreator() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity version = version(owner, TestCaseVersionStatus.DRAFT);

        assertThat(policy.isOwner(version, principal(ownerId, "TEST_COORDINATOR"))).isTrue();
        assertThat(policy.isOwner(version, principal(UUID.randomUUID(), "TEST_COORDINATOR"))).isFalse();
    }

    @Test
    void contributorMembershipIsReadFromRepository() {
        UUID ownerId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity version = version(owner, TestCaseVersionStatus.DRAFT);
        when(contributorRepository.existsByTestCaseVersionIdAndUserId(version.getId(), contributorId)).thenReturn(true);

        assertThat(policy.isContributor(version, principal(contributorId, "TESTER"))).isTrue();
    }

    @Test
    void canEditOrSubmitAllowsAdminOwnerAndContributor() {
        UUID ownerId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity draft = version(owner, TestCaseVersionStatus.DRAFT);

        when(contributorRepository.existsByTestCaseVersionIdAndUserId(draft.getId(), contributorId)).thenReturn(true);

        // admin short-circuits before isContributor is called
        assertThat(policy.canEditOrSubmit(draft, admin())).isTrue();
        // owner short-circuits before isContributor is called
        assertThat(policy.canEditOrSubmit(draft, principal(ownerId, "TEST_COORDINATOR"))).isTrue();
        // contributor is confirmed via repository
        assertThat(policy.canEditOrSubmit(draft, principal(contributorId, "TESTER"))).isTrue();
    }

    @Test
    void canEditOrSubmitDeniesNonOwnerNonContributor() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity draft = version(owner, TestCaseVersionStatus.DRAFT);

        // No stub — Mockito defaults boolean to false
        assertThat(policy.canEditOrSubmit(draft, principal(UUID.randomUUID(), "TESTER"))).isFalse();
    }

    @Test
    void canManageContributorsAllowsAdminAndOwnerButNotContributor() {
        UUID ownerId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity draft = version(owner, TestCaseVersionStatus.DRAFT);

        assertThat(policy.canManageContributors(draft, admin())).isTrue();
        assertThat(policy.canManageContributors(draft, principal(ownerId, "TEST_COORDINATOR"))).isTrue();
        assertThat(policy.canManageContributors(draft, principal(contributorId, "TESTER"))).isFalse();
    }

    @Test
    void buildAllowedActionsForEditableDraftEnablesEditSubmitAndManage() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setVersions(new java.util.ArrayList<>());
        TestCaseVersionEntity draft = version(owner, TestCaseVersionStatus.DRAFT);
        draft.setRevisionClosed(false);
        master.getVersions().add(draft);

        AllowedActions actions = policy.buildAllowedActions(master, draft, draft,
                principal(ownerId, "TEST_COORDINATOR", "test_case:draft_edit", "test_case:draft_create",
                        "test_case:submit_review", "test_case:read"));

        assertThat(actions.editDraft()).isTrue();
        assertThat(actions.submitReview()).isTrue();
        assertThat(actions.manageContributors()).isTrue();
        assertThat(actions.createDraft()).isTrue();
        assertThat(actions.createRevision()).isFalse();
        assertThat(actions.publish()).isFalse();
        assertThat(actions.reject()).isFalse();
    }

    @Test
    void buildAllowedActionsForReviewVersionEnablesPublishReturnRejectForAdmin() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setVersions(new java.util.ArrayList<>());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW);
        review.setRevisionClosed(false);
        master.getVersions().add(review);

        AllowedActions actions = policy.buildAllowedActions(master, null, review, admin());

        assertThat(actions.publish()).isTrue();
        assertThat(actions.returnReview()).isTrue();
        assertThat(actions.reject()).isTrue();
        assertThat(actions.editDraft()).isFalse();
        assertThat(actions.submitReview()).isFalse();
    }

    @Test
    void buildAllowedActionsForPublishedVersionEnablesDeprecateAndCreateRevision() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setVersions(new java.util.ArrayList<>());
        TestCaseVersionEntity published = version(owner, TestCaseVersionStatus.PUBLISHED);
        published.setCurrentVersion(true);
        master.getVersions().add(published);

        AllowedActions actions = policy.buildAllowedActions(master, null, published, admin());

        assertThat(actions.deprecate()).isTrue();
        assertThat(actions.createRevision()).isTrue();
    }

    @Test
    void buildAllowedActionsDisablesActionsWhenRevisionClosed() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setVersions(new java.util.ArrayList<>());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW);
        review.setRevisionClosed(true);
        master.getVersions().add(review);

        AllowedActions actions = policy.buildAllowedActions(master, null, review, admin());

        assertThat(actions.publish()).isFalse();
        assertThat(actions.returnReview()).isFalse();
        assertThat(actions.reject()).isFalse();
    }

    @Test
    void buildAllowedActionsForCoordinatorWithoutReviewPermissionDisablesReviewActions() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setVersions(new java.util.ArrayList<>());
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW);
        review.setRevisionClosed(false);
        master.getVersions().add(review);

        // Coordinator has submit permission but not review/publish — publish/return/reject must be false.
        UserPrincipal coordinator = new UserPrincipal(ownerId, "coord", "hash", "Coord", true, false,
                Set.of("TEST_COORDINATOR"), Set.of("test_case:submit_review", "test_case:draft_edit"));
        AllowedActions actions = policy.buildAllowedActions(master, null, review, coordinator);

        assertThat(actions.publish()).isFalse();
        assertThat(actions.returnReview()).isFalse();
        assertThat(actions.reject()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Unified visibility (HIGH-01 / MEDIUM-01) — single source of truth
    // -------------------------------------------------------------------------

    @Test
    void deprecatedVersionVisibleToAnyLoggedInUser() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity deprecated = version(owner, TestCaseVersionStatus.DEPRECATED);

        // A totally unrelated user (no ownership / no contributor / not admin) may read it.
        MasterTestCaseEntity master = masterWith(List.of(deprecated));
        assertThat(policy.isVersionVisible(master, deprecated, principal(UUID.randomUUID(), "TESTER"))).isTrue();
    }

    @Test
    void publishedVersionVisibleToAnyLoggedInUser() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity published = version(owner, TestCaseVersionStatus.PUBLISHED);

        assertThat(policy.isVersionVisible(masterWith(List.of(published)), published,
                principal(UUID.randomUUID(), "TESTER"))).isTrue();
    }

    @Test
    void draftVisibleOnlyToOwnerContributorOrAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity draft = version(owner, TestCaseVersionStatus.DRAFT);

        // owner sees it
        assertThat(policy.isVersionVisible(masterWith(List.of(draft)), draft, principal(ownerId, "TEST_COORDINATOR"))).isTrue();
        // contributor sees it
        when(contributorRepository.existsByTestCaseVersionIdAndUserId(draft.getId(), contributorId)).thenReturn(true);
        assertThat(policy.isVersionVisible(masterWith(List.of(draft)), draft, principal(contributorId, "TESTER"))).isTrue();
        // admin sees it
        assertThat(policy.isVersionVisible(masterWith(List.of(draft)), draft, admin())).isTrue();
        // unrelated user does NOT see it
        assertThat(policy.isVersionVisible(masterWith(List.of(draft)), draft, principal(UUID.randomUUID(), "TESTER"))).isFalse();
    }

    @Test
    void reviewVisibleOnlyToOwnerContributorOrAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity review = version(owner, TestCaseVersionStatus.REVIEW);

        when(contributorRepository.existsByTestCaseVersionIdAndUserId(review.getId(), contributorId)).thenReturn(true);
        assertThat(policy.isVersionVisible(masterWith(List.of(review)), review, principal(contributorId, "TESTER"))).isTrue();
        assertThat(policy.isVersionVisible(masterWith(List.of(review)), review, principal(UUID.randomUUID(), "TESTER"))).isFalse();
    }

    @Test
    void rejectedReviewVersionVisibleToOwnerContributorOrAdminOnly() {
        UUID ownerId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity rejected = version(owner, TestCaseVersionStatus.REVIEW);
        rejected.setRevisionClosed(true);

        // The owner/contributor of a rejected (closed) REVIEW version can still read it — this
        // is what lets them create a new revision from it (HIGH-03).
        when(contributorRepository.existsByTestCaseVersionIdAndUserId(rejected.getId(), contributorId)).thenReturn(true);
        assertThat(policy.isVersionVisible(masterWith(List.of(rejected)), rejected, principal(contributorId, "TESTER"))).isTrue();
        // An unrelated user cannot read the rejected review version.
        assertThat(policy.isVersionVisible(masterWith(List.of(rejected)), rejected, principal(UUID.randomUUID(), "TESTER"))).isFalse();
    }

    @Test
    void adminSeesAllVersionsRegardlessOfStatus() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        MasterTestCaseEntity master = masterWith(List.of(
                version(owner, TestCaseVersionStatus.DRAFT),
                version(owner, TestCaseVersionStatus.REVIEW),
                version(owner, TestCaseVersionStatus.PUBLISHED),
                version(owner, TestCaseVersionStatus.DEPRECATED)));

        for (TestCaseVersionEntity v : master.getVersions()) {
            assertThat(policy.isVersionVisible(master, v, admin())).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Controller pre-auth helper (HIGH-02): canEditDraftById
    // -------------------------------------------------------------------------

    @Test
    void canEditDraftByIdTrueForContributorOnOpenDraft() {
        UUID ownerId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        TestCaseVersionEntity draft = version(owner, TestCaseVersionStatus.DRAFT);
        draft.setRevisionClosed(false);

        MasterTestCaseEntity master = masterWith(List.of(draft));
        when(masterRepository.findById(master.getId())).thenReturn(Optional.of(master));
        when(contributorRepository.existsByTestCaseVersionIdAndUserId(draft.getId(), contributorId)).thenReturn(true);

        // A TESTER contributor (no global draft_edit) may still edit the open draft.
        assertThat(policy.canEditDraftById(master.getId(), principal(contributorId, "TESTER"))).isTrue();
    }

    @Test
    void canEditDraftByIdFalseWhenNoOpenDraftOrNotMember() {
        UUID ownerId = UUID.randomUUID();
        UserEntity owner = user(ownerId);
        // Only a closed REVIEW (rejected) version exists — no open DRAFT to edit.
        TestCaseVersionEntity rejected = version(owner, TestCaseVersionStatus.REVIEW);
        rejected.setRevisionClosed(true);

        MasterTestCaseEntity master = masterWith(List.of(rejected));
        when(masterRepository.findById(master.getId())).thenReturn(Optional.of(master));

        // Unrelated user: no open draft and not a member → false.
        assertThat(policy.canEditDraftById(master.getId(), principal(UUID.randomUUID(), "TESTER"))).isFalse();
    }

    private UserEntity user(UUID id) {
        UserEntity user = new UserEntity("user" + id, "User", "hash");
        user.setId(id);
        return user;
    }

    private TestCaseVersionEntity version(UserEntity owner, TestCaseVersionStatus status) {
        TestCaseVersionEntity version = new TestCaseVersionEntity();
        version.setId(UUID.randomUUID());
        version.setStatus(status);
        version.setCreatedBy(owner);
        version.setCaseName("Case");
        version.setSelectionMode(SelectionMode.SINGLE);
        version.setEvidenceRequired(false);
        version.setRevisionClosed(status != TestCaseVersionStatus.DRAFT);
        return version;
    }

    private MasterTestCaseEntity masterWith(List<TestCaseVersionEntity> versions) {
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setId(UUID.randomUUID());
        master.setVersions(new ArrayList<>(versions));
        return master;
    }

    private UserPrincipal admin() {
        return principal(UUID.randomUUID(), "ADMIN", "test_case:draft_create", "test_case:draft_edit",
                "test_case:submit_review", "test_case:review", "test_case:publish", "test_case:deprecate", "test_case:read");
    }

    private UserPrincipal coordinator() {
        return principal(UUID.randomUUID(), "TEST_COORDINATOR", "test_case:draft_create", "test_case:draft_edit",
                "test_case:submit_review", "test_case:read");
    }

    private UserPrincipal principal(UUID id, String role, String... permissions) {
        return new UserPrincipal(id, "user" + id, "hash", "User", true, false, Set.of(role), Set.of(permissions));
    }
}
