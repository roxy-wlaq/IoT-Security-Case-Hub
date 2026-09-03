package com.company.casehub.testcase.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One transition per decision point in Phase 8. */
@Entity
@Table(name = "transitions", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class TransitionEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_point_id", nullable = false, unique = true)
    private DecisionPointEntity decisionPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private TransitionType type;

    @OneToMany(mappedBy = "transition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransitionTargetEntity> targets = new ArrayList<>();
}
