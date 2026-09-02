package com.company.casehub.standard.service;

import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.standard.dto.StandardTaskTypeCreateRequest;
import com.company.casehub.standard.dto.StandardTaskTypeResponse;
import com.company.casehub.standard.dto.StandardTaskTypeUpdateRequest;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
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
 * Standard / Task Type dictionary service.
 *
 * <p>{@code code} uniqueness is case-insensitive (backed by
 * {@code uq_standard_task_types_code_lower}) and is enforced here instead of relying on
 * the database to raise a constraint violation.
 */
@Service
@Transactional(readOnly = true)
public class StandardTaskTypeService {

    private final StandardTaskTypeRepository repository;

    public StandardTaskTypeService(StandardTaskTypeRepository repository) {
        this.repository = repository;
    }

    public List<StandardTaskTypeResponse> list(String q, Boolean enabled, String type) {
        return repository.findAll(querySpec(q, enabled, type), Sort.by(Sort.Direction.ASC, "code"))
                .stream()
                .map(StandardTaskTypeResponse::from)
                .toList();
    }

    @Transactional
    public StandardTaskTypeResponse create(StandardTaskTypeCreateRequest request) {
        String code = request.code().trim();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(ErrorCode.STANDARD_CODE_DUPLICATE, "Standard/Task Type code already exists: " + code);
        }

        StandardTaskTypeEntity entity = new StandardTaskTypeEntity();
        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setType(request.type());
        entity.setDescription(normalizeDescription(request.description()));
        entity.setEnabled(request.enabled() == null || request.enabled());

        return StandardTaskTypeResponse.from(repository.save(entity));
    }

    @Transactional
    public StandardTaskTypeResponse update(UUID id, StandardTaskTypeUpdateRequest request) {
        StandardTaskTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STANDARD_NOT_FOUND, "Standard/Task Type not found: " + id));

        if (StringUtils.hasText(request.code())) {
            String code = request.code().trim();
            if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new ConflictException(ErrorCode.STANDARD_CODE_DUPLICATE, "Standard/Task Type code already exists: " + code);
            }
            entity.setCode(code);
        }
        if (StringUtils.hasText(request.name())) {
            entity.setName(request.name().trim());
        }
        if (StringUtils.hasText(request.type())) {
            entity.setType(request.type());
        }
        if (request.description() != null) {
            entity.setDescription(normalizeDescription(request.description()));
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }

        return StandardTaskTypeResponse.from(repository.save(entity));
    }

    private static String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    /**
     * Free-text filter over {@code name} / {@code code}. The query parameter is the
     * frozen contract's {@code q} (not {@code search}) — see
     * {@code GET /api/v1/standard-task-types?q=...}.
     */
    private static Specification<StandardTaskTypeEntity> querySpec(String q, Boolean enabled, String type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern)));
            }
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
