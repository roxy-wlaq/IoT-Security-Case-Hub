package com.company.casehub.tool.service;

import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.tool.dto.ToolCreateRequest;
import com.company.casehub.tool.dto.ToolResponse;
import com.company.casehub.tool.dto.ToolUpdateRequest;
import com.company.casehub.tool.entity.ToolEntity;
import com.company.casehub.tool.repository.ToolRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Tool metadata service. {@code code} and {@code name} are both unique
 * case-insensitively; each has its own frozen conflict error code.
 *
 * <p>Phase 4 deliberately stops at metadata CRUD — no attachment upload.
 */
@Service
@Transactional(readOnly = true)
public class ToolService {

    private final ToolRepository repository;

    public ToolService(ToolRepository repository) {
        this.repository = repository;
    }

    public List<ToolResponse> list(String search, Boolean enabled) {
        return repository.findAll(searchSpec(search, enabled), Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(ToolResponse::from)
                .toList();
    }

    public ToolResponse get(UUID id) {
        ToolEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TOOL_NOT_FOUND, "Tool not found: " + id));
        return ToolResponse.from(entity);
    }

    @Transactional
    public ToolResponse create(ToolCreateRequest request) {
        String code = request.code().trim();
        String name = request.name().trim();

        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(ErrorCode.TOOL_CODE_DUPLICATE, "Tool code already exists: " + code);
        }
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(ErrorCode.TOOL_NAME_DUPLICATE, "Tool name already exists: " + name);
        }

        ToolEntity entity = new ToolEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(normalizeDescription(request.description()));
        entity.setPlatform(normalizeDescription(request.platform()));
        entity.setWebsite(normalizeDescription(request.website()));
        entity.setEnabled(request.enabled() == null || request.enabled());

        return ToolResponse.from(repository.save(entity));
    }

    @Transactional
    public ToolResponse update(UUID id, ToolUpdateRequest request) {
        ToolEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TOOL_NOT_FOUND, "Tool not found: " + id));

        if (StringUtils.hasText(request.code())) {
            String code = request.code().trim();
            if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new ConflictException(ErrorCode.TOOL_CODE_DUPLICATE, "Tool code already exists: " + code);
            }
            entity.setCode(code);
        }
        if (StringUtils.hasText(request.name())) {
            String name = request.name().trim();
            if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new ConflictException(ErrorCode.TOOL_NAME_DUPLICATE, "Tool name already exists: " + name);
            }
            entity.setName(name);
        }
        if (request.description() != null) {
            entity.setDescription(normalizeDescription(request.description()));
        }
        if (request.platform() != null) {
            entity.setPlatform(normalizeDescription(request.platform()));
        }
        if (request.website() != null) {
            entity.setWebsite(normalizeDescription(request.website()));
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }

        return ToolResponse.from(repository.save(entity));
    }

    private static String normalizeDescription(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static Specification<ToolEntity> searchSpec(String search, Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern)));
            }
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
