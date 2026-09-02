package com.company.casehub.standard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.standard.dto.StandardTaskTypeCreateRequest;
import com.company.casehub.standard.dto.StandardTaskTypeUpdateRequest;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for {@link StandardTaskTypeService} (no Spring context, no DB).
 *
 * <p>The critical invariant is the case-insensitive {@code code} uniqueness enforced by
 * the service itself, mirroring the {@code uq_standard_task_types_code_lower} constraint.
 */
@ExtendWith(MockitoExtension.class)
class StandardTaskTypeServiceTest {

    @Mock
    private StandardTaskTypeRepository repository;

    @InjectMocks
    private StandardTaskTypeService service;

    @Test
    void createRejectsDuplicateCodeCaseInsensitively() {
        // Stored row has code "STD-001"; the incoming request only differs in case.
        when(repository.existsByCodeIgnoreCase("std-001")).thenReturn(true);

        StandardTaskTypeCreateRequest request =
                new StandardTaskTypeCreateRequest("std-001", "USB Fuzzing", "STANDARD", null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STANDARD_CODE_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void createTrimsValuesAndDefaultsEnabledToTrue() {
        when(repository.existsByCodeIgnoreCase("STD-001")).thenReturn(false);
        when(repository.save(any(StandardTaskTypeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StandardTaskTypeCreateRequest request =
                new StandardTaskTypeCreateRequest("  STD-001  ", "  USB Fuzzing  ", "TASK_TYPE", "  ", null);

        var response = service.create(request);

        ArgumentCaptor<StandardTaskTypeEntity> captor = ArgumentCaptor.forClass(StandardTaskTypeEntity.class);
        verify(repository).save(captor.capture());
        StandardTaskTypeEntity saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("STD-001");
        assertThat(saved.getName()).isEqualTo("USB Fuzzing");
        assertThat(saved.getType()).isEqualTo("TASK_TYPE");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(response.code()).isEqualTo("STD-001");
    }

    @Test
    void updateRejectsDuplicateCodeOnAnotherEntity() {
        UUID id = UUID.randomUUID();
        StandardTaskTypeEntity entity = new StandardTaskTypeEntity();
        entity.setCode("STD-001");
        entity.setName("Old Name");
        entity.setType("STANDARD");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByCodeIgnoreCaseAndIdNot("std-002", id)).thenReturn(true);

        StandardTaskTypeUpdateRequest request = new StandardTaskTypeUpdateRequest("std-002", null, null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STANDARD_CODE_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        StandardTaskTypeUpdateRequest request = new StandardTaskTypeUpdateRequest(null, "New Name", null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STANDARD_NOT_FOUND);

        verify(repository, never()).save(any());
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        StandardTaskTypeEntity entity = new StandardTaskTypeEntity();
        entity.setCode("STD-001");
        entity.setName("Old Name");
        entity.setType("STANDARD");
        entity.setDescription("old description");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByCodeIgnoreCaseAndIdNot("STD-001", id)).thenReturn(false);
        when(repository.save(any(StandardTaskTypeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StandardTaskTypeUpdateRequest request =
                new StandardTaskTypeUpdateRequest("  STD-001  ", "  New Name  ", "TASK_TYPE", null, false);

        service.update(id, request);

        assertThat(entity.getCode()).isEqualTo("STD-001");
        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getType()).isEqualTo("TASK_TYPE");
        // description == null means "leave unchanged"
        assertThat(entity.getDescription()).isEqualTo("old description");
        assertThat(entity.isEnabled()).isFalse();
    }
}
