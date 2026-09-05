package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

import com.company.casehub.audit.service.AuditService;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.evidence.entity.EvidenceEntity;
import com.company.casehub.evidence.repository.EvidenceRepository;
import com.company.casehub.evidence.service.EvidenceService;
import com.company.casehub.customcase.entity.ProjectCustomTestCaseEntity;
import com.company.casehub.customcase.repository.ProjectCustomTestCaseRepository;
import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.entity.ProjectStatus;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.storage.StorageService;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class EvidenceDeleteTransactionIT extends AbstractIntegrationTest {

    @Autowired private EvidenceService service;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private ProjectTestCaseRepository testCaseRepository;
    @Autowired private ProjectTestCaseAssigneeRepository assigneeRepository;
    @Autowired private ProjectCustomTestCaseRepository customRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;

    @MockBean private StorageService storage;
    @MockBean private AuditService auditService;

    @TempDir Path tempDir;
    private EvidenceEntity evidence;
    private UserPrincipal tester;
    private Path original;

    @BeforeEach
    void setUp() throws Exception {
        reset(storage, auditService);
        whenStorageResolvesLocalFiles();

        UserEntity user = userRepository.save(new UserEntity("evidence_it_" + UUID.randomUUID(), "Evidence IT", "hash"));
        tester = new UserPrincipal(user.getId(), user.getUsername(), "hash", user.getDisplayName(), true, false,
                Set.of("TESTER"), Set.of("evidence:delete"));

        ProjectEntity project = new ProjectEntity();
        project.setProjectNumber("PRJ-EVIDENCE-" + UUID.randomUUID());
        project.setProjectName("Evidence IT");
        project.setDeviceName("Device");
        project.setStatus(ProjectStatus.DRAFT);
        project.setCreatedBy(user);
        project = projectRepository.saveAndFlush(project);

        ProjectCustomTestCaseEntity custom = new ProjectCustomTestCaseEntity();
        custom.setProject(project);
        custom.setCaseCode("CUSTOM-EVIDENCE-" + UUID.randomUUID());
        custom.setCaseName("Evidence custom case");
        custom.setSelectionMode(SelectionMode.SINGLE);
        custom.setEvidenceRequired(false);
        custom.setCreatedBy(user);
        custom.setUpdatedBy(user);
        custom = customRepository.saveAndFlush(custom);

        ProjectTestCaseEntity ptc = new ProjectTestCaseEntity();
        ptc.setProject(project);
        ptc.setCustomTestCase(custom);
        ptc.setCreatedBy(user);
        ptc.setLastModifiedBy(user);
        ptc.setLastModifiedAt(Instant.now());
        ptc = testCaseRepository.saveAndFlush(ptc);

        ProjectTestCaseAssigneeEntity assignee = new ProjectTestCaseAssigneeEntity();
        assignee.setProjectTestCase(ptc);
        assignee.setUser(user);
        assignee.setAssignedAt(Instant.now());
        assigneeRepository.saveAndFlush(assignee);

        original = tempDir.resolve("final_evidence_original.bin");
        Files.writeString(original, "evidence");
        evidence = new EvidenceEntity();
        evidence.setProjectTestCase(ptc);
        evidence.setStorageKey("final/evidence/" + UUID.randomUUID() + "/original.bin");
        evidence.setOriginalFilename("evidence.txt");
        evidence.setContentType("text/plain");
        evidence.setFileSize(8);
        evidence.setSha256("a".repeat(64));
        evidence.setUploadedBy(user);
        evidence = evidenceRepository.saveAndFlush(evidence);
    }

    @Test
    void auditFailureRollsBackMetadataAndRestoresOriginalObject() throws Exception {
        doThrow(new IllegalStateException("audit unavailable")).when(auditService)
                .record(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                        ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyMap());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.delete(evidence.getId(), tester))
                .isInstanceOf(IllegalStateException.class);

        assertThat(evidenceRepository.findById(evidence.getId())).isPresent();
        assertThat(Files.exists(original)).isTrue();
        verify(storage).move(startsWith("trash/evidence/"), org.mockito.ArgumentMatchers.eq(evidence.getStorageKey()));
    }

    @Test
    void successfulDeleteCommitsMetadataAndCleansTrashAfterCommit() throws Exception {
        service.delete(evidence.getId(), tester);

        assertThat(evidenceRepository.findById(evidence.getId())).isEmpty();
        assertThat(Files.exists(original)).isFalse();
        verify(storage).delete(startsWith("trash/evidence/"));
    }

    @Test
    void postCommitCleanupFailureLeavesOnlyRecoverableTrashOrphan() throws Exception {
        doThrow(new IOException("cleanup unavailable")).when(storage).delete(startsWith("trash/evidence/"));

        service.delete(evidence.getId(), tester);

        assertThat(evidenceRepository.findById(evidence.getId())).isEmpty();
        assertThat(Files.exists(original)).isFalse();
        verify(storage).delete(startsWith("trash/evidence/"));
    }

    private void whenStorageResolvesLocalFiles() throws Exception {
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (key.startsWith("final/evidence/")) {
                return original == null ? tempDir.resolve("final_evidence_original.bin") : original;
            }
            return tempDir.resolve(key.replace('/', '_'));
        }).when(storage).resolve(anyString());
        doAnswer(invocation -> {
            String source = invocation.getArgument(0);
            String target = invocation.getArgument(1);
            Files.move(storage.resolve(source), storage.resolve(target));
            return null;
        }).when(storage).move(anyString(), anyString());
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Files.deleteIfExists(tempDir.resolve(key.replace('/', '_')));
            return null;
        }).when(storage).delete(anyString());
    }

}
