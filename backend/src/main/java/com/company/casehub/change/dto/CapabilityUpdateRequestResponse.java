package com.company.casehub.change.dto;

import com.company.casehub.change.entity.CapabilityUpdateRequestStatus;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import java.time.Instant;
import java.util.UUID;

public record CapabilityUpdateRequestResponse(UUID id, UUID projectId, UUID capabilityId,
                                              ProjectCapabilityValue currentValue, ProjectCapabilityValue proposedValue,
                                              String reason, String evidenceReference, UUID submittedBy, UUID reviewedBy,
                                              CapabilityUpdateRequestStatus status, Instant createdAt, Instant updatedAt) { }
