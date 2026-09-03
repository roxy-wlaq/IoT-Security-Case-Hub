package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.TransitionTargetEntity;
import java.util.UUID;

public record TransitionTargetResponse(UUID id, int targetOrder, UUID masterTestCaseId, String caseCode) {
    public static TransitionTargetResponse from(TransitionTargetEntity target) {
        return new TransitionTargetResponse(target.getId(), target.getTargetOrder(),
                target.getTargetMasterTestCase().getId(), target.getTargetMasterTestCase().getCaseCode());
    }
}
