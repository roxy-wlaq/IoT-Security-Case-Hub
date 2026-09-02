package com.company.casehub.capability.repository;

import com.company.casehub.capability.entity.CapabilityEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapabilityRepository extends JpaRepository<CapabilityEntity, UUID> {

    /**
     * Flat list used to assemble the tree in memory. Ordering by {@code sort_order}
     * then {@code name} keeps sibling order stable across requests.
     */
    List<CapabilityEntity> findAllByOrderBySortOrderAscNameAsc();

    /**
     * Case-insensitive code lookup. Backed by {@code uq_capabilities_code_lower}.
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Same as above but excluding one node, so a capability may keep its own code
     * when it is updated.
     */
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
