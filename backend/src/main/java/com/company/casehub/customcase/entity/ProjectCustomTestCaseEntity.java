package com.company.casehub.customcase.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.DecisionPointEntity;
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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_custom_test_cases", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class ProjectCustomTestCaseEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;
    @Column(name = "case_code", nullable = false, length = 100)
    private String caseCode;
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by", nullable = false)
    private UserEntity updatedBy;
    @OneToMany(mappedBy = "customTestCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomTestStepEntity> steps = new ArrayList<>();
    @OneToMany(mappedBy = "customTestCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DecisionPointEntity> decisionPoints = new ArrayList<>();
}
