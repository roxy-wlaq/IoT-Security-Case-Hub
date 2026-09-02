package com.company.casehub.testcase.dto;

import com.company.casehub.testcase.entity.TestCaseAttachmentEntity;
import java.time.Instant;
import java.util.UUID;

public record AttachmentRef(UUID id, String originalFilename, long fileSize, String contentType,
                            String description, UUID uploadedBy, Instant createdAt) {
    public static AttachmentRef from(TestCaseAttachmentEntity entity) {
        return new AttachmentRef(entity.getId(), entity.getOriginalFilename(), entity.getFileSize(),
                entity.getContentType(), entity.getDescription(), entity.getUploadedBy().getId(), entity.getCreatedAt());
    }
}
