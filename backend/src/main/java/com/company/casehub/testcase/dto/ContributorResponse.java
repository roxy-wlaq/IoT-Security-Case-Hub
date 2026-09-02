package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.RevisionContributorEntity;
import java.time.Instant;
import java.util.UUID;

public record ContributorResponse(UUID id, UUID userId, String username, String displayName, UUID addedBy, Instant createdAt) {

    public static ContributorResponse from(RevisionContributorEntity entity) {
        return new ContributorResponse(entity.getId(), entity.getUser().getId(), entity.getUser().getUsername(),
                entity.getUser().getDisplayName(), entity.getAddedBy().getId(), entity.getCreatedAt());
    }
}
