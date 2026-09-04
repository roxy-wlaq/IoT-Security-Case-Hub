package com.company.casehub.change.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.change.dto.CapabilityUpdateRequestPayload;
import com.company.casehub.change.dto.CapabilityUpdateRequestResponse;
import com.company.casehub.change.dto.ReviewRequestPayload;
import com.company.casehub.change.entity.CapabilityUpdateRequestEntity;
import com.company.casehub.change.entity.CapabilityUpdateRequestStatus;
import com.company.casehub.change.repository.CapabilityUpdateRequestRepository;
import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.generation.dto.GenerationRunRequest;
import com.company.casehub.generation.entity.GenerationRunMode;
import com.company.casehub.generation.entity.GenerationTriggerType;
import com.company.casehub.generation.service.GenerationRuntimeService;
import com.company.casehub.project.entity.ProjectCapabilityEntity;
import com.company.casehub.project.entity.ProjectCapabilitySource;
import com.company.casehub.project.entity.ProjectCapabilityValue;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectCapabilityRepository;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapabilityUpdateRequestService {
    private final CapabilityUpdateRequestRepository requestRepository;
    private final ProjectRepository projectRepository;
    private final CapabilityRepository capabilityRepository;
    private final ProjectCapabilityRepository capabilityValueRepository;
    private final UserRepository userRepository;
    private final ProjectAccessPolicy accessPolicy;
    private final com.company.casehub.project.service.CapabilityEngine capabilityEngine;
    private final GenerationRuntimeService generationRuntimeService;

    public CapabilityUpdateRequestService(CapabilityUpdateRequestRepository requestRepository, ProjectRepository projectRepository,
                                          CapabilityRepository capabilityRepository, ProjectCapabilityRepository capabilityValueRepository,
                                          UserRepository userRepository, ProjectAccessPolicy accessPolicy,
                                          com.company.casehub.project.service.CapabilityEngine capabilityEngine,
                                          GenerationRuntimeService generationRuntimeService) {
        this.requestRepository = requestRepository; this.projectRepository = projectRepository; this.capabilityRepository = capabilityRepository;
        this.capabilityValueRepository = capabilityValueRepository; this.userRepository = userRepository; this.accessPolicy = accessPolicy;
        this.capabilityEngine = capabilityEngine; this.generationRuntimeService = generationRuntimeService;
    }

    @Transactional
    public CapabilityUpdateRequestResponse submit(UUID projectId, UUID capabilityId, CapabilityUpdateRequestPayload payload, UserPrincipal principal) {
        accessPolicy.requireView(projectId, principal);
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found"));
        CapabilityEntity capability = capabilityRepository.findById(capabilityId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CAPABILITY_NOT_FOUND, "Capability not found"));
        ProjectCapabilityValue current = capabilityValueRepository.findByProjectIdAndCapabilityId(projectId, capabilityId).map(ProjectCapabilityEntity::getValue).orElse(ProjectCapabilityValue.UNKNOWN);
        CapabilityUpdateRequestEntity request = new CapabilityUpdateRequestEntity(); request.setProject(project); request.setCapability(capability); request.setCurrentValue(current); request.setProposedValue(payload.proposedValue()); request.setReason(payload.reason().trim()); request.setEvidenceReference(trim(payload.evidenceReference())); request.setSubmittedBy(currentUser(principal));
        return response(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<CapabilityUpdateRequestResponse> list(UUID projectId, UserPrincipal principal) {
        accessPolicy.requireView(projectId, principal);
        return requestRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(this::response).toList();
    }

    @Transactional
    public CapabilityUpdateRequestResponse review(UUID requestId, boolean approve, ReviewRequestPayload payload, UserPrincipal principal) {
        CapabilityUpdateRequestEntity request = requestRepository.findByIdForUpdate(requestId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CAPABILITY_REQUEST_NOT_FOUND, "Capability Update Request not found"));
        accessPolicy.requireManage(request.getProject().getId(), principal);
        if (request.getStatus() != CapabilityUpdateRequestStatus.PENDING) throw new ConflictException(ErrorCode.CAPABILITY_REQUEST_STATE_INVALID, "Request is already reviewed");
        UserEntity reviewer = currentUser(principal); request.setReviewedBy(reviewer); request.setReviewComment(payload == null ? null : trim(payload.comment()));
        if (!approve) { request.setStatus(CapabilityUpdateRequestStatus.REJECTED); return response(requestRepository.save(request)); }
        ProjectCapabilityEntity row = capabilityValueRepository.findByProjectIdAndCapabilityId(request.getProject().getId(), request.getCapability().getId()).orElseGet(ProjectCapabilityEntity::new);
        row.setProject(request.getProject()); row.setCapability(request.getCapability()); row.setValue(request.getProposedValue()); row.setSource(ProjectCapabilitySource.TESTER_DISCOVERED); row.setDerived(false); row.setUpdatedBy(reviewer); capabilityValueRepository.save(row);
        capabilityEngine.recalculateDerivedParents(request.getProject().getId(), request.getCapability().getId(), reviewer);
        generationRuntimeService.run(request.getProject().getId(), new GenerationRunRequest(GenerationRunMode.FULL, GenerationTriggerType.CAPABILITY_UPDATE), principal);
        request.setStatus(CapabilityUpdateRequestStatus.APPROVED);
        return response(requestRepository.save(request));
    }

    private CapabilityUpdateRequestResponse response(CapabilityUpdateRequestEntity r) { return new CapabilityUpdateRequestResponse(r.getId(), r.getProject().getId(), r.getCapability().getId(), r.getCurrentValue(), r.getProposedValue(), r.getReason(), r.getEvidenceReference(), r.getSubmittedBy().getId(), r.getReviewedBy() == null ? null : r.getReviewedBy().getId(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()); }
    private UserEntity currentUser(UserPrincipal p) { return userRepository.findById(p.getId()).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found")); }
    private static String trim(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }
}
