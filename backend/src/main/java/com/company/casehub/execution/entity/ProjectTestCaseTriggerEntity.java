package com.company.casehub.execution.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_test_case_triggers", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class ProjectTestCaseTriggerEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_project_test_case_id", nullable = false)
    private ProjectTestCaseEntity sourceProjectTestCase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_test_case_version_id", nullable = false)
    private TestCaseVersionEntity sourceTestCaseVersion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_decision_point_id", nullable = false)
    private DecisionPointEntity sourceDecisionPoint;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_project_test_case_id", nullable = false)
    private ProjectTestCaseEntity targetProjectTestCase;
}
