package com.company.casehub.tag.controller;

import com.company.casehub.tag.dto.TagCreateRequest;
import com.company.casehub.tag.dto.TagResponse;
import com.company.casehub.tag.dto.TagUpdateRequest;
import com.company.casehub.tag.service.TagService;
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
 * Tag dictionary endpoints (frozen contract, Phase 4).
 *
 * <p>Reads are open to any authenticated user; writes require {@code tag:manage}.
 */
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    @GetMapping
    public List<TagResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return service.list(search, enabled);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('tag:manage')")
    public TagResponse create(@Valid @RequestBody TagCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('tag:manage')")
    public TagResponse update(@PathVariable("id") UUID id, @Valid @RequestBody TagUpdateRequest request) {
        return service.update(id, request);
    }
}
