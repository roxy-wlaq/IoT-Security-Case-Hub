package com.company.casehub.testcase.dto;

import java.util.UUID;

public record LogicGraphNodeResponse(UUID masterTestCaseId, String caseCode, String label) {
}
