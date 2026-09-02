package com.company.casehub.testcase.entity;

import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.common.BaseEntity;
import com.company.casehub.tag.entity.TagEntity;
import com.company.casehub.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "master_test_cases", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class MasterTestCaseEntity extends BaseEntity {

    @Column(name = "case_code", nullable = false, length = 100)
    private String caseCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "masterTestCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCaseVersionEntity> versions = new ArrayList<>();

    @OneToMany(mappedBy = "masterTestCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCaseTagEntity> tags = new ArrayList<>();
}
