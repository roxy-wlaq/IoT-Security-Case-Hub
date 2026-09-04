package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.DecisionPointEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DecisionPointRepository extends JpaRepository<DecisionPointEntity, UUID> {
    @EntityGraph(attributePaths = {"transition", "transition.targets", "transition.targets.targetMasterTestCase", "transition.targets.targetCustomTestCase"})
    List<DecisionPointEntity> findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(UUID versionId);

    @EntityGraph(attributePaths = {"transition", "transition.targets", "transition.targets.targetMasterTestCase", "transition.targets.targetCustomTestCase"})
    List<DecisionPointEntity> findByCustomTestCaseIdOrderByDisplayOrderAscIdAsc(UUID customTestCaseId);

    @EntityGraph(attributePaths = {"testCaseVersion", "customTestCase", "transition", "transition.targets", "transition.targets.targetMasterTestCase", "transition.targets.targetCustomTestCase"})
    Optional<DecisionPointEntity> findByIdAndTestCaseVersionId(UUID id, UUID versionId);

    @EntityGraph(attributePaths = {"customTestCase", "transition", "transition.targets", "transition.targets.targetMasterTestCase", "transition.targets.targetCustomTestCase"})
    Optional<DecisionPointEntity> findByIdAndCustomTestCaseId(UUID id, UUID customCaseId);

    @EntityGraph(attributePaths = {"testCaseVersion", "customTestCase", "transition", "transition.targets", "transition.targets.targetMasterTestCase", "transition.targets.targetCustomTestCase"})
    @Query("select distinct point from DecisionPointEntity point")
    List<DecisionPointEntity> findAllWithGraphBy();
}
