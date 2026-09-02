package com.company.casehub.testcase.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TestCaseDetailResponse(UUID id, String caseCode, UUID categoryId, String categoryName, UUID createdBy,
                                     boolean enabled, Instant createdAt, Instant updatedAt, List<TagRef> tags,
                                     TestCaseVersionResponse currentVersion, TestCaseVersionResponse draftVersion,
                                     TestCaseVersionResponse visibleVersion, List<VersionSummaryResponse> versions,
                                     AllowedActions allowedActions) {
}
