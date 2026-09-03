package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.project.entity.ProjectEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectTestCaseRepository extends JpaRepository<ProjectTestCaseEntity, UUID> {

    Optional<ProjectTestCaseEntity> findByProjectIdAndMasterTestCaseId(UUID projectId, UUID masterTestCaseId);

    List<ProjectTestCaseEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    @Query("select distinct p.project from ProjectTestCaseEntity p join p.assignees a where a.user.id = :userId order by p.project.createdAt desc")
    List<ProjectEntity> findProjectsAssignedTo(@Param("userId") UUID userId);

    @Query("select count(p) > 0 from ProjectTestCaseEntity p join p.assignees a where p.project.id = :projectId and a.user.id = :userId")
    boolean existsAssignmentInProject(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProjectTestCaseEntity p where p.project.id = :projectId and p.masterTestCase.id = :masterId")
    Optional<ProjectTestCaseEntity> findForUpdate(@Param("projectId") UUID projectId, @Param("masterId") UUID masterId);
}
