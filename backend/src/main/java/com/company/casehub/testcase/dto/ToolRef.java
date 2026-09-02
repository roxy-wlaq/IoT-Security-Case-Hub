package com.company.casehub.testcase.dto;

import com.company.casehub.tool.entity.ToolEntity;
import java.util.UUID;

public record ToolRef(UUID id, String code, String name) {
    public static ToolRef from(ToolEntity entity) {
        return new ToolRef(entity.getId(), entity.getCode(), entity.getName());
    }
}
