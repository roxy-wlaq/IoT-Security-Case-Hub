package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TestCaseVersionResponse(UUID id, UUID masterTestCaseId, String versionLabel, int versionMajor,
                                      int versionMinor, String status, boolean isCurrentVersion, String caseName,
                                      String testPurpose, String preconditions, String selectionMode,
                                      boolean evidenceRequired, String evidenceRequirement, String remarkRequirement,
                                      String progressiveRole, UUID basedOnVersionId, String changeReason, UUID createdBy,
                                      UUID reviewedBy, Instant publishedAt, Instant deprecatedAt, boolean revisionClosed,
                                      ReviewRecordAction latestReviewAction,
                                      List<TestStepResponse> steps, List<ToolRef> tools, List<StandardMappingRef> standardMappings,
                                      List<AttachmentRef> attachments, Instant createdAt, Instant updatedAt) {

    /**
     * Builds a version response without a known latest review action (defaults to null).
     * Use {@link #from(TestCaseVersionEntity, ReviewRecordAction)} when the latest review
     * record has been resolved, which is required for the version detail surface and for
     * deriving the UI "Rejected" label.
     */
    public static TestCaseVersionResponse from(TestCaseVersionEntity entity) {
        return from(entity, null);
    }

    public static TestCaseVersionResponse from(TestCaseVersionEntity entity, ReviewRecordAction latestReviewAction) {
        return new TestCaseVersionResponse(entity.getId(), entity.getMasterTestCase().getId(), VersionSummaryResponse.label(entity),
                entity.getVersionMajor(), entity.getVersionMinor(), entity.getStatus().name(), entity.isCurrentVersion(),
                entity.getCaseName(), entity.getTestPurpose(), entity.getPreconditions(), entity.getSelectionMode().name(),
                entity.isEvidenceRequired(), entity.getEvidenceRequirement(), entity.getRemarkRequirement(),
                entity.getProgressiveRole() == null ? null : entity.getProgressiveRole().name(),
                entity.getBasedOnVersion() == null ? null : entity.getBasedOnVersion().getId(), entity.getChangeReason(),
                entity.getCreatedBy().getId(), entity.getReviewedBy() == null ? null : entity.getReviewedBy().getId(),
                entity.getPublishedAt(), entity.getDeprecatedAt(), entity.isRevisionClosed(), latestReviewAction,
                entity.getSteps().stream().sorted(java.util.Comparator.comparingInt(s -> s.getSequenceNo())).map(TestStepResponse::from).toList(),
                entity.getTools().stream().sorted(java.util.Comparator.comparingInt(t -> t.getSortOrder())).map(t -> ToolRef.from(t.getTool())).toList(),
                entity.getStandardMappings().stream().map(StandardMappingRef::from).toList(),
                entity.getAttachments().stream().map(AttachmentRef::from).toList(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
