package com.company.casehub.evidence.dto;

import java.time.Instant;
import java.util.UUID;

public record EvidenceResponse(UUID id, UUID projectTestCaseId, String originalFilename,
                               long fileSize, String contentType, String sha256,
                               UUID uploadedBy, Instant createdAt) {
}
