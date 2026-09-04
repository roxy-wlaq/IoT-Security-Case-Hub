package com.company.casehub.change.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.change.dto.CapabilityUpdateRequestPayload;
import com.company.casehub.change.dto.CapabilityUpdateRequestResponse;
import com.company.casehub.change.dto.ReviewRequestPayload;
import com.company.casehub.change.service.CapabilityUpdateRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/capability-update-requests")
public class CapabilityUpdateRequestController {
    private final CapabilityUpdateRequestService service;
    public CapabilityUpdateRequestController(CapabilityUpdateRequestService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAuthority('project_capability:read')") public List<CapabilityUpdateRequestResponse> list(@PathVariable UUID projectId, @AuthenticationPrincipal UserPrincipal principal) { return service.list(projectId, principal); }
    @PostMapping("/{capabilityId}") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('capability_request:create')") public CapabilityUpdateRequestResponse submit(@PathVariable UUID projectId, @PathVariable UUID capabilityId, @Valid @RequestBody CapabilityUpdateRequestPayload payload, @AuthenticationPrincipal UserPrincipal principal) { return service.submit(projectId, capabilityId, payload, principal); }
    @PostMapping("/{requestId}/approve") @PreAuthorize("hasAuthority('capability_request:review')") public CapabilityUpdateRequestResponse approve(@PathVariable UUID requestId, @RequestBody(required = false) ReviewRequestPayload payload, @AuthenticationPrincipal UserPrincipal principal) { return service.review(requestId, true, payload, principal); }
    @PostMapping("/{requestId}/reject") @PreAuthorize("hasAuthority('capability_request:review')") public CapabilityUpdateRequestResponse reject(@PathVariable UUID requestId, @RequestBody(required = false) ReviewRequestPayload payload, @AuthenticationPrincipal UserPrincipal principal) { return service.review(requestId, false, payload, principal); }
}
