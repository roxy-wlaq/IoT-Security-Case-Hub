package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.TestStepEntity;
import java.util.UUID;

public record TestStepResponse(UUID id, int sequenceNo, String title, String content) {
    public static TestStepResponse from(TestStepEntity entity) {
        return new TestStepResponse(entity.getId(), entity.getSequenceNo(), entity.getTitle(), entity.getContent());
    }
}
