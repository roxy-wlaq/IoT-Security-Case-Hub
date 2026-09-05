package com.company.casehub.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.casehub.export.dto.ProjectExportRow;
import com.company.casehub.export.dto.ProjectExportSnapshot;
import com.company.casehub.export.service.ProjectExportService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ProjectExportServiceTest {

    @Test
    void writesFixedSheetsAndPreservesMasterCustomAndFormulaSafeValues() throws Exception {
        UUID masterPtc = UUID.randomUUID();
        UUID customPtc = UUID.randomUUID();
        ProjectExportSnapshot snapshot = new ProjectExportSnapshot(
                "PRJ-1", "=Project", "Device", "FULL", "DRAFT", "Admin",
                Instant.parse("2026-01-01T00:00:00Z"), "STD · Standard",
                List.of(
                        new ProjectExportRow(masterPtc, "MASTER", UUID.randomUUID(), null, "+MASTER", "Master Case",
                                "INITIAL, GENERATED", UUID.randomUUID(), "1.2", "COMPLETED", "CONNECTED", false,
                                "Tester (tester)", 1),
                        new ProjectExportRow(customPtc, "CUSTOM", null, UUID.randomUUID(), "CUSTOM-1", "Custom Case",
                                "CUSTOM", null, null, "IN_PROGRESS", "FLOATING", true, "", 0)),
                List.of(new ProjectExportSnapshot.EvidenceRow(UUID.randomUUID(), masterPtc, "@evidence.txt",
                        "text/plain", 5, "sha", "Tester", Instant.parse("2026-01-01T00:00:00Z"))));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ProjectExportService(null, null, null, null).write(snapshot, output);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertThat(workbook.getSheetIndex("Project Summary")).isGreaterThanOrEqualTo(0);
            assertThat(workbook.getSheetIndex("Test Cases")).isGreaterThanOrEqualTo(0);
            assertThat(workbook.getSheetIndex("Evidence Index")).isGreaterThanOrEqualTo(0);
            assertThat(workbook.getSheet("Project Summary").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Project Number");
            assertThat(workbook.getSheet("Test Cases").getRow(0).getCell(1).getStringCellValue())
                    .isEqualTo("Backing Type");
            assertThat(workbook.getSheet("Test Cases").getRow(0).getCell(6).getStringCellValue())
                    .isEqualTo("Plan Sources");
            assertThat(workbook.getSheet("Project Summary").getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo("'=Project");
            assertThat(workbook.getSheet("Test Cases").getRow(1).getCell(4).getStringCellValue())
                    .isEqualTo("'+MASTER");
            assertThat(workbook.getSheet("Evidence Index").getRow(1).getCell(2).getStringCellValue())
                    .isEqualTo("'@evidence.txt");
        }
    }

    @Test
    void writesLargeRowExportUsingStreamingWriter() throws Exception {
        List<ProjectExportRow> rows = IntStream.range(0, 3000)
                .mapToObj(i -> new ProjectExportRow(UUID.randomUUID(), "MASTER", UUID.randomUUID(), null,
                        "CASE-" + i, "Case " + i, "GENERATED", UUID.randomUUID(), "1.0", "NOT_STARTED",
                        "FLOATING", false, "", 0))
                .toList();
        ProjectExportSnapshot snapshot = new ProjectExportSnapshot(
                "PRJ-LARGE", "Large", "Device", "FULL", "DRAFT", "Admin", Instant.now(), "", rows, List.of());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ProjectExportService(null, null, null, null).write(snapshot, output);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertThat(workbook.getSheet("Test Cases").getLastRowNum()).isEqualTo(3000);
            assertThat(workbook.getSheet("Test Cases").getRow(3000).getCell(4).getStringCellValue())
                    .isEqualTo("CASE-2999");
        }
    }
}
