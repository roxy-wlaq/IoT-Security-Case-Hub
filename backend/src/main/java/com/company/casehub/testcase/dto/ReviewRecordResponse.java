package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.TestCaseReviewRecordEntity;
import java.time.Instant;
import java.util.UUID;

public record ReviewRecordResponse(UUID id, UUID testCaseVersionId, ReviewRecordAction action,
                                   UUID reviewerId, String reviewerName, String comment, Instant createdAt) {

    public static ReviewRecordResponse from(TestCaseReviewRecordEntity entity) {
        return new ReviewRecordResponse(entity.getId(), entity.getTestCaseVersion().getId(), entity.getAction(),
                entity.getReviewer().getId(), entity.getReviewer().getDisplayName(), entity.getComment(), entity.getCreatedAt());
    }
}
