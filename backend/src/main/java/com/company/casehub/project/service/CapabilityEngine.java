package com.company.casehub.project.service;

import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.project.entity.ProjectCapabilityEntity;
import com.company.casehub.project.entity.ProjectCapabilitySource;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import com.company.casehub.project.repository.ProjectCapabilityRepository;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.user.entity.UserEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapabilityEngine {

    private final CapabilityRepository capabilityRepository;
    private final ProjectCapabilityRepository projectCapabilityRepository;
    private final ProjectRepository projectRepository;

    public CapabilityEngine(CapabilityRepository capabilityRepository,
                            ProjectCapabilityRepository projectCapabilityRepository,
                            ProjectRepository projectRepository) {
        this.capabilityRepository = capabilityRepository;
        this.projectCapabilityRepository = projectCapabilityRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public EffectiveCapability resolveEffectiveValue(UUID projectId, UUID capabilityId) {
        Map<UUID, ProjectCapabilityEntity> values = new HashMap<>();
        projectCapabilityRepository.findByProjectId(projectId)
                .forEach(row -> values.put(row.getCapability().getId(), row));
        CapabilityEntity capability = capabilityRepository.findById(capabilityId).orElse(null);
        if (capability == null) {
            return new EffectiveCapability(ProjectCapabilityValue.UNKNOWN, true, false);
        }
        if (hasNoAncestor(values, capability)) {
            return new EffectiveCapability(ProjectCapabilityValue.UNKNOWN, false, false);
        }
        ProjectCapabilityEntity row = values.get(capabilityId);
        return row == null
                ? new EffectiveCapability(ProjectCapabilityValue.UNKNOWN, true, false)
                : new EffectiveCapability(row.getValue(), true, row.isDerived());
    }

    @Transactional
    public void recalculateDerivedParents(UUID projectId, UUID ignoredCapabilityId, UserEntity actor) {
        List<CapabilityEntity> capabilities = capabilityRepository.findAllByOrderBySortOrderAscNameAsc();
        Map<UUID, ProjectCapabilityEntity> current = new HashMap<>();
        projectCapabilityRepository.findByProjectId(projectId)
                .forEach(row -> current.put(row.getCapability().getId(), row));
        Set<UUID> requiredDerived = new HashSet<>();
        for (ProjectCapabilityEntity row : current.values()) {
            if (row.isDerived() || row.getValue() != ProjectCapabilityValue.YES) {
                continue;
            }
            CapabilityEntity node = row.getCapability();
            while (node.getParentId() != null) {
                requiredDerived.add(node.getParentId());
                UUID parentId = node.getParentId();
                node = capabilities.stream().filter(candidate -> candidate.getId().equals(parentId)).findFirst().orElse(null);
                if (node == null) {
                    break;
                }
            }
        }
        for (UUID parentId : requiredDerived) {
            ProjectCapabilityEntity row = current.get(parentId);
            if (row == null) {
                CapabilityEntity parent = capabilityRepository.findById(parentId).orElseThrow();
                row = new ProjectCapabilityEntity();
                row.setProject(projectRepository.findById(projectId).orElseThrow());
                row.setCapability(parent);
                row.setValue(ProjectCapabilityValue.YES);
                row.setSource(ProjectCapabilitySource.DERIVED);
                row.setDerived(true);
                row.setUpdatedBy(actor);
                current.put(parentId, row);
            } else if (row.isDerived()) {
                row.setValue(ProjectCapabilityValue.YES);
                row.setSource(ProjectCapabilitySource.DERIVED);
            }
            projectCapabilityRepository.save(row);
        }
        current.values().stream()
                .filter(ProjectCapabilityEntity::isDerived)
                .filter(row -> !requiredDerived.contains(row.getCapability().getId()))
                .forEach(projectCapabilityRepository::delete);
    }

    private boolean hasNoAncestor(Map<UUID, ProjectCapabilityEntity> values, CapabilityEntity node) {
        CapabilityEntity current = node;
        Set<UUID> visited = new HashSet<>();
        while (current != null && current.getParentId() != null && visited.add(current.getId())) {
            ProjectCapabilityEntity parent = values.get(current.getParentId());
            if (parent != null && parent.getValue() == ProjectCapabilityValue.NO && !parent.isDerived()) {
                return true;
            }
            UUID parentId = current.getParentId();
            current = capabilityRepository.findById(parentId).orElse(null);
        }
        return false;
    }

    public record EffectiveCapability(ProjectCapabilityValue value, boolean applicable, boolean derived) {
    }
}
