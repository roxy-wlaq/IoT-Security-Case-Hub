package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.TransitionEntity;
import com.company.casehub.testcase.entity.TransitionType;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record TransitionResponse(UUID id, TransitionType type, List<TransitionTargetResponse> targets) {
    public static TransitionResponse from(TransitionEntity transition) {
        return new TransitionResponse(transition.getId(), transition.getType(), transition.getTargets().stream()
                .sorted(Comparator.comparingInt(target -> target.getTargetOrder()))
                .map(TransitionTargetResponse::from).toList());
    }
}
