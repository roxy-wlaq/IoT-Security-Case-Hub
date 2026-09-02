package com.company.casehub.tool.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tool metadata (Database Schema V1.0 §9.3 plus the user-mandated {@code code}).
 *
 * <p>Phase 4 covers tool metadata CRUD only — tool attachments belong to a later wave.
 */
@Entity
@Table(name = "tools", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class ToolEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "platform", length = 100)
    private String platform;

    @Column(name = "website")
    private String website;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
