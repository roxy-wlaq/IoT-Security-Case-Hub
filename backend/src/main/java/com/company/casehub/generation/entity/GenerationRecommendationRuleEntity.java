package com.company.casehub.generation.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "generation_recommendation_rules", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class GenerationRecommendationRuleEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private GenerationRecommendationEntity recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_rule_id", nullable = false)
    private GenerationRuleEntity rule;
}
