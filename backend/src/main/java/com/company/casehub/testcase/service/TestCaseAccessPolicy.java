package com.company.casehub.testcase.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.AllowedActions;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.RevisionContributorRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for resource-level (row-level) lifecycle permissions.
 *
 * <p>Controller-level {@code @PreAuthorize} only checks the permission code; this
 * component enforces the second gate: ownership, contributor membership, current
 * status and {@code revision_closed}. ADMIN never gets to bypass Published
 * Immutable or to edit someone else's Draft without being an explicit actor —
 * the ADMIN branch here only grants the same edit/submit access an owner has,
 * still gated on {@code status == DRAFT && revision_closed == false}.
 *
 * <p>Contributor membership is read from {@code revision_contributors} (Data Model §50)
 * and is distinct from the Draft owner.
 */
@Component
public class TestCaseAccessPolicy {

    private final RevisionContributorRepository contributorRepository;

    public TestCaseAccessPolicy(RevisionContributorRepository contributorRepository) {
        this.contributorRepository = contributorRepository;
    }

    public boolean isAdmin(UserPrincipal principal) {
        return principal.getRoles().contains("ADMIN");
    }

    public boolean isOwner(TestCaseVersionEntity version, UserPrincipal principal) {
        return version.getCreatedBy() != null && Objects.equals(version.getCreatedBy().getId(), principal.getId());
    }

    public boolean isContributor(TestCaseVersionEntity version, UserPrincipal principal) {
        return contributorRepository.existsByTestCaseVersionIdAndUserId(version.getId(), principal.getId());
    }

    /**
     * Edit / Submit access: ADMIN, the Draft owner, or an explicit contributor.
     * Always combined with status + revision_closed checks by the caller.
     */
    public boolean canEditOrSubmit(TestCaseVersionEntity version, UserPrincipal principal) {
        return isAdmin(principal) || isOwner(version, principal) || isContributor(version, principal);
    }

    /**
     * Contributor management: ADMIN or the Draft owner only (not contributors themselves).
     */
    public boolean canManageContributors(TestCaseVersionEntity version, UserPrincipal principal) {
        return isAdmin(principal) || isOwner(version, principal);
    }

    /**
     * Builds the 9-field {@link AllowedActions} for a detail response.
     *
     * @param master           the master aggregate (with versions hydrated)
     * @param draft            the latest visible DRAFT (may be null)
     * @param visible          the version currently surfaced to the user (may be null)
     * @param principal        the current user
     */
    public AllowedActions buildAllowedActions(MasterTestCaseEntity master, TestCaseVersionEntity draft,
                                             TestCaseVersionEntity visible, UserPrincipal principal) {
        TestCaseVersionEntity currentPublished = master.getVersions().stream()
                .filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .findFirst().orElse(null);
        TestCaseVersionEntity reviewable = latestReviewFor(master, principal);
        boolean hasDraftEdit = principal.getPermissions().contains("test_case:draft_edit");
        boolean hasDraftCreate = principal.getPermissions().contains("test_case:draft_create");
        boolean hasSubmit = principal.getPermissions().contains("test_case:submit_review");
        boolean hasReview = principal.getPermissions().contains("test_case:review");
        boolean hasPublish = principal.getPermissions().contains("test_case:publish");
        boolean hasDeprecate = principal.getPermissions().contains("test_case:deprecate");

        boolean editableDraft = draft != null && draft.getStatus() == TestCaseVersionStatus.DRAFT
                && !draft.isRevisionClosed() && hasDraftEdit && canEditOrSubmit(draft, principal);
        boolean submittable = draft != null && draft.getStatus() == TestCaseVersionStatus.DRAFT
                && !draft.isRevisionClosed() && hasSubmit && canEditOrSubmit(draft, principal);
        boolean reviewOpen = reviewable != null && reviewable.getStatus() == TestCaseVersionStatus.REVIEW
                && !reviewable.isRevisionClosed();
        boolean canManage = draft != null && draft.getStatus() == TestCaseVersionStatus.DRAFT
                && !draft.isRevisionClosed() && hasDraftEdit && canManageContributors(draft, principal);
        boolean deprecateVisible = visible != null && visible.getStatus() == TestCaseVersionStatus.PUBLISHED && hasDeprecate;

        return new AllowedActions(
                editableDraft,
                hasDraftCreate,
                submittable,
                reviewOpen && hasReview && hasPublish,
                reviewOpen && hasReview,
                reviewOpen && hasReview,
                deprecateVisible,
                currentPublished != null && hasDraftCreate,
                canManage
        );
    }

    private TestCaseVersionEntity latestReviewFor(MasterTestCaseEntity master, UserPrincipal principal) {
        return master.getVersions().stream()
                .filter(v -> v.getStatus() == TestCaseVersionStatus.REVIEW)
                .filter(v -> isAdmin(principal))
                .max((a, b) -> Integer.compare(a.getVersionMajor() * 10000 + a.getVersionMinor(),
                        b.getVersionMajor() * 10000 + b.getVersionMinor()))
                .orElse(null);
    }
}
