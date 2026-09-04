package com.company.casehub.change.entity;

import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.common.BaseEntity;
import com.company.casehub.project.entity.ProjectCapabilityValue;
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
@Table(name = "capability_update_requests", schema = "casehub")
@Getter @Setter @NoArgsConstructor
public class CapabilityUpdateRequestEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "capability_id", nullable = false)
    private CapabilityEntity capability;
    @Enumerated(EnumType.STRING) @Column(name = "current_value", nullable = false, length = 16)
    private ProjectCapabilityValue currentValue;
    @Enumerated(EnumType.STRING) @Column(name = "proposed_value", nullable = false, length = 16)
    private ProjectCapabilityValue proposedValue;
    @Column(name = "reason", nullable = false) private String reason;
    @Column(name = "evidence_reference") private String evidenceReference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "submitted_by", nullable = false)
    private UserEntity submittedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private UserEntity reviewedBy;
    @Column(name = "review_comment") private String reviewComment;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 16)
    private CapabilityUpdateRequestStatus status = CapabilityUpdateRequestStatus.PENDING;
}
