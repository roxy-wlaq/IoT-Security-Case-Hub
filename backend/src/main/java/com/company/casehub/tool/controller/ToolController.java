package com.company.casehub.tool.controller;

import com.company.casehub.tool.dto.ToolCreateRequest;
import com.company.casehub.tool.dto.ToolResponse;
import com.company.casehub.tool.dto.ToolUpdateRequest;
import com.company.casehub.tool.service.ToolService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tool metadata endpoints (frozen contract, Phase 4).
 *
 * <p>Reads are open to any authenticated user; writes require {@code tool:manage}.
 * Phase 4 intentionally exposes metadata only — no attachment endpoints.
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolService service;

    public ToolController(ToolService service) {
        this.service = service;
    }

    @GetMapping
    public List<ToolResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return service.list(search, enabled);
    }

    @GetMapping("/{id}")
    public ToolResponse get(@PathVariable("id") UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('tool:manage')")
    public ToolResponse create(@Valid @RequestBody ToolCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('tool:manage')")
    public ToolResponse update(@PathVariable("id") UUID id, @Valid @RequestBody ToolUpdateRequest request) {
        return service.update(id, request);
    }
}
