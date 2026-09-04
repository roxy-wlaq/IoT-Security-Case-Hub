package com.company.casehub.upgrade.dto;

import com.company.casehub.execution.entity.ExecutionStatus;
import java.util.UUID;

public record VersionAvailabilityResponse(UUID projectTestCaseId, UUID masterTestCaseId, UUID boundVersionId,
                                          UUID currentPublishedVersionId, boolean newVersionAvailable,
                                          ExecutionStatus executionStatus, VersionDiffResponse diff) { }
