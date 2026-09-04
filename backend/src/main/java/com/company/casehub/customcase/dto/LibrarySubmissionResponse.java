package com.company.casehub.customcase.dto;

import java.util.UUID;

public record LibrarySubmissionResponse(UUID customTestCaseId, UUID masterTestCaseId, UUID draftVersionId,
                                        UUID contributorId) {
}
