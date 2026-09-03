package com.company.casehub.project.repository;

import com.company.casehub.project.entity.ProjectEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    List<ProjectEntity> findAllByOrderByCreatedAtDesc();

    @Query("select distinct p from ProjectEntity p join p.coordinators c where c.user.id = :userId order by p.createdAt desc")
    List<ProjectEntity> findAllVisibleToCoordinator(@Param("userId") UUID userId);

    boolean existsByProjectNumber(String projectNumber);
}
