package com.company.casehub.testcase.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.common.exception.ValidationException;
import com.company.casehub.testcase.dto.AddContributorRequest;
import com.company.casehub.testcase.dto.ContributorResponse;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.ReviewRecordResponse;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.RevisionContributorEntity;
import com.company.casehub.testcase.entity.TestCaseReviewRecordEntity;
import com.company.casehub.testcase.entity.TestCaseStandardMappingEntity;
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
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implements the Test Case Lifecycle (Phase 7): submit review, publish, return,
 * reject, deprecate, create revision, and revision contributor management.
 *
 * <p>Hard rules enforced here (frozen by Final Technical Review V1.0 §11/§12 and the
 * Phase 7 implementation plan):
 * <ul>
 *   <li>No REJECTED version status — Reject keeps {@code status = REVIEW} and sets
 *       {@code revision_closed = true} plus a REJECT review record.</li>
 *   <li>Published Immutable — no method mutates a PUBLISHED version's business
 *       content; Deprecate only flips status/audit fields, Publish only flips
 *       status/audit fields, Create Revision creates a brand-new version.</li>
 *   <li>Single current published — Publish atomically clears the prior current
 *       inside a PESSIMISTIC_WRITE-locked master transaction; the partial unique
 *       index {@code uq_test_case_current_version} is the DB backstop.</li>
 *   <li>Version number server-controlled — Create Revision computes
 *       {@code minor = MAX(minor of same major) + 1} under the master write lock;
 *       client major/minor are never accepted.</li>
 *   <li>ReviewRecord is append-only — methods only insert, never update/delete.</li>
 * </ul>
 */
@Service
public class TestCaseLifecycleService {

    private final MasterTestCaseRepository masterRepository;
    private final TestCaseVersionRepository versionRepository;
    private final TestCaseReviewRecordRepository reviewRecordRepository;
    private final RevisionContributorRepository contributorRepository;
    private final TestStepRepository stepRepository;
    private final TestCaseToolRepository toolRepository;
    private final TestCaseStandardMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final TestCaseAccessPolicy accessPolicy;
    private final TestCaseQueryService queryService;

    public TestCaseLifecycleService(MasterTestCaseRepository masterRepository, TestCaseVersionRepository versionRepository,
                                    TestCaseReviewRecordRepository reviewRecordRepository,
                                    RevisionContributorRepository contributorRepository, TestStepRepository stepRepository,
                                    TestCaseToolRepository toolRepository, TestCaseStandardMappingRepository mappingRepository,
                                    UserRepository userRepository, TestCaseAccessPolicy accessPolicy,
                                    TestCaseQueryService queryService) {
        this.masterRepository = masterRepository;
        this.versionRepository = versionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.contributorRepository = contributorRepository;
        this.stepRepository = stepRepository;
        this.toolRepository = toolRepository;
        this.mappingRepository = mappingRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
        this.queryService = queryService;
    }

    // -------------------------------------------------------------------------
    // Lifecycle actions
    // -------------------------------------------------------------------------

    @Transactional
    public TestCaseDetailResponse submitReview(UUID masterId, LifecycleActionRequest request, UserPrincipal principal) {
        MasterTestCaseEntity master = requireMaster(masterId);
        TestCaseVersionEntity draft = versionRepository
                .findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(masterId, TestCaseVersionStatus.DRAFT)
                .orElseThrow(() -> new ConflictException(ErrorCode.TEST_CASE_DRAFT_REQUIRED,
                        "No Draft exists to submit for: " + masterId));
        ensureEditableRevision(draft);
        if (!accessPolicy.canSubmitReview(draft, principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN,
                    "Only an authorised Draft owner or administrator may submit this draft.");
        }
        requireCompleteDraft(draft);
        draft.setStatus(TestCaseVersionStatus.REVIEW);
        versionRepository.save(draft);
        record(draft, ReviewRecordAction.SUBMIT, principal, request.comment());
        return queryService.detail(masterId, principal);
    }

