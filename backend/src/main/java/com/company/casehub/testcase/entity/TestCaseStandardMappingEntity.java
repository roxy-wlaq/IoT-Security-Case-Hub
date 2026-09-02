package com.company.casehub.testcase.entity;

import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_case_standard_mappings", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class TestCaseStandardMappingEntity {

    @jakarta.persistence.Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_version_id", nullable = false)
    private TestCaseVersionEntity testCaseVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standard_task_type_id", nullable = false)
    private StandardTaskTypeEntity standardTaskType;

    @Column(name = "mapping_note")
    private String mappingNote;

    public UUID getId() { return id; }
}
