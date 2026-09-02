package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TestCaseAttachmentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseAttachmentRepository extends JpaRepository<TestCaseAttachmentEntity, UUID> {
}