    @Transactional
    public TestCaseDetailResponse publish(UUID masterId, UUID versionId, LifecycleActionRequest request, UserPrincipal principal) {
        MasterTestCaseEntity master = masterRepository.findByIdWithLock(masterId)
                .orElseThrow(() -> notFound(masterId));
        TestCaseVersionEntity target = requireVisibleVersion(master, versionId, principal);
        if (!accessPolicy.isAdmin(principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN,
                    "Only an administrator may publish a version.");
        }
        ensureReviewTransition(target);
        target.setStatus(TestCaseVersionStatus.PUBLISHED);
        target.setCurrentVersion(true);
        target.setPublishedAt(Instant.now());
        target.setReviewedBy(currentUser(principal));
        target.setRevisionClosed(true);
        master.getVersions().stream()
                .filter(v -> v != target && v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .forEach(v -> v.setCurrentVersion(false));
        versionRepository.save(target);
        record(target, ReviewRecordAction.PUBLISH, principal, request.comment());
        return queryService.detail(masterId, principal);
    }

    @Transactional
    public TestCaseDetailResponse returnReview(UUID masterId, UUID versionId, LifecycleActionRequest request, UserPrincipal principal) {
        MasterTestCaseEntity master = requireMaster(masterId);
        TestCaseVersionEntity target = requireVisibleVersion(master, versionId, principal);
        if (!accessPolicy.isAdmin(principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN,
                    "Only an administrator may return a version to draft.");
        }
        ensureReviewTransition(target);
        requireComment(request, ReviewRecordAction.RETURN);
        target.setStatus(TestCaseVersionStatus.DRAFT);
        target.setRevisionClosed(false);
        target.setReviewedBy(currentUser(principal));
        versionRepository.save(target);
        record(target, ReviewRecordAction.RETURN, principal, request.comment());
        return queryService.detail(masterId, principal);
    }

    @Transactional
    public TestCaseDetailResponse reject(UUID masterId, UUID versionId, LifecycleActionRequest request, UserPrincipal principal) {
        MasterTestCaseEntity master = requireMaster(masterId);
        TestCaseVersionEntity target = requireVisibleVersion(master, versionId, principal);
        if (!accessPolicy.isAdmin(principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN,
                    "Only an administrator may reject a version.");
        }
        ensureReviewTransition(target);
        requireComment(request, ReviewRecordAction.REJECT);
        // R2: status stays REVIEW; only revision_closed flips and a REJECT record is appended.
        target.setStatus(TestCaseVersionStatus.REVIEW);
        target.setRevisionClosed(true);
        target.setReviewedBy(currentUser(principal));
        versionRepository.save(target);
        record(target, ReviewRecordAction.REJECT, principal, request.comment());
        return queryService.detail(masterId, principal);
    }

    @Transactional
    public TestCaseDetailResponse deprecate(UUID masterId, UUID versionId, LifecycleActionRequest request, UserPrincipal principal) {
        MasterTestCaseEntity master = masterRepository.findByIdWithLock(masterId)
                .orElseThrow(() -> notFound(masterId));
        TestCaseVersionEntity target = requireVisibleVersion(master, versionId, principal);
        if (!accessPolicy.isAdmin(principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN,
                    "Only an administrator may deprecate a version.");
        }
        if (target.getStatus() != TestCaseVersionStatus.PUBLISHED) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_LIFECYCLE_TRANSITION_INVALID,
                    "Only PUBLISHED versions can be deprecated.");
        }
        target.setStatus(TestCaseVersionStatus.DEPRECATED);
        target.setCurrentVersion(false);
        target.setDeprecatedAt(Instant.now());
        target.setRevisionClosed(true);
        versionRepository.save(target);
        record(target, ReviewRecordAction.DEPRECATE, principal, request.comment());
        return queryService.detail(masterId, principal);
    }

    @Transactional
    public TestCaseDetailResponse createRevision(UUID masterId, CreateRevisionRequest request, UserPrincipal principal) {
        MasterTestCaseEntity master = masterRepository.findByIdWithLock(masterId)
                .orElseThrow(() -> notFound(masterId));
        TestCaseVersionEntity source = resolveRevisionSource(master, request, principal);
        if (source.getStatus() != TestCaseVersionStatus.PUBLISHED
                && !(source.getStatus() == TestCaseVersionStatus.REVIEW && source.isRevisionClosed())) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_REVISION_SOURCE_INVALID,
                    "A revision can only be created from a PUBLISHED version or a rejected (closed) REVIEW version.");
        }
        int major = source.getVersionMajor();
        int nextMinor = master.getVersions().stream()
                .filter(v -> v.getVersionMajor() == major)
                .mapToInt(TestCaseVersionEntity::getVersionMinor)
                .max().orElse(source.getVersionMinor()) + 1;
        UserEntity user = currentUser(principal);
        TestCaseVersionEntity revision = new TestCaseVersionEntity();
        revision.setMasterTestCase(master);
        revision.setVersionMajor(major);
        revision.setVersionMinor(nextMinor);
        revision.setStatus(TestCaseVersionStatus.DRAFT);
        revision.setCurrentVersion(false);
        revision.setCaseName(source.getCaseName());
        revision.setTestPurpose(source.getTestPurpose());
        revision.setPreconditions(source.getPreconditions());
        revision.setSelectionMode(source.getSelectionMode());
        revision.setEvidenceRequired(source.isEvidenceRequired());
        revision.setEvidenceRequirement(source.getEvidenceRequirement());
        revision.setRemarkRequirement(source.getRemarkRequirement());
        revision.setProgressiveRole(source.getProgressiveRole());
        revision.setBasedOnVersion(source);
        revision.setChangeReason(trimToNull(request.changeReason()));
        revision.setCreatedBy(user);
        revision.setRevisionClosed(false);
        copySteps(source, revision);
        copyTools(source, revision);
        copyMappings(source, revision);
        master.getVersions().add(revision);
        versionRepository.save(revision);
        return queryService.detail(masterId, principal);
    }

    // -------------------------------------------------------------------------
    // Review history (read-only, append-only surface)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReviewRecordResponse> reviewRecords(UUID masterId, UUID versionId, UserPrincipal principal) {
        MasterTestCaseEntity master = requireMaster(masterId);
        TestCaseVersionEntity version = requireVisibleVersion(master, versionId, principal);
        return reviewRecordRepository.findByTestCaseVersionIdOrderByCreatedAtAscIdAsc(version.getId()).stream()
                .map(ReviewRecordResponse::from).toList();
    }

    // -------------------------------------------------------------------------
    // Revision contributors
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ContributorResponse> listContributors(UUID masterId, UserPrincipal principal) {
        return latestVisibleDraft(masterId, principal)
                .map(draft -> contributorRepository.findByTestCaseVersionIdOrderByCreatedAtAsc(draft.getId()).stream()
                        .map(ContributorResponse::from).toList())
                .orElse(List.of());
    }

    @Transactional
    public List<ContributorResponse> addContributor(UUID masterId, AddContributorRequest request, UserPrincipal principal) {
        TestCaseVersionEntity draft = requireManageableDraft(masterId, principal);
        UserEntity target = userRepository.findById(request.userId())
                .filter(UserEntity::isEnabled)
                .orElseThrow(() -> new ValidationException(ErrorCode.TEST_CASE_CONTRIBUTOR_INVALID,
                        "Contributor user does not exist or is disabled."));
        if (Objects.equals(target.getId(), draft.getCreatedBy().getId())) {
            throw new ValidationException(ErrorCode.TEST_CASE_CONTRIBUTOR_INVALID,
                    "The Draft owner is already an editor and cannot be added as a contributor.");
        }
        if (contributorRepository.existsByTestCaseVersionIdAndUserId(draft.getId(), target.getId())) {
            throw new ValidationException(ErrorCode.TEST_CASE_CONTRIBUTOR_INVALID,
                    "This user is already a contributor on this Draft.");
        }
        RevisionContributorEntity contributor = new RevisionContributorEntity();
        contributor.setTestCaseVersion(draft);
        contributor.setUser(target);
        contributor.setAddedBy(currentUser(principal));
        contributorRepository.save(contributor);
        return contributorRepository.findByTestCaseVersionIdOrderByCreatedAtAsc(draft.getId()).stream()
                .map(ContributorResponse::from).toList();
    }

    @Transactional
    public List<ContributorResponse> removeContributor(UUID masterId, UUID userId, UserPrincipal principal) {
        TestCaseVersionEntity draft = requireManageableDraft(masterId, principal);
        contributorRepository.deleteByTestCaseVersionIdAndUserId(draft.getId(), userId);
        contributorRepository.flush();
        return contributorRepository.findByTestCaseVersionIdOrderByCreatedAtAsc(draft.getId()).stream()
                .map(ContributorResponse::from).toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MasterTestCaseEntity requireMaster(UUID masterId) {
        return masterRepository.findById(masterId).orElseThrow(() -> notFound(masterId));
    }

    private TestCaseVersionEntity requireVisibleVersion(MasterTestCaseEntity master, UUID versionId, UserPrincipal principal) {
        TestCaseVersionEntity version = master.getVersions().stream()
                .filter(v -> Objects.equals(v.getId(), versionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_VERSION_NOT_FOUND,
                        "Test case version not found: " + versionId));
        if (!isVisible(master, version, principal)) {
            throw new ResourceNotFoundException(ErrorCode.TEST_CASE_VERSION_NOT_FOUND,
                    "Test case version not found: " + versionId);
        }
        return version;
    }

    private boolean isVisible(MasterTestCaseEntity master, TestCaseVersionEntity version, UserPrincipal principal) {
        return accessPolicy.isVersionVisible(master, version, principal);
    }

    private Optional<TestCaseVersionEntity> latestVisibleDraft(UUID masterId, UserPrincipal principal) {
        MasterTestCaseEntity master = requireMaster(masterId);
        return master.getVersions().stream()
                .filter(v -> v.getStatus() == TestCaseVersionStatus.DRAFT)
                .filter(v -> isVisible(master, v, principal))
                .max(Comparator.comparingInt(TestCaseVersionEntity::getVersionMajor)
                        .thenComparingInt(TestCaseVersionEntity::getVersionMinor));
    }

    private TestCaseVersionEntity requireManageableDraft(UUID masterId, UserPrincipal principal) {
        TestCaseVersionEntity draft = latestVisibleDraft(masterId, principal)
                .orElseThrow(() -> new ConflictException(ErrorCode.TEST_CASE_DRAFT_REQUIRED,
                        "No manageable Draft exists for: " + masterId));
        ensureEditableRevision(draft);
        if (!accessPolicy.canManageContributors(draft, principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_LIFECYCLE_FORBIDDEN,
                    "Only the Draft owner or an administrator may manage contributors.");
        }
        return draft;
    }

    private void ensureEditableRevision(TestCaseVersionEntity draft) {
        if (draft.isRevisionClosed()) {
            throw new ConflictException(ErrorCode.TEST_CASE_REVISION_CLOSED,
                    "This revision is closed and cannot be edited or submitted.");
        }
        if (draft.getStatus() != TestCaseVersionStatus.DRAFT) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_LIFECYCLE_TRANSITION_INVALID,
                    "Only DRAFT versions can be edited or submitted.");
        }
    }

    private void ensureReviewTransition(TestCaseVersionEntity target) {
        if (target.isRevisionClosed()) {
            throw new ConflictException(ErrorCode.TEST_CASE_REVISION_CLOSED,
                    "This revision is closed and cannot be reviewed.");
        }
        if (target.getStatus() != TestCaseVersionStatus.REVIEW) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_LIFECYCLE_TRANSITION_INVALID,
                    "This action requires the version to be in REVIEW status.");
        }
    }

    private void requireCompleteDraft(TestCaseVersionEntity draft) {
        if (!StringUtils.hasText(draft.getCaseName()) || draft.getSteps().isEmpty()) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_DRAFT_INCOMPLETE,
                    "The Draft must have a case name and at least one step before review.");
        }
    }

    private void requireComment(LifecycleActionRequest request, ReviewRecordAction action) {
        if (!StringUtils.hasText(request.comment())) {
            throw new ValidationException(ErrorCode.TEST_CASE_REVIEW_COMMENT_REQUIRED,
                    "A comment is required when performing " + action + ".");
        }
    }

    private TestCaseVersionEntity resolveRevisionSource(MasterTestCaseEntity master, CreateRevisionRequest request, UserPrincipal principal) {
        if (request.sourceVersionId() != null) {
            TestCaseVersionEntity source = requireVisibleVersion(master, request.sourceVersionId(), principal);
            if (source.getStatus() == TestCaseVersionStatus.PUBLISHED) {
                return source;
            }
            // HIGH-03: a rejected revision (REVIEW + revision_closed) may be revised into a new Draft.
            if (accessPolicy.canUseRejectedRevisionSource(source, principal)) {
                return source;
            }
            throw new BusinessRuleException(ErrorCode.TEST_CASE_REVISION_SOURCE_INVALID,
                    "A revision can only be created from a PUBLISHED version or a rejected (closed) REVIEW version.");
        }
        // Default source: current PUBLISHED remains public and must not gain a
        // private resource restriction merely because sourceVersionId was omitted.
        TestCaseVersionEntity currentPublished = master.getVersions().stream()
                .filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .findFirst().orElse(null);
        if (currentPublished != null) {
            return currentPublished;
        }

        TestCaseVersionEntity defaultSource = master.getVersions().stream()
                .filter(version -> accessPolicy.canUseRejectedRevisionSource(version, principal))
                .max(Comparator.comparingInt(TestCaseVersionEntity::getVersionMajor)
                        .thenComparingInt(TestCaseVersionEntity::getVersionMinor))
                .orElse(null);
        if (defaultSource == null) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_REVISION_SOURCE_INVALID,
                    "No current PUBLISHED or authorised rejected version is available to revise.");
        }
        return defaultSource;
    }

    private void record(TestCaseVersionEntity version, ReviewRecordAction action, UserPrincipal principal, String comment) {
        TestCaseReviewRecordEntity record = new TestCaseReviewRecordEntity();
        record.setTestCaseVersion(version);
        record.setAction(action);
        record.setReviewer(currentUser(principal));
        record.setComment(trimToNull(comment));
        reviewRecordRepository.save(record);
    }

    private void copySteps(TestCaseVersionEntity source, TestCaseVersionEntity target) {
        int sequence = 1;
        for (TestStepEntity src : source.getSteps().stream()
                .sorted(Comparator.comparingInt(TestStepEntity::getSequenceNo)).toList()) {
            TestStepEntity step = new TestStepEntity();
            step.setTestCaseVersion(target);
            step.setSequenceNo(sequence++);
            step.setTitle(src.getTitle());
            step.setContent(src.getContent());
            target.getSteps().add(step);
        }
    }

    private void copyTools(TestCaseVersionEntity source, TestCaseVersionEntity target) {
        int order = 0;
        for (TestCaseToolEntity src : source.getTools().stream()
                .sorted(Comparator.comparingInt(TestCaseToolEntity::getSortOrder)).toList()) {
            TestCaseToolEntity relation = new TestCaseToolEntity();
            relation.setTestCaseVersion(target);
            relation.setTool(src.getTool());
            relation.setSortOrder(order++);
            target.getTools().add(relation);
        }
    }

    private void copyMappings(TestCaseVersionEntity source, TestCaseVersionEntity target) {
        for (TestCaseStandardMappingEntity src : source.getStandardMappings()) {
            TestCaseStandardMappingEntity relation = new TestCaseStandardMappingEntity();
            relation.setTestCaseVersion(target);
            relation.setStandardTaskType(src.getStandardTaskType());
            relation.setMappingNote(src.getMappingNote());
            target.getStandardMappings().add(relation);
        }
    }

    private UserEntity currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND,
                        "Current user was not found."));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + id);
    }
}
