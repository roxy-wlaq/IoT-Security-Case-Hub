package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.TestCaseStandardMappingEntity;
import java.util.UUID;

public record StandardMappingRef(UUID standardTaskTypeId, String standardCode, String standardName, String mappingNote) {
    public static StandardMappingRef from(TestCaseStandardMappingEntity entity) {
        return new StandardMappingRef(entity.getStandardTaskType().getId(), entity.getStandardTaskType().getCode(),
                entity.getStandardTaskType().getName(), entity.getMappingNote());
    }
}
