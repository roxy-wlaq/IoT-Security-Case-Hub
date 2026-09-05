package com.company.casehub.evidence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.evidence.entity.EvidenceEntity;
import com.company.casehub.evidence.repository.EvidenceRepository;
import com.company.casehub.evidence.service.EvidenceService;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.storage.StorageService;
import com.company.casehub.user.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceAuthorizationTest {
    @Mock private EvidenceRepository evidenceRepository;
    @Mock private ProjectTestCaseRepository testCaseRepository;
    @Mock private ProjectTestCaseAssigneeRepository assigneeRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storage;
    @Mock private ProjectAccessPolicy accessPolicy;
    @Mock private AuditService auditService;
    @TempDir Path tempDir;
    private EvidenceService service;
    private UserPrincipal tester;
    private ProjectTestCaseEntity ptc;

    @BeforeEach
    void setUp() {
        service = new EvidenceService(evidenceRepository, testCaseRepository, assigneeRepository, userRepository,
                storage, accessPolicy, auditService);
        UUID userId = UUID.randomUUID();
        tester = new UserPrincipal(userId, "tester", "hash", "Tester", true, false, Set.of("TESTER"), Set.of("evidence:read"));
        ProjectEntity project = new ProjectEntity(); project.setId(UUID.randomUUID());
        ptc = new ProjectTestCaseEntity(); ptc.setId(UUID.randomUUID()); ptc.setProject(project);
    }

    @Test
    void unassignedUserCannotUploadOrDownload() {
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(false);
        when(accessPolicy.canView(ptc.getProject().getId(), tester)).thenReturn(false);

        assertThatThrownBy(() -> service.upload(ptc.getId(), null, tester))
                .isInstanceOf(ForbiddenOperationException.class);
        EvidenceEntity evidence = new EvidenceEntity(); evidence.setId(UUID.randomUUID()); evidence.setProjectTestCase(ptc);
        when(evidenceRepository.findById(evidence.getId())).thenReturn(Optional.of(evidence));
        assertThatThrownBy(() -> service.download(evidence.getId(), tester))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteRestoresTrashObjectWhenMetadataDeleteFails() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        EvidenceEntity evidence = new EvidenceEntity(); evidence.setId(evidenceId); evidence.setProjectTestCase(ptc);
        evidence.setStorageKey("final/evidence/original.bin");
        when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(evidence));
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(true);
        Path trash = Files.createFile(tempDir.resolve("trash.bin"));
        when(storage.resolve(startsWith("trash/evidence/"))).thenReturn(trash);
        doThrow(new IllegalStateException("db failure")).when(evidenceRepository).delete(any(EvidenceEntity.class));

        assertThatThrownBy(() -> service.delete(evidenceId, tester)).isInstanceOf(IllegalStateException.class);
        verify(storage).move(eq("final/evidence/original.bin"), startsWith("trash/evidence/"));
    }

    @Test
    void successfulDeleteEmitsOneEvidenceAuditEvent() {
        UUID evidenceId = UUID.randomUUID();
        EvidenceEntity evidence = new EvidenceEntity();
        evidence.setId(evidenceId);
        evidence.setProjectTestCase(ptc);
        evidence.setStorageKey("final/evidence/original.bin");
        evidence.setOriginalFilename("report.txt");
        evidence.setFileSize(12L);
        when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(evidence));
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(true);
        service.delete(evidenceId, tester);

        verify(auditService).record(eq(AuditAction.EVIDENCE_DELETE), eq(tester), eq("EVIDENCE"),
                eq(evidenceId), eq("report.txt"), anyMap());
    }

    @Test
    void deleteDefersTrashCleanupUntilTransactionCommit() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        EvidenceEntity evidence = evidence(evidenceId);
        when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(evidence));
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(evidenceId, tester);

            verify(storage, never()).delete(startsWith("trash/evidence/"));
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
            synchronization.afterCommit();
            verify(storage).delete(startsWith("trash/evidence/"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteRestoresTrashObjectOnlyAfterTransactionRollback() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        EvidenceEntity evidence = evidence(evidenceId);
        when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(evidence));
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(true);
        when(storage.resolve(startsWith("trash/evidence/"))).thenReturn(tempDir.resolve("trash.bin"));
        Files.createFile(tempDir.resolve("trash.bin"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(evidenceId, tester);

            verify(storage, never()).move(startsWith("trash/evidence/"), eq(evidence.getStorageKey()));
            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(storage).move(startsWith("trash/evidence/"), eq(evidence.getStorageKey()));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteRestoresTrashObjectWhenTransactionCompletionIsUnknown() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        EvidenceEntity evidence = evidence(evidenceId);
        when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(evidence));
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(true);
        when(storage.resolve(startsWith("trash/evidence/"))).thenReturn(tempDir.resolve("trash.bin"));
        Files.createFile(tempDir.resolve("trash.bin"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(evidenceId, tester);

            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

            verify(storage).move(startsWith("trash/evidence/"), eq(evidence.getStorageKey()));
            verify(storage, never()).delete(startsWith("trash/evidence/"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void auditFailureLeavesRollbackRestorationToTransactionSynchronization() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        EvidenceEntity evidence = evidence(evidenceId);
        when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(evidence));
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(true);
        when(storage.resolve(startsWith("trash/evidence/"))).thenReturn(tempDir.resolve("trash.bin"));
        Files.createFile(tempDir.resolve("trash.bin"));
        doThrow(new IllegalStateException("audit unavailable")).when(auditService).record(any(), any(), any(), any(), any(), anyMap());

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> service.delete(evidenceId, tester))
                    .isInstanceOf(IllegalStateException.class);
            verify(storage, never()).move(startsWith("trash/evidence/"), eq(evidence.getStorageKey()));
            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(storage).move(startsWith("trash/evidence/"), eq(evidence.getStorageKey()));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private EvidenceEntity evidence(UUID evidenceId) {
        EvidenceEntity evidence = new EvidenceEntity();
        evidence.setId(evidenceId);
        evidence.setProjectTestCase(ptc);
        evidence.setStorageKey("final/evidence/original.bin");
        evidence.setOriginalFilename("report.txt");
        evidence.setFileSize(12L);
        return evidence;
    }

    @Test
    void missingPhysicalObjectIsReportedAsNotFound() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        EvidenceEntity evidence = new EvidenceEntity(); evidence.setId(evidenceId); evidence.setProjectTestCase(ptc);
        evidence.setStorageKey("final/evidence/missing.bin");
        when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(evidence));
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(accessPolicy.canView(ptc.getProject().getId(), tester)).thenReturn(true);
        when(storage.resolve(evidence.getStorageKey())).thenReturn(tempDir.resolve("missing.bin"));

        assertThatThrownBy(() -> service.download(evidenceId, tester)).isInstanceOf(ResourceNotFoundException.class);
    }
}
