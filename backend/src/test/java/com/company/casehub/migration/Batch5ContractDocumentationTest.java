package com.company.casehub.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Batch5ContractDocumentationTest {

    @Test
    void freezesBatch5ExportAndAuditContract() throws Exception {
        Path contract = Stream.of(Path.of("docs", "batch5-api-contract.md"),
                        Path.of("..", "docs", "batch5-api-contract.md"))
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("batch5-api-contract.md not found"));
        String document = Files.readString(contract);

        assertThat(document).contains(
                "GET /api/v1/projects/{projectId}/export.xlsx",
                "Project Summary",
                "Test Cases",
                "Evidence Index",
                "Backing Type",
                "Plan Sources",
                "REPEATABLE_READ",
                "ExcelCellSafety.text",
                "ASCII apostrophe",
                "GET /api/v1/audit-logs",
                "occurredAt DESC, id DESC",
                "recursive",
                "LOGIN_FAILURE",
                "V018");
        assertThat(document).doesNotContain("STATIC_REVIEW_PROMPT", "V019");
    }
}
