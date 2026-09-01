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
@Table(name = "permissions", schema = "casehub",
        uniqueConstraints = @UniqueConstraint(name = "uq_permissions_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
public class PermissionEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    public PermissionEntity(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
