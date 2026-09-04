package com.company.casehub.category.repository;

import com.company.casehub.category.entity.CategoryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface CategoryRepository
        extends JpaRepository<CategoryEntity, UUID>, JpaSpecificationExecutor<CategoryEntity> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<CategoryEntity> findAllByOrderBySortOrderAscNameAsc();
}
