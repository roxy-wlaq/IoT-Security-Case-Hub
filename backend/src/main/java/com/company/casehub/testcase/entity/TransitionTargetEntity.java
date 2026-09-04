package com.company.casehub.testcase.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.customcase.entity.ProjectCustomTestCaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A stable Master Test Case target; version resolution is deliberately deferred. */
@Entity
@Table(name = "transition_targets", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class TransitionTargetEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transition_id", nullable = false)
    private TransitionEntity transition;

    @Column(name = "target_order", nullable = false)
    private int targetOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_master_test_case_id")
    private MasterTestCaseEntity targetMasterTestCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_custom_test_case_id")
    private ProjectCustomTestCaseEntity targetCustomTestCase;
}
