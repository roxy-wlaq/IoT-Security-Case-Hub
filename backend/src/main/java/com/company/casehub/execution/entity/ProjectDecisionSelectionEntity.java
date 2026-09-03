package com.company.casehub.execution.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_decision_selections", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class ProjectDecisionSelectionEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_test_case_id", nullable = false)
    private ProjectTestCaseEntity projectTestCase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_point_id", nullable = false)
    private DecisionPointEntity decisionPoint;
}
