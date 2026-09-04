package com.company.casehub.execution.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.customcase.entity.ProjectCustomTestCaseEntity;
import com.company.casehub.project.entity.ProjectEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_test_cases", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class ProjectTestCaseEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_test_case_id")
    private MasterTestCaseEntity masterTestCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_version_id")
    private TestCaseVersionEntity testCaseVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_test_case_id")
    private ProjectCustomTestCaseEntity customTestCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 24)
    private ExecutionStatus executionStatus = ExecutionStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_status", nullable = false, length = 24)
    private RelationStatus relationStatus = RelationStatus.FLOATING;

    @Column(name = "is_root", nullable = false)
    private boolean root;

    @Column(name = "removed", nullable = false)
    private boolean removed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "last_modified_by", nullable = false)
    private UserEntity lastModifiedBy;

    @Column(name = "last_modified_at", nullable = false)
    private java.time.Instant lastModifiedAt;

    @OneToMany(mappedBy = "projectTestCase")
    private List<ProjectTestCaseSourceEntity> sources = new ArrayList<>();

    @OneToMany(mappedBy = "projectTestCase")
    private List<ProjectTestCaseAssigneeEntity> assignees = new ArrayList<>();
}
