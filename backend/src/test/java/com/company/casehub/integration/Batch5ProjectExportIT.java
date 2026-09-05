package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.customcase.entity.ProjectCustomTestCaseEntity;
import com.company.casehub.customcase.repository.ProjectCustomTestCaseRepository;
import com.company.casehub.evidence.entity.EvidenceEntity;
import com.company.casehub.evidence.repository.EvidenceRepository;
import com.company.casehub.execution.entity.ExecutionStatus;
import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.entity.RelationStatus;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseSourceRepository;
import com.company.casehub.export.dto.ProjectExportSnapshot;
import com.company.casehub.export.service.ProjectExportService;
import com.company.casehub.project.entity.ProjectCoordinatorEntity;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectCoordinatorRepository;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.testcase.entity.SelectionMode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Batch5ProjectExportIT extends AbstractIntegrationTest {

    @Autowired private ProjectExportService exportService;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectCoordinatorRepository coordinatorRepository;
    @Autowired private ProjectCustomTestCaseRepository customRepository;
    @Autowired private ProjectTestCaseRepository projectTestCaseRepository;
    @Autowired private ProjectTestCaseSourceRepository sourceRepository;
    @Autowired private ProjectTestCaseAssigneeRepository assigneeRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private ProjectEntity project;
    private UserPrincipal coordinator;
    private UserPrincipal outsider;
    private ProjectTestCaseEntity customPtc;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity coordinatorUser = userRepository.save(new UserEntity("export_coord_" + suffix, "Export Coordinator", "hash"));
        UserEntity testerUser = userRepository.save(new UserEntity("export_tester_" + suffix, "Export Tester", "hash"));
        UserEntity outsiderUser = userRepository.save(new UserEntity("export_outsider_" + suffix, "Export Outsider", "hash"));
        coordinator = principal(coordinatorUser, "TEST_COORDINATOR");
        outsider = principal(outsiderUser, "TEST_COORDINATOR");

        project = new ProjectEntity();
        project.setProjectNumber("PRJ-EXPORT-" + suffix);
        project.setProjectName("Export Project");
        project.setDeviceName("Device");
        project.setCreatedBy(coordinatorUser);
        project = projectRepository.saveAndFlush(project);

        ProjectCoordinatorEntity membership = new ProjectCoordinatorEntity();
        membership.setProject(project);
        membership.setUser(coordinatorUser);
        membership.setPrimary(true);
        coordinatorRepository.saveAndFlush(membership);

        ProjectCustomTestCaseEntity custom = new ProjectCustomTestCaseEntity();
        custom.setProject(project);
        custom.setCaseCode("CUSTOM-" + suffix);
        custom.setCaseName("Custom export case");
        custom.setSelectionMode(SelectionMode.SINGLE);
        custom.setEvidenceRequired(false);
        custom.setCreatedBy(coordinatorUser);
        custom.setUpdatedBy(coordinatorUser);
        custom = customRepository.saveAndFlush(custom);

        customPtc = new ProjectTestCaseEntity();
        customPtc.setProject(project);
        customPtc.setCustomTestCase(custom);
        customPtc.setExecutionStatus(ExecutionStatus.COMPLETED);
        customPtc.setRelationStatus(RelationStatus.FLOATING);
        customPtc.setRoot(false);
        customPtc.setRemoved(true);
        customPtc.setCreatedBy(coordinatorUser);
        customPtc.setLastModifiedBy(coordinatorUser);
        customPtc.setLastModifiedAt(Instant.now());
        customPtc = projectTestCaseRepository.saveAndFlush(customPtc);

        ProjectTestCaseSourceEntity source = new ProjectTestCaseSourceEntity();
        source.setProjectTestCase(customPtc);
        source.setSourceType(ProjectTestCaseSourceType.CUSTOM);
        sourceRepository.saveAndFlush(source);

        ProjectTestCaseAssigneeEntity assignee = new ProjectTestCaseAssigneeEntity();
        assignee.setProjectTestCase(customPtc);
        assignee.setUser(testerUser);
        assignee.setAssignedAt(Instant.now());
        assigneeRepository.saveAndFlush(assignee);

        EvidenceEntity evidence = new EvidenceEntity();
        evidence.setProjectTestCase(customPtc);
        evidence.setStorageKey("final/evidence/" + customPtc.getId() + "/metadata-only.bin");
        evidence.setOriginalFilename("evidence.txt");
        evidence.setContentType("text/plain");
        evidence.setFileSize(7);
        evidence.setSha256("a".repeat(64));
        evidence.setUploadedBy(testerUser);
        evidenceRepository.saveAndFlush(evidence);
    }

    @Test
    void authorizedProjectSnapshotContainsCustomPlanAndEvidenceMetadataOnly() {
        ProjectExportSnapshot snapshot = exportService.snapshot(project.getId(), coordinator);

        assertThat(snapshot.projectNumber()).isEqualTo(project.getProjectNumber());
        assertThat(snapshot.testCases()).singleElement().satisfies(row -> {
            assertThat(row.backingType()).isEqualTo("CUSTOM");
            assertThat(row.planSources()).isEqualTo("CUSTOM");
            assertThat(row.executionStatus()).isEqualTo("COMPLETED");
            assertThat(row.relationStatus()).isEqualTo("FLOATING");
            assertThat(row.removed()).isTrue();
            assertThat(row.assignees()).contains("Export Tester");
            assertThat(row.evidenceCount()).isEqualTo(1);
        });
        assertThat(snapshot.evidence()).singleElement().satisfies(row -> {
            assertThat(row.originalFilename()).isEqualTo("evidence.txt");
            assertThat(row.fileSize()).isEqualTo(7);
            assertThat(row.sha256()).isEqualTo("a".repeat(64));
        });
        assertThat(snapshot.evidence().get(0).toString()).doesNotContain("storageKey", "final/evidence");
    }

    @Test
    void projectWithoutResourceAccessCannotExport() {
        assertThatThrownBy(() -> exportService.snapshot(project.getId(), outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void repeatableReadExportSnapshotDoesNotMixConcurrentProjectMutation() throws Exception {
        Transactional transactional = ProjectExportService.class
                .getMethod("snapshot", UUID.class, UserPrincipal.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional.isolation()).isEqualTo(Isolation.REPEATABLE_READ);

        CountDownLatch firstRead = new CountDownLatch(1);
        CountDownLatch allowSecondRead = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> names = executor.submit(() -> {
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
                return transaction.execute(status -> {
                    String before = jdbcTemplate.queryForObject(
                            "SELECT project_name FROM casehub.projects WHERE id = ?", String.class, project.getId());
                    firstRead.countDown();
                    try {
                        assertThat(allowSecondRead.await(10, TimeUnit.SECONDS)).isTrue();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    String after = jdbcTemplate.queryForObject(
                            "SELECT project_name FROM casehub.projects WHERE id = ?", String.class, project.getId());
                    return before + "|" + after;
                });
            });

            assertThat(firstRead.await(10, TimeUnit.SECONDS)).isTrue();
            jdbcTemplate.update("UPDATE casehub.projects SET project_name = 'Mutated concurrently' WHERE id = ?",
                    project.getId());
            allowSecondRead.countDown();
            assertThat(names.get(10, TimeUnit.SECONDS)).isEqualTo("Export Project|Export Project");
        } finally {
            executor.shutdownNow();
        }
    }

    private UserPrincipal principal(UserEntity user, String role) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), user.getDisplayName(),
                true, false, Set.of(role), Set.of("export:project", "project:read"));
    }
}
