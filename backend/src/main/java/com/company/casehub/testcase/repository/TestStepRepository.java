package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TestStepEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestStepRepository extends JpaRepository<TestStepEntity, UUID> {
}
