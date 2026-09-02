package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import java.time.Instant;
import java.util.UUID;

public record VersionSummaryResponse(UUID id, String versionLabel, int versionMajor, int versionMinor, String status,
                                     boolean isCurrentVersion, String changeReason, UUID createdBy,
                                     Instant publishedAt, Instant createdAt) {
    public static VersionSummaryResponse from(TestCaseVersionEntity entity) {
        return new VersionSummaryResponse(entity.getId(), label(entity), entity.getVersionMajor(), entity.getVersionMinor(),
                entity.getStatus().name(), entity.isCurrentVersion(), entity.getChangeReason(), entity.getCreatedBy().getId(),
                entity.getPublishedAt(), entity.getCreatedAt());
    }

    public static String label(TestCaseVersionEntity entity) {
        return entity.getVersionMajor() + "." + entity.getVersionMinor();
    }
}
