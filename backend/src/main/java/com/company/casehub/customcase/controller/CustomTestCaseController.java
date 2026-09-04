package com.company.casehub.customcase.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.customcase.dto.CustomTestCaseRequest;
import com.company.casehub.customcase.dto.CustomTestCaseResponse;
import com.company.casehub.customcase.dto.LibrarySubmissionResponse;
import com.company.casehub.customcase.service.CustomTestCaseService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/custom-test-cases")
public class CustomTestCaseController {
    private final CustomTestCaseService service;
    public CustomTestCaseController(CustomTestCaseService service) { this.service = service; }
    @GetMapping
    @PreAuthorize("hasAuthority('project_custom_test_case:create')")
    public List<CustomTestCaseResponse> list(@PathVariable UUID projectId, @AuthenticationPrincipal UserPrincipal principal) { return service.list(projectId, principal); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('project_custom_test_case:create')")
    public CustomTestCaseResponse create(@PathVariable UUID projectId, @Valid @RequestBody CustomTestCaseRequest request, @AuthenticationPrincipal UserPrincipal principal) { return service.create(projectId, request, principal); }
    @PutMapping("/{customId}")
    @PreAuthorize("hasAuthority('project_custom_test_case:edit_own_or_assigned')")
    public CustomTestCaseResponse update(@PathVariable UUID projectId, @PathVariable UUID customId, @Valid @RequestBody CustomTestCaseRequest request, @AuthenticationPrincipal UserPrincipal principal) { return service.update(projectId, customId, request, principal); }
    @PostMapping("/{customId}/assignees/{userId}")
    @PreAuthorize("hasAuthority('project_test_case:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable UUID projectId, @PathVariable UUID customId, @PathVariable UUID userId, @AuthenticationPrincipal UserPrincipal principal) { service.assign(projectId, customId, userId, principal); }
    @PostMapping("/{customId}/submit-to-library")
    @PreAuthorize("hasAuthority('project_custom_test_case:edit_own_or_assigned')")
    public LibrarySubmissionResponse submit(@PathVariable UUID projectId, @PathVariable UUID customId, @AuthenticationPrincipal UserPrincipal principal) { return service.submitToLibrary(projectId, customId, principal); }
}
