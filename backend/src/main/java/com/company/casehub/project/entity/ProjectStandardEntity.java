package com.company.casehub.project.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_standards", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class ProjectStandardEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standard_task_type_id", nullable = false)
    private StandardTaskTypeEntity standardTaskType;
}
