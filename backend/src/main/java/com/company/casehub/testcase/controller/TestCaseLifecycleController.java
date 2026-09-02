package com.company.casehub.testcase.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.AddContributorRequest;
import com.company.casehub.testcase.dto.ContributorResponse;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.ReviewRecordResponse;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.service.TestCaseLifecycleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 7 Test Case Lifecycle endpoints. Sits under the existing
 * {@code /api/v1/test-cases} resource so there is a single REST surface; Phase 6
 * CRUD/query endpoints remain in {@link TestCaseController}.
 *
 * <p>Controller-level {@code @PreAuthorize} only checks the permission code; the
 * resource-level gate (ownership / contributor / status / revision_closed) and
 * Published Immutable are enforced in {@link TestCaseLifecycleService} +
 * {@link com.company.casehub.testcase.service.TestCaseAccessPolicy}.
 */
@RestController
@RequestMapping("/api/v1/test-cases")
public class TestCaseLifecycleController {

    private final TestCaseLifecycleService lifecycleService;

    public TestCaseLifecycleController(TestCaseLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/{masterId}/draft/submit-review")
    @PreAuthorize("hasAuthority('test_case:submit_review')")
    public TestCaseDetailResponse submitReview(@PathVariable UUID masterId,
                                                @RequestBody(required = false) LifecycleActionRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.submitReview(masterId, requestOrEmpty(request), principal);
    }

    @PostMapping("/{masterId}/versions/{versionId}/publish")
    @PreAuthorize("hasAuthority('test_case:review') and hasAuthority('test_case:publish')")
    public TestCaseDetailResponse publish(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                          @RequestBody(required = false) LifecycleActionRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.publish(masterId, versionId, requestOrEmpty(request), principal);
    }

    @PostMapping("/{masterId}/versions/{versionId}/return")
    @PreAuthorize("hasAuthority('test_case:review')")
    public TestCaseDetailResponse returnReview(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                                @RequestBody(required = false) LifecycleActionRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.returnReview(masterId, versionId, requestOrEmpty(request), principal);
    }

    @PostMapping("/{masterId}/versions/{versionId}/reject")
    @PreAuthorize("hasAuthority('test_case:review')")
    public TestCaseDetailResponse reject(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                         @RequestBody(required = false) LifecycleActionRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.reject(masterId, versionId, requestOrEmpty(request), principal);
    }

    @PostMapping("/{masterId}/versions/{versionId}/deprecate")
    @PreAuthorize("hasAuthority('test_case:deprecate')")
    public TestCaseDetailResponse deprecate(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                             @RequestBody(required = false) LifecycleActionRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.deprecate(masterId, versionId, requestOrEmpty(request), principal);
    }

    @PostMapping("/{masterId}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('test_case:draft_create')")
    public TestCaseDetailResponse createRevision(@PathVariable UUID masterId,
                                                  @Valid @RequestBody(required = false) CreateRevisionRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.createRevision(masterId,
                request == null ? new CreateRevisionRequest(null, null) : request, principal);
    }

    @GetMapping("/{masterId}/versions/{versionId}/review-records")
    public List<ReviewRecordResponse> reviewRecords(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.reviewRecords(masterId, versionId, principal);
    }

    @GetMapping("/{masterId}/draft/contributors")
    public List<ContributorResponse> listContributors(@PathVariable UUID masterId,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.listContributors(masterId, principal);
    }

    @PostMapping("/{masterId}/draft/contributors")
    @PreAuthorize("hasAuthority('test_case:draft_edit')")
    public List<ContributorResponse> addContributor(@PathVariable UUID masterId,
                                                     @Valid @RequestBody AddContributorRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.addContributor(masterId, request, principal);
    }

    @DeleteMapping("/{masterId}/draft/contributors/{userId}")
    @PreAuthorize("hasAuthority('test_case:draft_edit')")
    public List<ContributorResponse> removeContributor(@PathVariable UUID masterId, @PathVariable UUID userId,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return lifecycleService.removeContributor(masterId, userId, principal);
    }

    private static LifecycleActionRequest requestOrEmpty(LifecycleActionRequest request) {
        return request == null ? new LifecycleActionRequest(null) : request;
    }
}
