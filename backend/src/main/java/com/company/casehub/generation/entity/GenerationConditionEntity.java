package com.company.casehub.generation.entity;

import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.common.BaseEntity;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
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
@Table(name = "generation_conditions", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class GenerationConditionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GenerationConditionGroupEntity group;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private ConditionTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capability_id")
    private CapabilityEntity capability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_task_type_id")
    private StandardTaskTypeEntity standardTaskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 32)
    private GenerationOperator operator;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
