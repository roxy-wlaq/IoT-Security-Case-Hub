package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.TestCaseReviewRecordEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseReviewRecordRepository extends JpaRepository<TestCaseReviewRecordEntity, UUID> {

    List<TestCaseReviewRecordEntity> findByTestCaseVersionIdOrderByCreatedAtAscIdAsc(UUID testCaseVersionId);

    Optional<TestCaseReviewRecordEntity> findFirstByTestCaseVersionIdOrderByCreatedAtDescIdDesc(UUID testCaseVersionId);

    long countByTestCaseVersionIdAndAction(UUID testCaseVersionId, ReviewRecordAction action);
}
