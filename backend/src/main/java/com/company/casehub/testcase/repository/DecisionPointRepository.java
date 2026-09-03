package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.DecisionPointEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DecisionPointRepository extends JpaRepository<DecisionPointEntity, UUID> {
    @EntityGraph(attributePaths = {"transition", "transition.targets", "transition.targets.targetMasterTestCase"})
    List<DecisionPointEntity> findByTestCaseVersionIdOrderByDisplayOrderAscIdAsc(UUID versionId);

    @EntityGraph(attributePaths = {"testCaseVersion", "transition", "transition.targets", "transition.targets.targetMasterTestCase"})
    Optional<DecisionPointEntity> findByIdAndTestCaseVersionId(UUID id, UUID versionId);

    @EntityGraph(attributePaths = {"testCaseVersion", "transition", "transition.targets", "transition.targets.targetMasterTestCase"})
    @Query("select distinct point from DecisionPointEntity point")
    List<DecisionPointEntity> findAllWithGraphBy();
}
