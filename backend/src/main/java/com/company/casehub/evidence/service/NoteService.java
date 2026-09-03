package com.company.casehub.evidence.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.evidence.dto.NoteRequest;
import com.company.casehub.evidence.dto.NoteResponse;
import com.company.casehub.evidence.entity.NoteEntity;
import com.company.casehub.evidence.repository.NoteRepository;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.project.service.ProjectAccessPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {
    private final NoteRepository noteRepository;
    private final ProjectTestCaseRepository testCaseRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final UserRepository userRepository;
    private final ProjectAccessPolicy accessPolicy;

    public NoteService(NoteRepository noteRepository, ProjectTestCaseRepository testCaseRepository,
                       ProjectTestCaseAssigneeRepository assigneeRepository, UserRepository userRepository,
                       ProjectAccessPolicy accessPolicy) {
        this.noteRepository = noteRepository;
        this.testCaseRepository = testCaseRepository;
        this.assigneeRepository = assigneeRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID ptcId, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = testCaseRepository.findById(ptcId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        if (!accessPolicy.canView(ptc.getProject().getId(), principal)) {
            throw new ForbiddenOperationException(ErrorCode.PROJECT_ACCESS_FORBIDDEN, "You are not a member of this Project");
        }
        return noteRepository.findByProjectTestCaseIdOrderByCreatedAtAsc(ptcId).stream().map(n -> toResponse(n, principal)).toList();
    }

    @Transactional
    public NoteResponse create(UUID ptcId, NoteRequest request, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = requireAssigned(ptcId, principal);
        NoteEntity note = new NoteEntity(); note.setProjectTestCase(ptc); note.setBody(request.body().trim());
        note.setAuthor(userRepository.findById(principal.getId()).orElseThrow());
        return toResponse(noteRepository.save(note), principal);
    }

    @Transactional
    public NoteResponse update(UUID noteId, NoteRequest request, UserPrincipal principal) {
        NoteEntity note = requireOwn(noteId, principal); note.setBody(request.body().trim());
        return toResponse(noteRepository.save(note), principal);
    }

    @Transactional
    public void delete(UUID noteId, UserPrincipal principal) {
        noteRepository.delete(requireOwn(noteId, principal));
    }

    private ProjectTestCaseEntity requireAssigned(UUID id, UserPrincipal principal) {
        ProjectTestCaseEntity ptc = testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_TEST_CASE_NOT_FOUND, "Project Test Case not found"));
        if (ptc.isRemoved() || !assigneeRepository.existsByProjectTestCaseIdAndUserId(id, principal.getId())) {
            throw new ForbiddenOperationException(ErrorCode.PROJECT_ACCESS_FORBIDDEN, "The case is not assigned to you");
        }
        return ptc;
    }

    private NoteEntity requireOwn(UUID id, UserPrincipal principal) {
        NoteEntity note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTE_NOT_FOUND, "Note not found"));
        if (!note.getAuthor().getId().equals(principal.getId())) {
            throw new ForbiddenOperationException(ErrorCode.NOTE_EDIT_FORBIDDEN, "You can only edit or delete your own Note");
        }
        return note;
    }

    private NoteResponse toResponse(NoteEntity n, UserPrincipal p) {
        return new NoteResponse(n.getId(), n.getProjectTestCase().getId(), n.getAuthor().getId(),
                n.getAuthor().getDisplayName(), n.getBody(), n.getCreatedAt(), n.getUpdatedAt(), n.getAuthor().getId().equals(p.getId()));
    }
}
