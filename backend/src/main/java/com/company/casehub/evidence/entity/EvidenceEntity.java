package com.company.casehub.evidence.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidence", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class EvidenceEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_test_case_id", nullable = false)
    private ProjectTestCaseEntity projectTestCase;
    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "content_type", length = 255)
    private String contentType;
    @Column(name = "file_size", nullable = false)
    private long fileSize;
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private UserEntity uploadedBy;
}
