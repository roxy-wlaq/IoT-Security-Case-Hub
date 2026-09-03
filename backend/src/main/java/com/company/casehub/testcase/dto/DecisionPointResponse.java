package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.DecisionPointEntity;
import java.util.UUID;

public record DecisionPointResponse(UUID id, UUID testCaseVersionId, int displayOrder, String name, String description,
                                    TransitionResponse transition) {
    public static DecisionPointResponse from(DecisionPointEntity point) {
        return new DecisionPointResponse(point.getId(), point.getTestCaseVersion().getId(), point.getDisplayOrder(),
                point.getName(), point.getDescription(), point.getTransition() == null ? null : TransitionResponse.from(point.getTransition()));
    }
}
