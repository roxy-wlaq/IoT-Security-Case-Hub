package com.company.casehub.export.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Complete project-scoped metadata snapshot detached from the JPA persistence context. */
public record ProjectExportSnapshot(
        String projectNumber,
        String projectName,
        String deviceName,
        String generationMode,
        String status,
        String createdBy,
        Instant createdAt,
        String standards,
        List<ProjectExportRow> testCases,
        List<EvidenceRow> evidence) {

    public ProjectExportSnapshot {
        testCases = List.copyOf(testCases);
        evidence = List.copyOf(evidence);
    }

    public record EvidenceRow(
            UUID evidenceId,
            UUID projectTestCaseId,
            String originalFilename,
            String contentType,
            long fileSize,
            String sha256,
            String uploadedBy,
            Instant createdAt) {
    }
}
