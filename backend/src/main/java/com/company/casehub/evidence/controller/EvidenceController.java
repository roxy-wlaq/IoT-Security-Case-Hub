package com.company.casehub.evidence.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.evidence.dto.EvidenceResponse;
import com.company.casehub.evidence.service.EvidenceService;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/project-test-cases/{projectTestCaseId}/evidence")
public class EvidenceController {
    private final EvidenceService service;
    public EvidenceController(EvidenceService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('evidence:read')")
    public List<EvidenceResponse> list(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.list(projectTestCaseId, principal);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('evidence:upload')")
    public EvidenceResponse upload(@PathVariable UUID projectTestCaseId, @RequestPart("file") MultipartFile file,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return service.upload(projectTestCaseId, file, principal);
    }

    @GetMapping("/{evidenceId}/download")
    @PreAuthorize("hasAuthority('evidence:read')")
    public ResponseEntity<ByteArrayResource> download(@PathVariable UUID evidenceId, @AuthenticationPrincipal UserPrincipal principal) {
        EvidenceService.Download download = service.download(evidenceId, principal);
        MediaType mediaType = download.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(download.contentType());
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.filename()).build().toString())
                .body(new ByteArrayResource(download.bytes()));
    }

    @DeleteMapping("/{evidenceId}")
    @PreAuthorize("hasAuthority('evidence:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID evidenceId, @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(evidenceId, principal); return ResponseEntity.noContent().build();
    }
}
