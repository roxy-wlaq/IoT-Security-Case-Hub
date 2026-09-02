package com.company.casehub.standard.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard / Task Type dictionary entry (Database Schema V1.0 §8.1).
 *
 * <p>{@code type} is stored as VARCHAR because the DB carries a CHECK constraint on the
 * literal values; the request DTO validates it with Bean Validation, so an illegal value
 * never reaches the persistence layer.
 */
@Entity
@Table(name = "standard_task_types", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class StandardTaskTypeEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
