package com.company.casehub.user.repository;

import com.company.casehub.user.entity.RolePermissionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    List<RolePermissionEntity> findByRoleId(UUID roleId);
}
