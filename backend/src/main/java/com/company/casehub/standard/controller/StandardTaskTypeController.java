package com.company.casehub.standard.controller;

import com.company.casehub.standard.dto.StandardTaskTypeCreateRequest;
import com.company.casehub.standard.dto.StandardTaskTypeResponse;
import com.company.casehub.standard.dto.StandardTaskTypeUpdateRequest;
import com.company.casehub.standard.service.StandardTaskTypeService;
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
 * Standard / Task Type dictionary endpoints (frozen contract, Phase 4).
 *
 * <p>Reads are open to any authenticated user ({@code SecurityConfig} already requires
 * authentication for every request); writes require {@code standard:manage}.
 */
@RestController
@RequestMapping("/api/v1/standard-task-types")
public class StandardTaskTypeController {

    private final StandardTaskTypeService service;

    public StandardTaskTypeController(StandardTaskTypeService service) {
        this.service = service;
    }

    @GetMapping
    public List<StandardTaskTypeResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "type", required = false) String type) {
        return service.list(search, enabled, type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('standard:manage')")
    public StandardTaskTypeResponse create(@Valid @RequestBody StandardTaskTypeCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('standard:manage')")
    public StandardTaskTypeResponse update(@PathVariable("id") UUID id,
                                           @Valid @RequestBody StandardTaskTypeUpdateRequest request) {
        return service.update(id, request);
    }
}
