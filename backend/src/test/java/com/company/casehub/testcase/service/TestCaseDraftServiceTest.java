package com.company.casehub.testcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.UpdateDraftRequest;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseAttachmentRepository;
import com.company.casehub.testcase.repository.TestCaseStandardMappingRepository;
import com.company.casehub.testcase.repository.TestCaseTagRepository;
import com.company.casehub.testcase.repository.TestCaseToolRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.TestStepRepository;
import com.company.casehub.tag.entity.TagEntity;
import com.company.casehub.tag.repository.TagRepository;
import com.company.casehub.tool.entity.ToolEntity;
import com.company.casehub.tool.repository.ToolRepository;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestCaseDraftServiceTest {

    @Mock private MasterTestCaseRepository masterRepository;
    @Mock private TestCaseVersionRepository versionRepository;
    @Mock private TestStepRepository stepRepository;
    @Mock private TestCaseTagRepository caseTagRepository;
    @Mock private TestCaseToolRepository caseToolRepository;
    @Mock private TestCaseStandardMappingRepository mappingRepository;
    @Mock private TestCaseAttachmentRepository attachmentRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private StandardTaskTypeRepository standardRepository;
    @Mock private UserRepository userRepository;
    @Mock private TestCaseAccessPolicy accessPolicy;

    @InjectMocks private TestCaseDraftService service;

    @Test
    void createsStableMasterAndInitialDraftWithDeduplicatedSteps() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("coordinator", "Coordinator", "hash");
        user.setId(userId);
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID());
        category.setEnabled(true);
        UUID categoryId = category.getId();
        UserPrincipal principal = principal(userId, "TEST_COORDINATOR");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(masterRepository.existsByCaseCodeIgnoreCase("BLE-001")).thenReturn(false);
        when(masterRepository.save(any(MasterTestCaseEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createDraft(new CreateDraftRequest("BLE-001", categoryId, "Pairing", "purpose", null,
                SelectionMode.SINGLE, null, null, null, null,
                List.of(new StepRequest("one", "first"), new StepRequest("two", "second"), new StepRequest("two", "second")),
                null, null, null), principal);

        assertThat(response.visibleVersion().status()).isEqualTo("DRAFT");
        assertThat(response.visibleVersion().versionLabel()).isEqualTo("1.0");
        assertThat(response.visibleVersion().steps()).extracting("sequenceNo").containsExactly(1, 2, 3);
        assertThat(response.visibleVersion().steps()).extracting("content").containsExactly("first", "second", "second");
        verify(masterRepository).save(any(MasterTestCaseEntity.class));
    }

    @Test
    void rejectsDuplicateCaseCodeBeforeWriting() {
        UUID categoryId = UUID.randomUUID();
        when(masterRepository.existsByCaseCodeIgnoreCase("BLE-001")).thenReturn(true);

        assertThatThrownBy(() -> service.createDraft(new CreateDraftRequest("BLE-001", categoryId, "Pairing", null, null,
                SelectionMode.SINGLE, null, null, null, null, null, null, null, null), principal(UUID.randomUUID(), "TEST_COORDINATOR")))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_CODE_DUPLICATE);
        verify(masterRepository, never()).save(any());
    }

    @Test
    void onlyDraftOwnerOrAdminCanUpdateDraft() {
        UUID userId = UUID.randomUUID();
        UUID masterId = UUID.randomUUID();
        UserEntity owner = new UserEntity("owner", "Owner", "hash");
        owner.setId(userId);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setId(masterId);
        master.setCreatedBy(owner);
        TestCaseVersionEntity draft = new TestCaseVersionEntity();
        draft.setStatus(TestCaseVersionStatus.DRAFT);
        draft.setCreatedBy(owner);
        draft.setMasterTestCase(master);
        when(masterRepository.findById(masterId)).thenReturn(Optional.of(master));
        when(versionRepository.findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(masterId, TestCaseVersionStatus.DRAFT))
                .thenReturn(Optional.of(draft));
        when(accessPolicy.canEditDraft(draft, principal(userId, "TEST_COORDINATOR"))).thenReturn(true);

        var updated = service.updateDraft(masterId, new UpdateDraftRequest("Updated", null, null, SelectionMode.MULTIPLE,
                false, null, null, null, List.of(new StepRequest(null, "content")), null, null, null), principal(userId, "TEST_COORDINATOR"));

        assertThat(updated.visibleVersion().caseName()).isEqualTo("Updated");
        assertThat(draft.getSteps()).hasSize(1);
    }

    private static UserPrincipal principal(UUID id, String role) {
        return new UserPrincipal(id, "user", "hash", "User", true, false, Set.of(role), Set.of("test_case:draft_create", "test_case:draft_edit"));
    }
}
