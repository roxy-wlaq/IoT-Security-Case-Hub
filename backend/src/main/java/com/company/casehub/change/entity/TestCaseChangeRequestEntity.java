package com.company.casehub.change.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_case_change_requests", schema = "casehub")
@Getter @Setter @NoArgsConstructor
public class TestCaseChangeRequestEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "master_test_case_id", nullable = false)
    private MasterTestCaseEntity masterTestCase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_version_id", nullable = false)
    private TestCaseVersionEntity sourceVersion;
    @Column(name = "reason", nullable = false) private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "submitted_by", nullable = false)
    private UserEntity submittedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private UserEntity reviewedBy;
    @Column(name = "review_comment") private String reviewComment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "revision_draft_version_id") private TestCaseVersionEntity revisionDraftVersion;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 16)
    private TestCaseChangeRequestStatus status = TestCaseChangeRequestStatus.PENDING;
}
