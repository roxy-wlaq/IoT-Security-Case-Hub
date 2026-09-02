package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TestCaseSummaryResponse(UUID id, String caseCode, String caseName, UUID categoryId, String categoryName,
                                      String status, int versionMajor, int versionMinor, String versionLabel,
                                      List<TagRef> tags, boolean enabled, Instant updatedAt) {
    public static TestCaseSummaryResponse from(MasterTestCaseEntity master, TestCaseVersionEntity version) {
        return new TestCaseSummaryResponse(master.getId(), master.getCaseCode(), version.getCaseName(), master.getCategory().getId(),
                master.getCategory().getName(), version.getStatus().name(), version.getVersionMajor(), version.getVersionMinor(),
                VersionSummaryResponse.label(version), master.getTags().stream().map(t -> TagRef.from(t.getTag())).toList(),
                master.isEnabled(), master.getUpdatedAt());
    }
}
