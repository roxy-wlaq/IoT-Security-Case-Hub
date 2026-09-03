package com.company.casehub.execution.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.execution.dto.MyCaseResponse;
import com.company.casehub.execution.dto.MyProjectResponse;
import com.company.casehub.execution.service.MyTestQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MyTestController {

    private final MyTestQueryService service;

    public MyTestController(MyTestQueryService service) {
        this.service = service;
    }

    @GetMapping("/my-projects")
    @PreAuthorize("hasAuthority('project:read')")
    public List<MyProjectResponse> myProjects(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listMyProjects(principal);
    }

    @GetMapping("/my-cases")
    @PreAuthorize("hasAuthority('project_test_case:read')")
    public List<MyCaseResponse> myCases(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listMyCases(principal);
    }

    @GetMapping("/projects/{projectId}/cases")
    @PreAuthorize("hasAuthority('project_test_case:read')")
    public List<MyCaseResponse> projectCases(@PathVariable UUID projectId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return service.listProjectCases(projectId, principal);
    }

    @PostMapping("/project-test-cases/{projectTestCaseId}/viewed")
    @PreAuthorize("hasAuthority('project_test_case:read')")
    public MyCaseResponse viewed(@PathVariable UUID projectTestCaseId,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return service.markViewed(projectTestCaseId, principal);
    }
}
