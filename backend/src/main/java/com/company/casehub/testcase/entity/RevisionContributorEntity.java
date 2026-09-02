package com.company.casehub.testcase.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Grants one user temporary edit access to a single Draft version, distinct
 * from the Draft owner (Data Model §50). Resource-level RBAC consults this
 * relation for edit/submit permissions.
 */
@Entity
@Table(name = "revision_contributors", schema = "casehub",
        uniqueConstraints = @UniqueConstraint(name = "uq_revision_contributors",
                columnNames = {"test_case_version_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class RevisionContributorEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_version_id", nullable = false)
    private TestCaseVersionEntity testCaseVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "added_by", nullable = false)
    private UserEntity addedBy;
}
