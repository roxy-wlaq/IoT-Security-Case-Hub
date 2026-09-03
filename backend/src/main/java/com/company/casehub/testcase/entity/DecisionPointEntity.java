package com.company.casehub.testcase.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A version-owned, ordered decision point in the master test-case template. */
@Entity
@Table(name = "decision_points", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class DecisionPointEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_version_id", nullable = false)
    private TestCaseVersionEntity testCaseVersion;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @OneToOne(mappedBy = "decisionPoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TransitionEntity transition;
}
