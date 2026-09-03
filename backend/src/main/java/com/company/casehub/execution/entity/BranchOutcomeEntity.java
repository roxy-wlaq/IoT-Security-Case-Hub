package com.company.casehub.execution.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TransitionType;
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
@Table(name = "branch_outcomes", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class BranchOutcomeEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_test_case_id", nullable = false)
    private ProjectTestCaseEntity projectTestCase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_point_id", nullable = false)
    private DecisionPointEntity decisionPoint;
    @Enumerated(EnumType.STRING)
    @Column(name = "transition_type", nullable = false, length = 32)
    private TransitionType transitionType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_master_test_case_id")
    private MasterTestCaseEntity targetMasterTestCase;
}
