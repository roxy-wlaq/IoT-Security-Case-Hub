package com.company.casehub.evidence.repository;

import com.company.casehub.evidence.entity.NoteEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<NoteEntity, UUID> {
    List<NoteEntity> findByProjectTestCaseIdOrderByCreatedAtAsc(UUID projectTestCaseId);
}
