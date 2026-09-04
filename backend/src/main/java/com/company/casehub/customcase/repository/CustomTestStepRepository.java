package com.company.casehub.customcase.repository;

import com.company.casehub.customcase.entity.CustomTestStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomTestStepRepository extends JpaRepository<CustomTestStepEntity, UUID> {
    List<CustomTestStepEntity> findByCustomTestCaseIdOrderBySequenceNoAsc(UUID customCaseId);
    void deleteByCustomTestCaseId(UUID customCaseId);
}
