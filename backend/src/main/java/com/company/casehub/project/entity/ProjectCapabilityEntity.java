package com.company.casehub.project.entity;

import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.common.BaseEntity;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.user.entity.UserEntity;
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
@Table(name = "project_capabilities", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class ProjectCapabilityEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capability_id", nullable = false)
    private CapabilityEntity capability;

    @Enumerated(EnumType.STRING)
    @Column(name = "value", nullable = false, length = 16)
    private ProjectCapabilityValue value;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 40)
    private ProjectCapabilitySource source;

    @Column(name = "is_derived", nullable = false)
    private boolean derived;

    @Column(name = "comment")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by", nullable = false)
    private UserEntity updatedBy;
}
