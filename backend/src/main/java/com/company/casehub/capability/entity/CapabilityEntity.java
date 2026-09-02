package com.company.casehub.capability.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Capability Library node (Database Schema V1.0 §10.1).
 *
 * <p>A capability answers <em>"what capability does the device have"</em>. It is a node
 * of the global Capability Tree and carries no security conclusion: the
 * {@code YES / NO / UNKNOWN} state belongs to Project Capability (later phase) and
 * is deliberately absent here.
 *
 * <p>The tree is modelled as a plain {@code parent_id} column rather than a
 * {@code @ManyToOne} association: cycle detection walks the chain id-by-id through the
 * repository, and a mapped association would force the walk to trigger lazy loads
 * (N+1) or require fetch joins for no benefit. Referential integrity is enforced by
 * the {@code ON DELETE RESTRICT} foreign key in V004.
 */
@Entity
@Table(name = "capabilities", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class CapabilityEntity extends BaseEntity {

    /** {@code null} marks a root capability. */
    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "code", nullable = false, length = 120)
    private String code;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public CapabilityEntity(UUID parentId, String code, String name, String description, int sortOrder) {
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
