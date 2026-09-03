package com.company.casehub.evidence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.evidence.dto.NoteRequest;
import com.company.casehub.evidence.entity.NoteEntity;
import com.company.casehub.evidence.repository.NoteRepository;
import com.company.casehub.evidence.service.NoteService;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceAuthorizationTest {
    @Mock private NoteRepository noteRepository;
    @Mock private ProjectTestCaseRepository testCaseRepository;
    @Mock private ProjectTestCaseAssigneeRepository assigneeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectAccessPolicy accessPolicy;
    private NoteService service;
    private UserPrincipal tester;
    private ProjectTestCaseEntity ptc;

    @BeforeEach
    void setUp() {
        service = new NoteService(noteRepository, testCaseRepository, assigneeRepository, userRepository, accessPolicy);
        tester = new UserPrincipal(UUID.randomUUID(), "tester", "hash", "Tester", true, false, Set.of("TESTER"), Set.of("note:read"));
        ProjectEntity project = new ProjectEntity(); project.setId(UUID.randomUUID());
        ptc = new ProjectTestCaseEntity(); ptc.setId(UUID.randomUUID()); ptc.setProject(project);
    }

    @Test
    void onlyAssignedUserCanCreate() {
        when(testCaseRepository.findById(ptc.getId())).thenReturn(Optional.of(ptc));
        when(assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), tester.getId())).thenReturn(false);
        assertThatThrownBy(() -> service.create(ptc.getId(), new NoteRequest("note"), tester))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void anotherUserCannotUpdateOrDeleteAuthorsNote() {
        UserEntity author = new UserEntity("author", "Author", "hash");
        author.setId(UUID.randomUUID());
        NoteEntity note = new NoteEntity(); note.setId(UUID.randomUUID()); note.setAuthor(author); note.setProjectTestCase(ptc);
        when(noteRepository.findById(note.getId())).thenReturn(Optional.of(note));
        assertThatThrownBy(() -> service.update(note.getId(), new NoteRequest("changed"), tester))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.delete(note.getId(), tester))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
