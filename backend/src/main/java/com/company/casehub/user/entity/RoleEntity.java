package com.company.casehub.user.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles", schema = "casehub",
        uniqueConstraints = @UniqueConstraint(name = "uq_roles_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
public class RoleEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    public RoleEntity(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
