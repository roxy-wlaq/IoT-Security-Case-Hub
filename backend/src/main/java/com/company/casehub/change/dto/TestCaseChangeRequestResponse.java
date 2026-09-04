package com.company.casehub.change.dto;

import com.company.casehub.change.entity.TestCaseChangeRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record TestCaseChangeRequestResponse(UUID id, UUID masterTestCaseId, UUID sourceVersionId,
                                            String reason, UUID submittedBy, UUID reviewedBy,
                                            UUID revisionDraftVersionId, TestCaseChangeRequestStatus status,
                                            Instant createdAt, Instant updatedAt) { }
