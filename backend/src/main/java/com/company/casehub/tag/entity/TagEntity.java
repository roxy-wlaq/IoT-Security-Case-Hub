package com.company.casehub.tag.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tag dictionary entry (Database Schema V1.0 §9.2 plus the user-mandated {@code code}).
 *
 * <p>Both {@code code} and {@code name} are unique case-insensitively
 * ({@code uq_tags_code_lower} / {@code uq_tags_name_lower}).
 */
@Entity
@Table(name = "tags", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class TagEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
