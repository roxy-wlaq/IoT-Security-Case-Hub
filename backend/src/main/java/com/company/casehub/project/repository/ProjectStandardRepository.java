package com.company.casehub.project.repository;

import com.company.casehub.project.entity.ProjectStandardEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStandardRepository extends JpaRepository<ProjectStandardEntity, UUID> {
}
