package com.company.casehub.user.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_permissions", schema = "casehub",
        uniqueConstraints = @UniqueConstraint(name = "uq_role_permissions", columnNames = {"role_id", "permission_id"}),
        indexes = {
                @Index(name = "ix_role_permissions_role", columnList = "role_id"),
                @Index(name = "ix_role_permissions_permission", columnList = "permission_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class RolePermissionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_role_permissions_role"))
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_role_permissions_permission"))
    private PermissionEntity permission;

    public RolePermissionEntity(RoleEntity role, PermissionEntity permission) {
        this.role = role;
        this.permission = permission;
    }
}
