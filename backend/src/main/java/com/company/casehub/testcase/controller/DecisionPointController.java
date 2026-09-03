package com.company.casehub.testcase.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.DecisionPointRequest;
import com.company.casehub.testcase.dto.DecisionPointResponse;
import com.company.casehub.testcase.dto.MasterLogicGraphResponse;
import com.company.casehub.testcase.service.DecisionPointService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-cases/{masterId}/versions/{versionId}")
public class DecisionPointController {

    private final DecisionPointService service;

    public DecisionPointController(DecisionPointService service) {
        this.service = service;
    }

    @GetMapping("/decision-points")
    public List<DecisionPointResponse> list(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return service.list(masterId, versionId, principal);
    }

    @GetMapping("/logic-graph")
    public MasterLogicGraphResponse graph(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return service.graph(masterId, versionId, principal);
    }

    @PostMapping("/decision-points")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('test_case:draft_edit') or @testCaseAccessPolicy.canEditDraftVersionById(#masterId, #versionId, principal)")
    public DecisionPointResponse create(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                        @Valid @RequestBody DecisionPointRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return service.create(masterId, versionId, request, principal);
    }

    @PutMapping("/decision-points/{pointId}")
    @PreAuthorize("hasAuthority('test_case:draft_edit') or @testCaseAccessPolicy.canEditDraftVersionById(#masterId, #versionId, principal)")
    public DecisionPointResponse update(@PathVariable UUID masterId, @PathVariable UUID versionId,
                                        @PathVariable UUID pointId, @Valid @RequestBody DecisionPointRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return service.update(masterId, versionId, pointId, request, principal);
    }

    @DeleteMapping("/decision-points/{pointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('test_case:draft_edit') or @testCaseAccessPolicy.canEditDraftVersionById(#masterId, #versionId, principal)")
    public void delete(@PathVariable UUID masterId, @PathVariable UUID versionId, @PathVariable UUID pointId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(masterId, versionId, pointId, principal);
    }
}
