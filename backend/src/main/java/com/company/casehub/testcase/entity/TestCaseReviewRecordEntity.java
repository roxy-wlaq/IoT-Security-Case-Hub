package com.company.casehub.testcase.entity;

import com.company.casehub.common.BaseEntity;
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

/**
 * Append-only audit row for one lifecycle action on a {@link TestCaseVersionEntity}.
 *
 * <p>Never updated or overwritten. The "latest" record for a version (max
 * created_at, ties broken by id) drives derived UI labels such as "Rejected".
 */
@Entity
@Table(name = "test_case_review_records", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class TestCaseReviewRecordEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_version_id", nullable = false)
    private TestCaseVersionEntity testCaseVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private ReviewRecordAction action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private UserEntity reviewer;

    @Column(name = "comment")
    private String comment;
}
