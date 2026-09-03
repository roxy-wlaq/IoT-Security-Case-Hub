package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.project.entity.ProjectEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProjectTestCaseEntity p where p.id = :id")
    Optional<ProjectTestCaseEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            INSERT INTO casehub.project_test_cases
              (id, project_id, master_test_case_id, test_case_version_id, execution_status,
               relation_status, is_root, removed, created_by, last_modified_by, last_modified_at)
            VALUES (:id, :projectId, :masterId, :versionId, 'NOT_STARTED', 'FLOATING', false, false,
                    :actorId, :actorId, CURRENT_TIMESTAMP)
            ON CONFLICT (project_id, master_test_case_id) DO NOTHING
            """, nativeQuery = true)
    int insertRuntimeTargetIfAbsent(@Param("id") UUID id, @Param("projectId") UUID projectId,
                                    @Param("masterId") UUID masterId, @Param("versionId") UUID versionId,
                                    @Param("actorId") UUID actorId);
}
