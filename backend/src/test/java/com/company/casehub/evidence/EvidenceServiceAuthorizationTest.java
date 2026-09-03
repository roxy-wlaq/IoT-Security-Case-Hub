package com.company.casehub.evidence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
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

@ExtendWith(MockitoExtension.class)
class EvidenceServiceAuthorizationTest {
    @Mock private EvidenceRepository evidenceRepository;
    @Mock private ProjectTestCaseRepository testCaseRepository;
    @Mock private ProjectTestCaseAssigneeRepository assigneeRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storage;
    @Mock private ProjectAccessPolicy accessPolicy;
    @TempDir Path tempDir;
    private EvidenceService service;
    private UserPrincipal tester;
    private ProjectTestCaseEntity ptc;

    @BeforeEach
    void setUp() {
        service = new EvidenceService(evidenceRepository, testCaseRepository, assigneeRepository, userRepository, storage, accessPolicy);
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
