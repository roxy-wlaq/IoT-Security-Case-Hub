package com.company.casehub.generation.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.generation.dto.GenerationRecommendationResponse;
import com.company.casehub.generation.dto.GenerationRunRequest;
import com.company.casehub.generation.dto.GenerationRunResponse;
import com.company.casehub.generation.dto.IgnoreRecommendationRequest;
import com.company.casehub.generation.service.GenerationRuntimeService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/generation")
public class GenerationController {

    private final GenerationRuntimeService runtimeService;

    public GenerationController(GenerationRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('generation:run')")
    public GenerationRunResponse run(@PathVariable UUID projectId, @RequestBody(required = false) GenerationRunRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return runtimeService.run(projectId, request, principal);
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAuthority('generation:run')")
    public List<GenerationRunResponse> runs(@PathVariable UUID projectId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return runtimeService.listRuns(projectId, principal);
    }

    @GetMapping("/runs/{runId}/recommendations")
    @PreAuthorize("hasAuthority('generation:run')")
    public List<GenerationRecommendationResponse> recommendations(@PathVariable UUID projectId,
                                                                    @PathVariable UUID runId,
                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return runtimeService.listRecommendations(runId, principal);
    }

    @PostMapping("/recommendations/{recommendationId}/add")
    @PreAuthorize("hasAuthority('generation:review_recommendation')")
    public GenerationRecommendationResponse add(@PathVariable UUID projectId, @PathVariable UUID recommendationId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return runtimeService.add(recommendationId, principal);
    }

    @PostMapping("/recommendations/{recommendationId}/ignore")
    @PreAuthorize("hasAuthority('generation:review_recommendation')")
    public GenerationRecommendationResponse ignore(@PathVariable UUID projectId, @PathVariable UUID recommendationId,
                                                    @RequestBody(required = false) IgnoreRecommendationRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return runtimeService.ignore(recommendationId, request == null || request.ignored(), principal);
    }
}
