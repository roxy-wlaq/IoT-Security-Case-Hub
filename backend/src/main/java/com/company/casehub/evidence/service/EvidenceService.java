package com.company.casehub.evidence.service;

import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.entity.AuditResourceType;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.evidence.dto.EvidenceResponse;
import com.company.casehub.evidence.entity.EvidenceEntity;
import com.company.casehub.evidence.repository.EvidenceRepository;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.storage.StorageService;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.user.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EvidenceService {
    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);
    private final EvidenceRepository evidenceRepository;
    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final UserRepository userRepository;
    private final StorageService storage;
    private final ProjectAccessPolicy accessPolicy;
    private final AuditService auditService;

    public EvidenceService(EvidenceRepository evidenceRepository, ProjectTestCaseRepository testCaseRepository,
                           ProjectTestCaseAssigneeRepository assigneeRepository, UserRepository userRepository,
                           StorageService storage, ProjectAccessPolicy accessPolicy) {
        this(evidenceRepository, testCaseRepository, assigneeRepository, userRepository, storage, accessPolicy, null);
    }

    @Autowired
    public EvidenceService(EvidenceRepository evidenceRepository, ProjectTestCaseRepository testCaseRepository,
                           ProjectTestCaseAssigneeRepository assigneeRepository, UserRepository userRepository,
                           StorageService storage, ProjectAccessPolicy accessPolicy, AuditService auditService) {
        this.evidenceRepository = evidenceRepository;
        this.testCaseRepository = testCaseRepository;
        this.assigneeRepository = assigneeRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.accessPolicy = accessPolicy;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> list(UUID ptcId, UserPrincipal principal) {
        requireReadable(ptcId, principal);
        return evidenceRepository.findByProjectTestCaseIdOrderByCreatedAtAsc(ptcId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EvidenceResponse upload(UUID ptcId, MultipartFile file, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = requireAssigned(ptcId, principal, ErrorCode.EVIDENCE_UPLOAD_FORBIDDEN);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Evidence file must not be empty");
        }
        String tempKey = null;
        String finalKey = "final/evidence/" + ptcId + "/" + UUID.randomUUID() + ".bin";
        try {
            tempKey = storage.writeTemp(file.getInputStream(), file.getOriginalFilename());
            String hash = storage.sha256(storage.resolve(tempKey));
            storage.move(tempKey, finalKey);
            EvidenceEntity entity = new EvidenceEntity();
            entity.setProjectTestCase(ptc);
            entity.setStorageKey(finalKey);
            entity.setOriginalFilename(file.getOriginalFilename() == null ? "evidence" : file.getOriginalFilename());
            entity.setContentType(file.getContentType());
            entity.setFileSize(file.getSize());
            entity.setSha256(hash);
            entity.setUploadedBy(userRepository.findById(principal.getId()).orElseThrow());
            return toResponse(evidenceRepository.saveAndFlush(entity));
        } catch (IOException | RuntimeException ex) {
            try {
                if (tempKey != null) storage.delete(tempKey);
                storage.delete(finalKey);
            } catch (IOException ignored) {
                // A later cleanup job may remove an orphaned object.
            }
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Evidence storage failed", ex);
        }
    }

    @Transactional(readOnly = true)
    public Download download(UUID evidenceId, UserPrincipal principal) {
        EvidenceEntity entity = requireEvidence(evidenceId);
        requireReadable(entity.getProjectTestCase().getId(), principal);
        try {
            var path = storage.resolve(entity.getStorageKey());
            if (!Files.exists(path)) {
                throw new ResourceNotFoundException(ErrorCode.STORAGE_OBJECT_MISSING, "Evidence object is missing");
            }
            return new Download(entity.getOriginalFilename(), entity.getContentType(), Files.readAllBytes(path));
        } catch (IOException ex) {
            throw new ResourceNotFoundException(ErrorCode.STORAGE_OBJECT_MISSING, "Evidence object is unavailable");
        }
    }

    @Transactional
    public void delete(UUID evidenceId, UserPrincipal principal) {
        EvidenceEntity entity = requireEvidence(evidenceId);
        requireAssigned(entity.getProjectTestCase().getId(), principal, ErrorCode.EVIDENCE_DELETE_FORBIDDEN);
        String trashKey = "trash/evidence/" + UUID.randomUUID() + ".bin";
        boolean synchronizationRegistered = false;
        try {
            storage.move(entity.getStorageKey(), trashKey);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new EvidenceDeletionSynchronization(storage, entity.getStorageKey(), trashKey));
                synchronizationRegistered = true;
            }
            evidenceRepository.delete(entity);
            evidenceRepository.flush();
            if (auditService != null) {
                auditService.record(AuditAction.EVIDENCE_DELETE, principal, AuditResourceType.EVIDENCE,
                        entity.getId(), entity.getOriginalFilename(), Map.of("fileSize", entity.getFileSize()));
            }
        } catch (IOException | RuntimeException ex) {
            if (!synchronizationRegistered) {
                restoreFromTrash(storage, trashKey, entity.getStorageKey());
            }
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Evidence delete failed", ex);
        }
    }

    private static void restoreFromTrash(StorageService storage, String trashKey, String originalKey) {
        try {
            if (Files.exists(storage.resolve(trashKey))) {
                storage.move(trashKey, originalKey);
            }
        } catch (IOException | RuntimeException restoreFailure) {
            log.warn("Could not restore evidence object {} from trash", trashKey, restoreFailure);
        }
    }

    private static final class EvidenceDeletionSynchronization implements TransactionSynchronization {
        private final StorageService storage;
        private final String originalKey;
        private final String trashKey;

        private EvidenceDeletionSynchronization(StorageService storage, String originalKey, String trashKey) {
            this.storage = storage;
            this.originalKey = originalKey;
            this.trashKey = trashKey;
        }

        @Override
        public void afterCommit() {
            try {
                storage.delete(trashKey);
            } catch (IOException | RuntimeException cleanupFailure) {
                log.warn("Could not permanently delete committed evidence trash object {}", trashKey, cleanupFailure);
            }
        }

        @Override
        public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
                restoreFromTrash(storage, trashKey, originalKey);
            }
        }
    }

    private ProjectTestCaseEntity requireAssigned(UUID ptcId, UserPrincipal principal, ErrorCode forbiddenCode) {
        ProjectTestCaseEntity ptc = testCaseRepository.findById(ptcId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        if (ptc.isRemoved() || !assigneeRepository.existsByProjectTestCaseIdAndUserId(ptcId, principal.getId())) {
            throw new ForbiddenOperationException(forbiddenCode, "The case is not assigned to you");
        }
        return ptc;
    }

    private void requireReadable(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = testCaseRepository.findById(ptcId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        boolean readable = accessPolicy.canView(ptc.getProject().getId(), principal);
        if (!readable) throw new ForbiddenOperationException(ErrorCode.EVIDENCE_DOWNLOAD_FORBIDDEN, "The case is not assigned to you");
    }

    private EvidenceEntity requireEvidence(UUID id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
    }

    private EvidenceResponse toResponse(EvidenceEntity e) {
        return new EvidenceResponse(e.getId(), e.getProjectTestCase().getId(), e.getOriginalFilename(), e.getFileSize(),
                e.getContentType(), e.getSha256(), e.getUploadedBy().getId(), e.getCreatedAt());
    }

    public record Download(String filename, String contentType, byte[] bytes) { }
}
