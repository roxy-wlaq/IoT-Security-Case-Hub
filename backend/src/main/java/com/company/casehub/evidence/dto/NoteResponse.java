package com.company.casehub.evidence.dto;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(UUID id, UUID projectTestCaseId, UUID authorId,
                           String authorName, String body, Instant createdAt, Instant updatedAt,
                           boolean editable) {
}
