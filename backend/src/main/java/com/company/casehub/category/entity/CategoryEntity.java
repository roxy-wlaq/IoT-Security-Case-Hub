package com.company.casehub.category.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Category dictionary entry (Database Schema V1.0 §9.1).
 *
 * <p>The hierarchy is capped at two levels: {@code parentId == null} implies
 * {@code level = 1}, otherwise {@code level = 2}. {@code level} is always derived
 * server-side from the parent — it is never accepted from a client payload.
 */
@Entity
@Table(name = "categories", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class CategoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_categories_parent"))
    private CategoryEntity parent;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
