package com.company.casehub.testcase.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.PagedResponse;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.dto.TestCaseSummaryResponse;
import com.company.casehub.testcase.dto.TestCaseVersionResponse;
import com.company.casehub.testcase.dto.UpdateDraftRequest;
import com.company.casehub.testcase.dto.VersionSummaryResponse;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.TestCaseQueryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-cases")
public class TestCaseController {

    private final TestCaseDraftService draftService;
    private final TestCaseQueryService queryService;

    public TestCaseController(TestCaseDraftService draftService, TestCaseQueryService queryService) {
        this.draftService = draftService;
        this.queryService = queryService;
    }

    @GetMapping
    public PagedResponse<TestCaseSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) List<UUID> tagIds,
            @RequestParam(required = false) List<UUID> toolIds,
            @RequestParam(required = false) List<UUID> standardTaskTypeIds,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal principal) {
        return queryService.list(q, categoryId, tagIds, toolIds, standardTaskTypeIds, status, page, size, sort, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('test_case:draft_create')")
    public TestCaseDetailResponse create(@Valid @RequestBody CreateDraftRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return draftService.createDraft(request, principal);
    }

    @GetMapping("/{masterId}")
    public TestCaseDetailResponse detail(@PathVariable UUID masterId, @AuthenticationPrincipal UserPrincipal principal) {
        return queryService.detail(masterId, principal);
    }

    @PutMapping("/{masterId}/draft")
    @PreAuthorize("hasAuthority('test_case:draft_edit') or @testCaseAccessPolicy.canEditDraftById(#masterId, principal)")
    public TestCaseDetailResponse updateDraft(@PathVariable UUID masterId, @Valid @RequestBody UpdateDraftRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return draftService.updateDraft(masterId, request, principal);
    }

    @GetMapping("/{masterId}/versions")
    public List<VersionSummaryResponse> versions(@PathVariable UUID masterId, @AuthenticationPrincipal UserPrincipal principal) {
        return queryService.versions(masterId, principal);
    }

    @GetMapping("/{masterId}/versions/{versionId}")
    public TestCaseVersionResponse version(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return queryService.version(masterId, versionId, principal);
    }
}
