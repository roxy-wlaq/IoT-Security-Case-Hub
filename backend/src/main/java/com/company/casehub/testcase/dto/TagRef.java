package com.company.casehub.testcase.dto;

import com.company.casehub.tag.entity.TagEntity;
import java.util.UUID;

public record TagRef(UUID id, String code, String name) {
    public static TagRef from(TagEntity entity) {
        return new TagRef(entity.getId(), entity.getCode(), entity.getName());
    }
}
