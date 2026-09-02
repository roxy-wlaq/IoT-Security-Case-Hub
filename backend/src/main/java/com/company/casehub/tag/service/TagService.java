package com.company.casehub.tag.service;

import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.tag.dto.TagCreateRequest;
import com.company.casehub.tag.dto.TagResponse;
import com.company.casehub.tag.dto.TagUpdateRequest;
import com.company.casehub.tag.entity.TagEntity;
import com.company.casehub.tag.repository.TagRepository;
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
 * Tag dictionary service. {@code code} and {@code name} are both unique
 * case-insensitively; each has its own frozen conflict error code so the client can
 * point the user at the offending field.
 */
@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository repository;

    public TagService(TagRepository repository) {
        this.repository = repository;
    }

    public List<TagResponse> list(String search, Boolean enabled) {
        return repository.findAll(searchSpec(search, enabled), Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(TagResponse::from)
                .toList();
    }

    @Transactional
    public TagResponse create(TagCreateRequest request) {
        String code = request.code().trim();
        String name = request.name().trim();

        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(ErrorCode.TAG_CODE_DUPLICATE, "Tag code already exists: " + code);
        }
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(ErrorCode.TAG_NAME_DUPLICATE, "Tag name already exists: " + name);
        }

        TagEntity entity = new TagEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(normalizeDescription(request.description()));
        entity.setEnabled(request.enabled() == null || request.enabled());

        return TagResponse.from(repository.save(entity));
    }

    @Transactional
    public TagResponse update(UUID id, TagUpdateRequest request) {
        TagEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TAG_NOT_FOUND, "Tag not found: " + id));

        if (StringUtils.hasText(request.code())) {
            String code = request.code().trim();
            if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new ConflictException(ErrorCode.TAG_CODE_DUPLICATE, "Tag code already exists: " + code);
            }
            entity.setCode(code);
        }
        if (StringUtils.hasText(request.name())) {
            String name = request.name().trim();
            if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new ConflictException(ErrorCode.TAG_NAME_DUPLICATE, "Tag name already exists: " + name);
            }
            entity.setName(name);
        }
        if (request.description() != null) {
            entity.setDescription(normalizeDescription(request.description()));
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }

        return TagResponse.from(repository.save(entity));
    }

    private static String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    private static Specification<TagEntity> searchSpec(String search, Boolean enabled) {
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
