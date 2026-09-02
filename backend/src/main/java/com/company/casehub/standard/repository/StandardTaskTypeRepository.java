package com.company.casehub.standard.repository;

import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StandardTaskTypeRepository
        extends JpaRepository<StandardTaskTypeEntity, UUID>, JpaSpecificationExecutor<StandardTaskTypeEntity> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
