package com.company.casehub.export.dto;

import java.util.UUID;

/** Immutable, transaction-detached row for the Project Test Cases sheet. */
public record ProjectExportRow(
        UUID projectTestCaseId,
        String backingType,
        UUID masterTestCaseId,
        UUID customTestCaseId,
        String caseCode,
        String caseName,
        String planSources,
        UUID boundVersionId,
        String version,
        String executionStatus,
        String relationStatus,
        boolean removed,
        String assignees,
        long evidenceCount) {
}
