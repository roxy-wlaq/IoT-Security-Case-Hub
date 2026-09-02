package com.company.casehub.capability.service;

import com.company.casehub.capability.dto.CapabilityResponse;
import com.company.casehub.capability.dto.CapabilityTreeNode;
import com.company.casehub.capability.dto.CreateCapabilityRequest;
import com.company.casehub.capability.dto.UpdateCapabilityRequest;
import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Capability Library (Database Schema V1.0 §10.1).
 *
 * <p>Owns the global Capability Tree: creation, renaming/re-parenting and retirement.
 * Capabilities are never physically deleted — historical references may still point at
 * them — so retirement means {@code enabled = false}.
 *
 * <p>The tree must stay acyclic. That invariant cannot be expressed as a table CHECK
 * (it spans an unbounded number of rows), so it is enforced here.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CapabilityService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityService.class);

    private final CapabilityRepository capabilityRepository;

    /**
     * {@code GET /api/v1/capabilities/tree} — the whole tree, nested.
     *
     * <p>Built from one flat query to avoid N+1. Nodes are only reachable through a
     * root ({@code parent_id IS NULL}) edge, so a corrupt cyclic component — which by
     * definition has no root — is never descended into and cannot cause unbounded
     * recursion here.
     */
    public List<CapabilityTreeNode> getTree() {
        List<CapabilityEntity> all = capabilityRepository.findAllByOrderBySortOrderAscNameAsc();

        Map<UUID, List<CapabilityEntity>> childrenByParent = new LinkedHashMap<>();
        List<CapabilityEntity> roots = new ArrayList<>();
        for (CapabilityEntity entity : all) {
            UUID parentId = entity.getParentId();
            if (parentId == null) {
                roots.add(entity);
            } else {
                childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(entity);
            }
        }

        List<CapabilityTreeNode> tree = new ArrayList<>(roots.size());
        for (CapabilityEntity root : roots) {
            tree.add(toNode(root, childrenByParent));
        }
        return tree;
    }

    /**
     * {@code POST /api/v1/capabilities}.
     *
     * <p>A brand new node has no descendants, so pointing it at an existing parent can
     * never close a cycle: only parent existence has to be checked.
     */
    @Transactional
    public CapabilityResponse create(CreateCapabilityRequest request) {
        String code = normalizeCode(request.code());

        if (capabilityRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(ErrorCode.CAPABILITY_CODE_DUPLICATE,
                    "Capability code '" + code + "' already exists.");
        }

        UUID parentId = request.parentId();
        if (parentId != null) {
            requireParentExists(parentId);
        }

        CapabilityEntity entity = new CapabilityEntity(
                parentId,
                code,
                normalizeName(request.name()),
                normalizeDescription(request.description()),
                sortOrderOrDefault(request.sortOrder()));

        return CapabilityResponse.of(capabilityRepository.save(entity));
    }

    /**
     * {@code PUT /api/v1/capabilities/{capabilityId}} — full replacement.
     */
    @Transactional
    public CapabilityResponse update(UUID capabilityId, UpdateCapabilityRequest request) {
        CapabilityEntity entity = requireCapability(capabilityId);

        UUID newParentId = request.parentId();
        if (newParentId != null) {
            requireParentExists(newParentId);
            assertAcyclic(capabilityId, newParentId);
        }

        String newCode = normalizeCode(request.code());
        if (capabilityRepository.existsByCodeIgnoreCaseAndIdNot(newCode, capabilityId)) {
            throw new ConflictException(ErrorCode.CAPABILITY_CODE_DUPLICATE,
                    "Capability code '" + newCode + "' already exists.");
        }

        entity.setParentId(newParentId);
        entity.setCode(newCode);
        entity.setName(normalizeName(request.name()));
        entity.setDescription(normalizeDescription(request.description()));
        entity.setSortOrder(sortOrderOrDefault(request.sortOrder()));

        return CapabilityResponse.of(capabilityRepository.save(entity));
    }

    /** {@code POST /api/v1/capabilities/{id}/enable}. */
    @Transactional
    public CapabilityResponse enable(UUID capabilityId) {
        return setEnabled(capabilityId, true);
    }

    /** {@code POST /api/v1/capabilities/{id}/disable}. */
    @Transactional
    public CapabilityResponse disable(UUID capabilityId) {
        return setEnabled(capabilityId, false);
    }

    /**
     * Retirement / reactivation never cascades: descendants keep their own
     * {@code enabled} flag so that re-enabling a parent does not resurrect subtrees a
     * librarian retired on purpose.
     */
    private CapabilityResponse setEnabled(UUID capabilityId, boolean enabled) {
        CapabilityEntity entity = requireCapability(capabilityId);
        entity.setEnabled(enabled);
        return CapabilityResponse.of(capabilityRepository.save(entity));
    }

    /**
     * Walks the {@code parent_id} chain upwards from the proposed parent and rejects
     * the move if the node being updated is part of that chain — that would make the
     * node its own ancestor.
     *
     * <p>Covers every cycle shape:
     * <ul>
     *   <li>self: {@code A.parent = A} — matched on the first iteration</li>
     *   <li>two-node: {@code A -> B} then {@code A.parent = B} — B's chain reaches A</li>
     *   <li>deep: {@code A -> B -> C} then {@code A.parent = C} — C's chain reaches A</li>
     * </ul>
     *
     * <p>{@code visited} is a safety net, not the rule: if the stored data already
     * contains a cycle (unreachable through this API, but possible after manual SQL
     * edits), the walk stops instead of spinning forever, and the check degrades to
     * "no new cycle involving this node was found".
     */
    private void assertAcyclic(UUID capabilityId, UUID proposedParentId) {
        Set<UUID> visited = new HashSet<>();
        UUID cursor = proposedParentId;

        while (cursor != null) {
            if (capabilityId.equals(cursor)) {
                throw new ConflictException(ErrorCode.CAPABILITY_CYCLE_DETECTED,
                        "Capability " + capabilityId + " cannot be moved under "
                                + proposedParentId + ": that would create a cycle.");
            }
            if (!visited.add(cursor)) {
                log.warn("Capability tree already contains a cycle at {}; aborting the cycle check for {}",
                        cursor, capabilityId);
                return;
            }
            cursor = capabilityRepository.findById(cursor)
                    .map(CapabilityEntity::getParentId)
                    .orElse(null);
        }
    }

    private CapabilityEntity requireCapability(UUID capabilityId) {
        return capabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CAPABILITY_NOT_FOUND,
                        "Capability " + capabilityId + " does not exist."));
    }

    private void requireParentExists(UUID parentId) {
        if (!capabilityRepository.existsById(parentId)) {
            throw new BusinessRuleException(ErrorCode.CAPABILITY_PARENT_INVALID,
                    "Parent capability " + parentId + " does not exist.");
        }
    }

    private static CapabilityTreeNode toNode(CapabilityEntity entity, Map<UUID, List<CapabilityEntity>> childrenByParent) {
        List<CapabilityEntity> children = childrenByParent.getOrDefault(entity.getId(), List.of());
        List<CapabilityTreeNode> childNodes = new ArrayList<>(children.size());
        for (CapabilityEntity child : children) {
            childNodes.add(toNode(child, childrenByParent));
        }
        return CapabilityTreeNode.of(entity, childNodes);
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }

    private static String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int sortOrderOrDefault(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}
