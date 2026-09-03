package com.company.casehub.evidence.entity;

import com.company.casehub.common.BaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notes", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class NoteEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_test_case_id", nullable = false)
    private ProjectTestCaseEntity projectTestCase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;
}
