package com.company.casehub.category.service;

import com.company.casehub.category.dto.CategoryCreateRequest;
import com.company.casehub.category.dto.CategoryResponse;
import com.company.casehub.category.dto.CategoryUpdateRequest;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Category dictionary service (Database Schema V1.0 §9.1).
 *
 * <p>The hierarchy is capped at two levels and {@code level} is <em>always</em> derived on
 * the server from {@code parentId}: {@code null} parent ⇒ level 1, a level-1 parent ⇒
 * level 2. A client-supplied level is never accepted, and a level-2 category can never
 * become a parent, so a third level is structurally impossible.
 *
 * <p>Every rule is validated here before the row is written; the database CHECK
 * constraints ({@code chk_categories_level}, {@code chk_categories_parent_consistency})
 * are only a last line of defence, never the primary guard.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Comparator<CategoryEntity> BY_SORT_ORDER_AND_NAME =
            Comparator.comparingInt(CategoryEntity::getSortOrder).thenComparing(CategoryEntity::getName);

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the two-level category tree. {@code search} matches name or code
     * case-insensitively; a level-2 node is returned nested under its level-1 parent even
     * when the parent itself does not match the filter.
     */
    public List<CategoryResponse> tree(String search, Boolean enabled) {
        List<CategoryEntity> matched =
                repository.findAll(searchSpec(search, enabled), Sort.by(Sort.Direction.ASC, "sortOrder", "name"));

        Set<UUID> matchedIds = new HashSet<>();
        List<CategoryEntity> roots = new ArrayList<>();
        Map<UUID, List<CategoryEntity>> childrenByParent = new LinkedHashMap<>();
        for (CategoryEntity category : matched) {
            matchedIds.add(category.getId());
            CategoryEntity parent = category.getParent();
            if (parent == null) {
                roots.add(category);
            } else {
                childrenByParent.computeIfAbsent(parent.getId(), key -> new ArrayList<>()).add(category);
            }
        }

        // A level-2 node can match while its level-1 parent does not (for example searching a
        // child name while filtering enabled=true on a disabled parent). Load those parents so
        // the tree keeps its structure instead of silently dropping the matched children.
        Set<UUID> missingParentIds = new HashSet<>();
        for (UUID parentId : childrenByParent.keySet()) {
            if (!matchedIds.contains(parentId)) {
                missingParentIds.add(parentId);
            }
        }
        if (!missingParentIds.isEmpty()) {
            roots.addAll(repository.findAllById(missingParentIds));
        }

        roots.sort(BY_SORT_ORDER_AND_NAME);

        List<CategoryResponse> tree = new ArrayList<>(roots.size());
        for (CategoryEntity root : roots) {
            List<CategoryResponse> children = childrenByParent.getOrDefault(root.getId(), List.of())
                    .stream()
                    .map(child -> CategoryResponse.from(child, List.of()))
                    .toList();
            tree.add(CategoryResponse.from(root, children));
        }
        return tree;
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        String code = request.code().trim();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(ErrorCode.CATEGORY_CODE_DUPLICATE, "Category code already exists: " + code);
        }

        CategoryEntity parent = resolveParent(request.parentId(), null);

        CategoryEntity entity = new CategoryEntity();
        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setParent(parent);
        entity.setLevel(parent == null ? 1 : 2);
        entity.setDescription(normalizeDescription(request.description()));
        entity.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        entity.setEnabled(request.enabled() == null || request.enabled());

        return CategoryResponse.from(repository.save(entity));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
        CategoryEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: " + id));

        if (StringUtils.hasText(request.code())) {
            String code = request.code().trim();
            if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new ConflictException(ErrorCode.CATEGORY_CODE_DUPLICATE, "Category code already exists: " + code);
            }
            entity.setCode(code);
        }
        if (StringUtils.hasText(request.name())) {
            entity.setName(request.name().trim());
        }
        if (request.description() != null) {
            entity.setDescription(normalizeDescription(request.description()));
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.parentId() != null) {
            CategoryEntity parent = resolveParent(request.parentId(), id);
            entity.setParent(parent);
            entity.setLevel(parent == null ? 1 : 2);
        }

        return CategoryResponse.from(repository.save(entity));
    }

    /**
     * Validates a proposed parent and loads it.
     *
     * @param parentId the requested parent id, or {@code null} for a level-1 category
     * @param selfId   the id of the node being updated, or {@code null} on create
     * @return the parent entity (level 1), or {@code null} when the category is a root
     */
    private CategoryEntity resolveParent(UUID parentId, UUID selfId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessRuleException(
                    ErrorCode.CATEGORY_PARENT_INVALID, "A category cannot be its own parent.");
        }

        CategoryEntity parent = repository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND, "Parent category not found: " + parentId));

        // Only level-1 categories may be parents — this is what makes a third level impossible.
        if (parent.getLevel() != 1) {
            throw new BusinessRuleException(
                    ErrorCode.CATEGORY_PARENT_INVALID,
                    "Parent category must be a level-1 category; category " + parentId + " is level " + parent.getLevel() + ".");
        }
        if (selfId != null && isSelfAncestorOf(parent, selfId)) {
            throw new BusinessRuleException(
                    ErrorCode.CATEGORY_PARENT_INVALID,
                    "A category cannot be moved under one of its own descendants.");
        }
        return parent;
    }

    /**
     * Walks the ancestor chain of {@code proposedParent} looking for {@code selfId}.
     * The visited set stops the walk if a cycle ever slips past the service rules.
     */
    private static boolean isSelfAncestorOf(CategoryEntity proposedParent, UUID selfId) {
        Set<UUID> visited = new HashSet<>();
        CategoryEntity current = proposedParent;
        while (current != null && visited.add(current.getId())) {
            if (selfId.equals(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    private static Specification<CategoryEntity> searchSpec(String search, Boolean enabled) {
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
