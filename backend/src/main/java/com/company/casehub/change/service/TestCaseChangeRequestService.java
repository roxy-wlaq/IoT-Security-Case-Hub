package com.company.casehub.change.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.change.dto.ReviewRequestPayload;
import com.company.casehub.change.dto.TestCaseChangeRequestPayload;
import com.company.casehub.change.dto.TestCaseChangeRequestResponse;
import com.company.casehub.change.entity.TestCaseChangeRequestEntity;
import com.company.casehub.change.entity.TestCaseChangeRequestStatus;
import com.company.casehub.change.repository.TestCaseChangeRequestRepository;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.RevisionContributorEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.RevisionContributorRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.service.TestCaseAccessPolicy;
import com.company.casehub.testcase.service.TestCaseLifecycleService;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseChangeRequestService {
    private final TestCaseChangeRequestRepository requestRepository;
    private final MasterTestCaseRepository masterRepository;
    private final TestCaseVersionRepository versionRepository;
    private final RevisionContributorRepository contributorRepository;
    private final UserRepository userRepository;
    private final TestCaseAccessPolicy accessPolicy;
    private final TestCaseLifecycleService lifecycleService;

    public TestCaseChangeRequestService(TestCaseChangeRequestRepository requestRepository, MasterTestCaseRepository masterRepository,
                                        TestCaseVersionRepository versionRepository, RevisionContributorRepository contributorRepository,
                                        UserRepository userRepository, TestCaseAccessPolicy accessPolicy,
                                        TestCaseLifecycleService lifecycleService) {
        this.requestRepository = requestRepository; this.masterRepository = masterRepository; this.versionRepository = versionRepository;
        this.contributorRepository = contributorRepository; this.userRepository = userRepository; this.accessPolicy = accessPolicy; this.lifecycleService = lifecycleService;
    }

    @Transactional
    public TestCaseChangeRequestResponse submit(UUID masterId, TestCaseChangeRequestPayload payload, UserPrincipal principal) {
        var master = masterRepository.findById(masterId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found"));
        TestCaseVersionEntity source = versionRepository.findByIdAndMasterTestCaseId(payload.sourceVersionId(), masterId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_VERSION_NOT_FOUND, "Source version not found"));
        if (!accessPolicy.isVersionVisible(master, source, principal) || source.getStatus() != TestCaseVersionStatus.PUBLISHED) throw new ForbiddenOperationException(ErrorCode.CHANGE_REQUEST_REVIEW_FORBIDDEN, "The source version is not available");
        TestCaseChangeRequestEntity request = new TestCaseChangeRequestEntity(); request.setMasterTestCase(master); request.setSourceVersion(source); request.setReason(payload.reason().trim()); request.setSubmittedBy(currentUser(principal));
        return response(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<TestCaseChangeRequestResponse> list(UUID masterId, UserPrincipal principal) {
        var master = masterRepository.findById(masterId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found"));
        // A Change Request is always bound to a version that has been published, so a Master
        // without one (a fresh Draft, which is what Submit-to-Library produces, or a Master whose
        // only Published version was later deprecated) simply has nothing to list. Returning an
        // empty list here keeps the endpoint deterministic instead of letting a missing version
        // escape as an unmapped NoSuchElementException (HTTP 500).
        var published = master.getVersions().stream()
                .filter(v -> v.getStatus() == TestCaseVersionStatus.PUBLISHED || v.getStatus() == TestCaseVersionStatus.DEPRECATED)
                .findFirst();
        if (published.isEmpty()) return List.of();
        if (!accessPolicy.isVersionVisible(master, published.get(), principal)) throw new ForbiddenOperationException(ErrorCode.CHANGE_REQUEST_REVIEW_FORBIDDEN, "You cannot view these requests");
        return requestRepository.findByMasterTestCaseIdOrderByCreatedAtDesc(masterId).stream().map(this::response).toList();
    }

    @Transactional
    public TestCaseChangeRequestResponse review(UUID requestId, boolean approve, ReviewRequestPayload payload, UserPrincipal principal) {
        TestCaseChangeRequestEntity request = requestRepository.findByIdForUpdate(requestId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CHANGE_REQUEST_NOT_FOUND, "Change Request not found"));
        if (!(principal.getRoles().contains("TEST_COORDINATOR") || principal.getRoles().contains("ADMIN"))) throw new ForbiddenOperationException(ErrorCode.CHANGE_REQUEST_REVIEW_FORBIDDEN, "Only a Coordinator or Admin may review");
        if (request.getStatus() != TestCaseChangeRequestStatus.PENDING) throw new ConflictException(ErrorCode.CHANGE_REQUEST_STATE_INVALID, "Request is already reviewed");
        UserEntity reviewer = currentUser(principal); request.setReviewedBy(reviewer); request.setReviewComment(payload == null ? null : trim(payload.comment()));
        if (!approve) { request.setStatus(TestCaseChangeRequestStatus.REJECTED); return response(requestRepository.save(request)); }
        TestCaseDetailResponse revision = lifecycleService.createRevision(request.getMasterTestCase().getId(), new CreateRevisionRequest(request.getSourceVersion().getId(), request.getReason()), principal);
        TestCaseVersionEntity draft = versionRepository.findById(revision.draftVersion().id()).orElseThrow(); draft.setChangeRequestId(request.getId()); request.setRevisionDraftVersion(draft);
        if (!request.getSubmittedBy().getId().equals(reviewer.getId())) { RevisionContributorEntity contributor = new RevisionContributorEntity(); contributor.setTestCaseVersion(draft); contributor.setUser(request.getSubmittedBy()); contributor.setAddedBy(reviewer); contributorRepository.save(contributor); }
        request.setStatus(TestCaseChangeRequestStatus.APPROVED);
        return response(requestRepository.save(request));
    }

    private TestCaseChangeRequestResponse response(TestCaseChangeRequestEntity r) { return new TestCaseChangeRequestResponse(r.getId(), r.getMasterTestCase().getId(), r.getSourceVersion().getId(), r.getReason(), r.getSubmittedBy().getId(), r.getReviewedBy() == null ? null : r.getReviewedBy().getId(), r.getRevisionDraftVersion() == null ? null : r.getRevisionDraftVersion().getId(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()); }
    private UserEntity currentUser(UserPrincipal p) { return userRepository.findById(p.getId()).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found")); }
    private static String trim(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }
}
