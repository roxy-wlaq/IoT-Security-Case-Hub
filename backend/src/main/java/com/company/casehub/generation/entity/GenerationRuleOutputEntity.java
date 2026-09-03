package com.company.casehub.generation.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "generation_rule_outputs", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class GenerationRuleOutputEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private GenerationRuleEntity rule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_test_case_id", nullable = false)
    private MasterTestCaseEntity masterTestCase;

    private int sortOrder;
}
