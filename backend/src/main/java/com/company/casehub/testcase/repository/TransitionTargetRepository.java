package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TransitionTargetEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransitionTargetRepository extends JpaRepository<TransitionTargetEntity, UUID> {
}
