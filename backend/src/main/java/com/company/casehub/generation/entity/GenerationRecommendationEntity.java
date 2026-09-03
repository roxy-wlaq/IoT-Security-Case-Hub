package com.company.casehub.generation.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "generation_recommendations", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class GenerationRecommendationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_run_id", nullable = false)
    private GenerationRunEntity generationRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_test_case_id", nullable = false)
    private MasterTestCaseEntity masterTestCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolved_test_case_version_id", nullable = false)
    private TestCaseVersionEntity resolvedVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecommendationStatus status = RecommendationStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_project_test_case_id")
    private ProjectTestCaseEntity addedProjectTestCase;

    @jakarta.persistence.OneToMany(mappedBy = "recommendation", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<GenerationRecommendationRuleEntity> matchedRules = new ArrayList<>();
}
