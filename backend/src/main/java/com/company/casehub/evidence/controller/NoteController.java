package com.company.casehub.evidence.controller;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.evidence.dto.NoteRequest;
import com.company.casehub.evidence.dto.NoteResponse;
import com.company.casehub.evidence.service.NoteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/project-test-cases/{projectTestCaseId}/notes")
public class NoteController {
    private final NoteService service;
    public NoteController(NoteService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('note:read')")
    public List<NoteResponse> list(@PathVariable UUID projectTestCaseId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.list(projectTestCaseId, principal);
    }
    @PostMapping
    @PreAuthorize("hasAuthority('note:create')")
    public ResponseEntity<NoteResponse> create(@PathVariable UUID projectTestCaseId, @Valid @RequestBody NoteRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(projectTestCaseId, request, principal));
    }
    @PatchMapping("/{noteId}")
    @PreAuthorize("hasAuthority('note:update_own')")
    public NoteResponse update(@PathVariable UUID noteId, @Valid @RequestBody NoteRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) { return service.update(noteId, request, principal); }
    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasAuthority('note:delete_own')")
    public ResponseEntity<Void> delete(@PathVariable UUID noteId, @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(noteId, principal); return ResponseEntity.noContent().build();
    }
}
