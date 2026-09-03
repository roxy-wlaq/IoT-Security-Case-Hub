package com.company.casehub.testcase.dto;

import java.util.UUID;

public record LogicGraphEdgeResponse(UUID id, UUID sourceMasterTestCaseId, UUID targetMasterTestCaseId,
                                     String transitionType, String label) {
}
