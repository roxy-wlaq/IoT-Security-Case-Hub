package com.company.casehub.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Phase6ApiContractDocumentationTest {

    private static final Path CONTRACT = Path.of("..", "docs", "phase6-api-contract.md");

    @Test
    void documentsTheActualPhase6ControllerSurface() throws Exception {
        String document = Files.readString(CONTRACT);

        assertThat(document).contains(
                "GET /api/v1/test-cases",
                "POST /api/v1/test-cases",
                "GET /api/v1/test-cases/{masterId}",
                "PUT /api/v1/test-cases/{masterId}/draft",
                "GET /api/v1/test-cases/{masterId}/versions",
                "GET /api/v1/test-cases/{masterId}/versions/{versionId}",
                "sort=caseName,asc",
                "totalElements",
                "mappingNote",
                "progressiveRole");
        assertThat(document).doesNotContain("/publish", "/review", "/reject", "/deprecate", "/decision-points");
    }
}
