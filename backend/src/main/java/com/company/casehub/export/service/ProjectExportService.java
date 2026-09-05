package com.company.casehub.export.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.evidence.entity.EvidenceEntity;
import com.company.casehub.evidence.repository.EvidenceRepository;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.export.dto.ProjectExportRow;
import com.company.casehub.export.dto.ProjectExportSnapshot;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectExportService {

    private static final String[] SUMMARY_HEADERS = {
            "Project Number", "Project Name", "Device Name", "Generation Mode", "Status",
            "Created By", "Created At", "Standards"
    };
    private static final String[] TEST_CASE_HEADERS = {
            "Project Test Case ID", "Backing Type", "Master Test Case ID", "Custom Test Case ID",
            "Case Code", "Case Name", "Plan Sources", "Bound Version ID", "Version",
            "Execution Status", "Relation Status", "Removed", "Assignees", "Evidence Count"
    };
    private static final String[] EVIDENCE_HEADERS = {
            "Evidence ID", "Project Test Case ID", "Original Filename", "Content Type",
            "File Size", "SHA-256", "Uploaded By", "Created At"
    };

    private final ProjectRepository projectRepository;
    private final ProjectTestCaseRepository projectTestCaseRepository;
    private final EvidenceRepository evidenceRepository;
    private final ProjectAccessPolicy accessPolicy;

    public ProjectExportService(ProjectRepository projectRepository,
                                ProjectTestCaseRepository projectTestCaseRepository,
                                EvidenceRepository evidenceRepository,
                                ProjectAccessPolicy accessPolicy) {
        this.projectRepository = projectRepository;
        this.projectTestCaseRepository = projectTestCaseRepository;
        this.evidenceRepository = evidenceRepository;
        this.accessPolicy = accessPolicy;
    }

    /**
     * Materializes all export data in one PostgreSQL snapshot before response streaming begins.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProjectExportSnapshot snapshot(UUID projectId, UserPrincipal principal) {
        accessPolicy.requireView(projectId, principal);
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND,
                        "Project not found: " + projectId));

        List<ProjectTestCaseEntity> testCases = projectTestCaseRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<ProjectExportRow> rows = new ArrayList<>(testCases.size());
        List<ProjectExportSnapshot.EvidenceRow> evidenceRows = new ArrayList<>();
        for (ProjectTestCaseEntity testCase : testCases) {
            rows.add(toTestCaseRow(testCase));
            for (EvidenceEntity evidence : evidenceRepository
                    .findByProjectTestCaseIdOrderByCreatedAtAsc(testCase.getId())) {
                evidenceRows.add(new ProjectExportSnapshot.EvidenceRow(
                        evidence.getId(), testCase.getId(), evidence.getOriginalFilename(),
                        evidence.getContentType(), evidence.getFileSize(), evidence.getSha256(),
                        evidence.getUploadedBy().getDisplayName(), evidence.getCreatedAt()));
            }
        }

        String standards = project.getStandards().stream()
                .map(link -> link.getStandardTaskType().getCode() + " · " + link.getStandardTaskType().getName())
                .collect(Collectors.joining(", "));
        return new ProjectExportSnapshot(project.getProjectNumber(), project.getProjectName(), project.getDeviceName(),
                project.getGenerationMode().name(), project.getStatus().name(),
                project.getCreatedBy().getDisplayName(), project.getCreatedAt(), standards, rows, evidenceRows);
    }

    public void write(ProjectExportSnapshot snapshot, OutputStream output) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try {
            CellStyle headerStyle = headerStyle(workbook);
            writeSummary(workbook.createSheet("Project Summary"), snapshot, headerStyle);
            writeTestCases(workbook.createSheet("Test Cases"), snapshot.testCases(), headerStyle);
            writeEvidence(workbook.createSheet("Evidence Index"), snapshot.evidence(), headerStyle);
            workbook.write(output);
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }

    private ProjectExportRow toTestCaseRow(ProjectTestCaseEntity testCase) {
        boolean custom = testCase.getCustomTestCase() != null;
        String version = testCase.getTestCaseVersion() == null ? null
                : testCase.getTestCaseVersion().getVersionMajor() + "." + testCase.getTestCaseVersion().getVersionMinor();
        String sources = testCase.getSources().stream()
                .map(source -> source.getSourceType().name())
                .sorted()
                .collect(Collectors.joining(", "));
        String assignees = testCase.getAssignees().stream()
                .map(assignee -> assignee.getUser().getDisplayName() + " (" + assignee.getUser().getUsername() + ")")
                .sorted()
                .collect(Collectors.joining(", "));
        String caseCode = custom ? testCase.getCustomTestCase().getCaseCode()
                : testCase.getMasterTestCase() == null ? null : testCase.getMasterTestCase().getCaseCode();
        String caseName = custom ? testCase.getCustomTestCase().getCaseName()
                : testCase.getTestCaseVersion() == null ? null : testCase.getTestCaseVersion().getCaseName();
        return new ProjectExportRow(testCase.getId(), custom ? "CUSTOM" : "MASTER",
                testCase.getMasterTestCase() == null ? null : testCase.getMasterTestCase().getId(),
                testCase.getCustomTestCase() == null ? null : testCase.getCustomTestCase().getId(),
                caseCode, caseName, sources,
                testCase.getTestCaseVersion() == null ? null : testCase.getTestCaseVersion().getId(), version,
                testCase.getExecutionStatus().name(), testCase.getRelationStatus().name(), testCase.isRemoved(),
                assignees, evidenceRepository.countByProjectTestCaseId(testCase.getId()));
    }

    private CellStyle headerStyle(SXSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeSummary(Sheet sheet, ProjectExportSnapshot snapshot, CellStyle headerStyle) {
        writeHeaders(sheet, SUMMARY_HEADERS, headerStyle);
        Row row = sheet.createRow(1);
        writeText(row, 0, snapshot.projectNumber());
        writeText(row, 1, snapshot.projectName());
        writeText(row, 2, snapshot.deviceName());
        writeText(row, 3, snapshot.generationMode());
        writeText(row, 4, snapshot.status());
        writeText(row, 5, snapshot.createdBy());
        writeText(row, 6, instant(snapshot.createdAt()));
        writeText(row, 7, snapshot.standards());
    }

    private void writeTestCases(Sheet sheet, List<ProjectExportRow> rows, CellStyle headerStyle) {
        writeHeaders(sheet, TEST_CASE_HEADERS, headerStyle);
        int rowIndex = 1;
        for (ProjectExportRow value : rows) {
            Row row = sheet.createRow(rowIndex++);
            writeText(row, 0, uuid(value.projectTestCaseId()));
            writeText(row, 1, value.backingType());
            writeText(row, 2, uuid(value.masterTestCaseId()));
            writeText(row, 3, uuid(value.customTestCaseId()));
            writeText(row, 4, value.caseCode());
            writeText(row, 5, value.caseName());
            writeText(row, 6, value.planSources());
            writeText(row, 7, uuid(value.boundVersionId()));
            writeText(row, 8, value.version());
            writeText(row, 9, value.executionStatus());
            writeText(row, 10, value.relationStatus());
            row.createCell(11).setCellValue(value.removed());
            writeText(row, 12, value.assignees());
            row.createCell(13).setCellValue(value.evidenceCount());
        }
    }

    private void writeEvidence(Sheet sheet, List<ProjectExportSnapshot.EvidenceRow> rows, CellStyle headerStyle) {
        writeHeaders(sheet, EVIDENCE_HEADERS, headerStyle);
        int rowIndex = 1;
        for (ProjectExportSnapshot.EvidenceRow value : rows) {
            Row row = sheet.createRow(rowIndex++);
            writeText(row, 0, uuid(value.evidenceId()));
            writeText(row, 1, uuid(value.projectTestCaseId()));
            writeText(row, 2, value.originalFilename());
            writeText(row, 3, value.contentType());
            row.createCell(4).setCellValue(value.fileSize());
            writeText(row, 5, value.sha256());
            writeText(row, 6, value.uploadedBy());
            writeText(row, 7, instant(value.createdAt()));
        }
    }

    private void writeHeaders(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeText(Row row, int column, String value) {
        row.createCell(column).setCellValue(ExcelCellSafety.text(value));
    }

    private static String uuid(UUID value) {
        return value == null ? null : value.toString();
    }

    private static String instant(Instant value) {
        return value == null ? null : value.toString();
    }
}
