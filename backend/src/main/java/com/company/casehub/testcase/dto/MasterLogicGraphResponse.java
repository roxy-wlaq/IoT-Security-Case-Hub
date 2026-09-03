package com.company.casehub.testcase.dto;

import java.util.List;
import java.util.UUID;

public record MasterLogicGraphResponse(UUID testCaseVersionId, UUID rootMasterTestCaseId,
                                       List<LogicGraphNodeResponse> nodes, List<LogicGraphEdgeResponse> edges) {
}
