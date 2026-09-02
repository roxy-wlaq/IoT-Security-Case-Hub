package com.company.casehub.testcase.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_case_versions", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class TestCaseVersionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_test_case_id", nullable = false)
    private MasterTestCaseEntity masterTestCase;

    @Column(name = "version_major", nullable = false)
    private int versionMajor;

    @Column(name = "version_minor", nullable = false)
    private int versionMinor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TestCaseVersionStatus status;

    @Column(name = "is_current_version", nullable = false)
    private boolean currentVersion;

    @Column(name = "case_name", nullable = false, length = 255)
    private String caseName;

    @Column(name = "test_purpose")
    private String testPurpose;

    @Column(name = "preconditions")
    private String preconditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_mode", nullable = false, length = 16)
    private SelectionMode selectionMode;

    @Column(name = "evidence_required", nullable = false)
    private boolean evidenceRequired;

    @Column(name = "evidence_requirement")
    private String evidenceRequirement;

    @Column(name = "remark_requirement")
    private String remarkRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "progressive_role", length = 16)
    private ProgressiveRole progressiveRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "based_on_version_id")
    private TestCaseVersionEntity basedOnVersion;

    @Column(name = "change_request_id")
    private java.util.UUID changeRequestId;

    @Column(name = "change_reason")
    private String changeReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;

    @Column(name = "revision_closed", nullable = false)
    private boolean revisionClosed;

    @OneToMany(mappedBy = "testCaseVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestStepEntity> steps = new ArrayList<>();

    @OneToMany(mappedBy = "testCaseVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCaseToolEntity> tools = new ArrayList<>();

    @OneToMany(mappedBy = "testCaseVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCaseStandardMappingEntity> standardMappings = new ArrayList<>();

    @OneToMany(mappedBy = "testCaseVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCaseAttachmentEntity> attachments = new ArrayList<>();
}
