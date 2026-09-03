package com.company.casehub.testcase.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.AllowedActions;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.RevisionContributorRepository;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
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
    private final MasterTestCaseRepository masterRepository;

    public TestCaseAccessPolicy(RevisionContributorRepository contributorRepository, MasterTestCaseRepository masterRepository) {
        this.contributorRepository = contributorRepository;
        this.masterRepository = masterRepository;
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
     * Draft edit access: ADMIN, the Draft owner, or an explicit contributor.
     * Always combined with status + revision_closed checks by the caller.
     */
    public boolean canEditDraft(TestCaseVersionEntity version, UserPrincipal principal) {
        return isAdmin(principal) || isOwner(version, principal) || isContributor(version, principal);
    }

    /**
     * Submit Review access is intentionally narrower than Draft edit access.
     * Contributor membership never grants Submit Review; the principal must also
     * hold the lifecycle permission and be an ADMIN or the Draft owner.
     */
    public boolean canSubmitReview(TestCaseVersionEntity version, UserPrincipal principal) {
        return principal.getPermissions().contains("test_case:submit_review")
                && (isAdmin(principal) || isOwner(version, principal));
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
     * <p>Edit is driven by resource membership (ADMIN, owner or contributor).
     * Submit is independent: it requires the submit permission and ADMIN/owner
     * resource relationship. Contributor membership is edit-only.
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
        boolean hasDraftCreate = principal.getPermissions().contains("test_case:draft_create");
        boolean hasReview = principal.getPermissions().contains("test_case:review");
        boolean hasPublish = principal.getPermissions().contains("test_case:publish");
        boolean hasDeprecate = principal.getPermissions().contains("test_case:deprecate");

        boolean openDraft = draft != null && draft.getStatus() == TestCaseVersionStatus.DRAFT && !draft.isRevisionClosed();
        boolean editableDraft = openDraft && canEditDraft(draft, principal);
        boolean submittable = openDraft && canSubmitReview(draft, principal);
        boolean reviewOpen = reviewable != null && reviewable.getStatus() == TestCaseVersionStatus.REVIEW
                && !reviewable.isRevisionClosed();
        boolean canManage = openDraft && canManageContributors(draft, principal);
        boolean deprecateVisible = visible != null && visible.getStatus() == TestCaseVersionStatus.PUBLISHED && hasDeprecate;
        // Create Revision is available when there is a current PUBLISHED version, or a
        // rejected (REVIEW + revision_closed) version the user may revise (HIGH-03).
        boolean createRevisionVisible = hasDraftCreate && (currentPublished != null || hasRevisableRejected(master, principal));

        return new AllowedActions(
                editableDraft,
                hasDraftCreate,
                submittable,
                reviewOpen && hasReview && hasPublish,
                reviewOpen && hasReview,
                reviewOpen && hasReview,
                deprecateVisible,
                createRevisionVisible,
                canManage
        );
    }

    /**
     * Unifies version visibility across the whole module (HIGH-01 / MEDIUM-01).
     * All logged-in users may read PUBLISHED and DEPRECATED versions; DRAFT and
     * REVIEW versions are visible only to ADMIN, the version owner or an explicit
     * revision contributor. A rejected version (REVIEW + revision_closed) is
     * therefore readable by its owner/contributor, which is what lets them
     * create a new revision from it (HIGH-03).
     */
    public boolean isVersionVisible(MasterTestCaseEntity master, TestCaseVersionEntity version, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return true;
        }
        return switch (version.getStatus()) {
            case PUBLISHED, DEPRECATED -> true;
            case DRAFT, REVIEW -> version.getCreatedBy() != null
                    && (Objects.equals(version.getCreatedBy().getId(), principal.getId()) || isContributor(version, principal));
        };
    }

    /**
     * Controller-level pre-authorization helper for the Draft edit endpoint.
     * A contributor's temporary edit right may satisfy the update-Draft gate
     * without the global {@code test_case:draft_edit} permission (HIGH-02).
     */
    public boolean canEditDraftById(UUID masterId, UserPrincipal principal) {
        return masterRepository.findById(masterId)
                .flatMap(master -> master.getVersions().stream()
                        .filter(v -> v.getStatus() == TestCaseVersionStatus.DRAFT && !v.isRevisionClosed())
                        .max(Comparator.comparingInt(TestCaseVersionEntity::getVersionMajor)
                                .thenComparingInt(TestCaseVersionEntity::getVersionMinor)))
                .map(draft -> canEditDraft(draft, principal))
                .orElse(false);
    }

    /**
     * Controller-level pre-authorization helper for Submit Review. This is
     * intentionally independent from {@link #canEditDraftById(UUID, UserPrincipal)}.
     */
    public boolean canSubmitReviewById(UUID masterId, UserPrincipal principal) {
        return masterRepository.findById(masterId)
                .flatMap(master -> master.getVersions().stream()
                        .filter(v -> v.getStatus() == TestCaseVersionStatus.DRAFT && !v.isRevisionClosed())
                        .max(Comparator.comparingInt(TestCaseVersionEntity::getVersionMajor)
                                .thenComparingInt(TestCaseVersionEntity::getVersionMinor)))
                .map(draft -> canSubmitReview(draft, principal))
                .orElse(false);
    }

    /**
     * Resource-level permission to use a rejected revision as a source for a
     * new Draft. This is deliberately separate from both edit and submit rules.
     */
    public boolean canUseRejectedRevisionSource(TestCaseVersionEntity version, UserPrincipal principal) {
        return version.getStatus() == TestCaseVersionStatus.REVIEW && version.isRevisionClosed()
                && (isAdmin(principal) || isOwner(version, principal) || isContributor(version, principal));
    }

    private boolean hasRevisableRejected(MasterTestCaseEntity master, UserPrincipal principal) {
        return master.getVersions().stream()
                .anyMatch(v -> v.getStatus() == TestCaseVersionStatus.REVIEW && v.isRevisionClosed()
                        && (isAdmin(principal) || isOwner(v, principal) || isContributor(v, principal)));
    }

    private TestCaseVersionEntity latestReviewFor(MasterTestCaseEntity master, UserPrincipal principal) {
        return master.getVersions().stream()
                .filter(v -> v.getStatus() == TestCaseVersionStatus.REVIEW)
                .filter(v -> isAdmin(principal) || isOwner(v, principal) || isContributor(v, principal))
                .max((a, b) -> Integer.compare(a.getVersionMajor() * 10000 + a.getVersionMinor(),
                        b.getVersionMajor() * 10000 + b.getVersionMinor()))
                .orElse(null);
    }
}
