package com.company.casehub.generation.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.generation.dto.GenerationRuleRequest;
import com.company.casehub.generation.dto.GenerationRuleResponse;
import com.company.casehub.generation.service.GenerationRuleService;
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
@RequestMapping("/api/v1/generation-rules")
public class GenerationRuleController {

    private final GenerationRuleService service;

    public GenerationRuleController(GenerationRuleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('generation_rule:read')")
    public List<GenerationRuleResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('generation_rule:read')")
    public GenerationRuleResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('generation_rule:manage')")
    public GenerationRuleResponse create(@Valid @RequestBody GenerationRuleRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return service.create(request, principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('generation_rule:manage')")
    public GenerationRuleResponse update(@PathVariable UUID id, @Valid @RequestBody GenerationRuleRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return service.update(id, request, principal);
    }
}
