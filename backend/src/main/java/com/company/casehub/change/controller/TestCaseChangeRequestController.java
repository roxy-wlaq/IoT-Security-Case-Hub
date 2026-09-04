package com.company.casehub.change.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.change.dto.ReviewRequestPayload;
import com.company.casehub.change.dto.TestCaseChangeRequestPayload;
import com.company.casehub.change.dto.TestCaseChangeRequestResponse;
import com.company.casehub.change.service.TestCaseChangeRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test-cases/{masterId}/change-requests")
public class TestCaseChangeRequestController {
    private final TestCaseChangeRequestService service;
    public TestCaseChangeRequestController(TestCaseChangeRequestService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAuthority('test_case:read')") public List<TestCaseChangeRequestResponse> list(@PathVariable UUID masterId, @AuthenticationPrincipal UserPrincipal principal) { return service.list(masterId, principal); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('change_request:create')") public TestCaseChangeRequestResponse submit(@PathVariable UUID masterId, @Valid @RequestBody TestCaseChangeRequestPayload payload, @AuthenticationPrincipal UserPrincipal principal) { return service.submit(masterId, payload, principal); }
    @PostMapping("/{requestId}/approve") @PreAuthorize("hasAuthority('change_request:review')") public TestCaseChangeRequestResponse approve(@PathVariable UUID requestId, @RequestBody(required = false) ReviewRequestPayload payload, @AuthenticationPrincipal UserPrincipal principal) { return service.review(requestId, true, payload, principal); }
    @PostMapping("/{requestId}/reject") @PreAuthorize("hasAuthority('change_request:review')") public TestCaseChangeRequestResponse reject(@PathVariable UUID requestId, @RequestBody(required = false) ReviewRequestPayload payload, @AuthenticationPrincipal UserPrincipal principal) { return service.review(requestId, false, payload, principal); }
}
