package com.company.casehub.category.controller;

import com.company.casehub.category.dto.CategoryCreateRequest;
import com.company.casehub.category.dto.CategoryResponse;
import com.company.casehub.category.dto.CategoryUpdateRequest;
import com.company.casehub.category.service.CategoryService;
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
 * Category dictionary endpoints (frozen contract, Phase 4).
 *
 * <p>Reads are open to any authenticated user; writes require {@code category:manage}.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping("/tree")
    public List<CategoryResponse> tree(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return service.tree(search, enabled);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('category:manage')")
    public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('category:manage')")
    public CategoryResponse update(@PathVariable("id") UUID id,
                                   @Valid @RequestBody CategoryUpdateRequest request) {
        return service.update(id, request);
    }
}
