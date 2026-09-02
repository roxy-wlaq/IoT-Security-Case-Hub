package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.RevisionContributorEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionContributorRepository extends JpaRepository<RevisionContributorEntity, UUID> {

    List<RevisionContributorEntity> findByTestCaseVersionIdOrderByCreatedAtAsc(UUID testCaseVersionId);

    boolean existsByTestCaseVersionIdAndUserId(UUID testCaseVersionId, UUID userId);

    Optional<RevisionContributorEntity> findByTestCaseVersionIdAndUserId(UUID testCaseVersionId, UUID userId);

    void deleteByTestCaseVersionIdAndUserId(UUID testCaseVersionId, UUID userId);
}
