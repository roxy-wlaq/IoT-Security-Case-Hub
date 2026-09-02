package com.company.casehub.testcase.entity;

import com.company.casehub.common.BaseEntity;
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
@Table(name = "test_steps", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class TestStepEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_version_id", nullable = false)
    private TestCaseVersionEntity testCaseVersion;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;
}
