package com.company.casehub.export.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.export.dto.ProjectExportSnapshot;
import com.company.casehub.export.service.ProjectExportService;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/export.xlsx")
public class ProjectExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ProjectExportService service;

    public ProjectExportController(ProjectExportService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('export:project')")
    public ResponseEntity<StreamingResponseBody> export(@PathVariable UUID projectId,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        ProjectExportSnapshot snapshot = service.snapshot(projectId, principal);
        StreamingResponseBody body = output -> write(snapshot, output);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(snapshot.projectNumber() + ".xlsx").build().toString())
                .body(body);
    }

    private void write(ProjectExportSnapshot snapshot, java.io.OutputStream output) throws IOException {
        service.write(snapshot, output);
    }
}
