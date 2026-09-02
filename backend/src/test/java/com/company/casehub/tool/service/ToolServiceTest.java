package com.company.casehub.tool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.tool.dto.ToolCreateRequest;
import com.company.casehub.tool.dto.ToolUpdateRequest;
import com.company.casehub.tool.entity.ToolEntity;
import com.company.casehub.tool.repository.ToolRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for {@link ToolService} (no Spring context, no DB).
 *
 * <p>{@code code} and {@code name} are each unique case-insensitively and carry their
 * own frozen error codes so the client can point the user at the offending field.
 */
@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository repository;

    @InjectMocks
    private ToolService service;

    @Test
    void createRejectsDuplicateCodeCaseInsensitively() {
        // service 原样传递 code，唯一性由数据库 lower 索引保证；
        // 这里用小写请求验证与已存在的大写 code 冲突（stub 模拟 DB 命中）。
        when(repository.existsByCodeIgnoreCase("tool-nmap")).thenReturn(true);

        ToolCreateRequest request = new ToolCreateRequest("tool-nmap", "Nmap", null, null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOOL_CODE_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNameCaseInsensitively() {
        when(repository.existsByCodeIgnoreCase("TOOL-NMAP")).thenReturn(false);
        when(repository.existsByNameIgnoreCase("nmap")).thenReturn(true);

        ToolCreateRequest request = new ToolCreateRequest("TOOL-NMAP", "nmap", null, null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOOL_NAME_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void createTrimsValuesAndDefaultsEnabledToTrue() {
        when(repository.existsByCodeIgnoreCase("TOOL-NMAP")).thenReturn(false);
        when(repository.existsByNameIgnoreCase("Nmap")).thenReturn(false);
        when(repository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ToolCreateRequest request =
                new ToolCreateRequest("  TOOL-NMAP  ", "  Nmap  ", "  Port scanner  ", "  Cross-platform  ", "  ", null);

        var response = service.create(request);

        ArgumentCaptor<ToolEntity> captor = ArgumentCaptor.forClass(ToolEntity.class);
        verify(repository).save(captor.capture());
        ToolEntity saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("TOOL-NMAP");
        assertThat(saved.getName()).isEqualTo("Nmap");
        assertThat(saved.getDescription()).isEqualTo("Port scanner");
        assertThat(saved.getPlatform()).isEqualTo("Cross-platform");
        // blank optional strings normalize to null instead of empty strings
        assertThat(saved.getWebsite()).isNull();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(response.code()).isEqualTo("TOOL-NMAP");
    }

    @Test
    void getReturnsToolById() {
        UUID id = UUID.randomUUID();
        ToolEntity entity = new ToolEntity();
        entity.setCode("TOOL-NMAP");
        entity.setName("Nmap");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        var response = service.get(id);

        assertThat(response.code()).isEqualTo("TOOL-NMAP");
        assertThat(response.name()).isEqualTo("Nmap");
    }

    @Test
    void getRejectsUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOOL_NOT_FOUND);
    }

    @Test
    void updateRejectsDuplicateCodeOnAnotherEntity() {
        UUID id = UUID.randomUUID();
        ToolEntity entity = new ToolEntity();
        entity.setCode("TOOL-NMAP");
        entity.setName("Nmap");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByCodeIgnoreCaseAndIdNot("tool-wireshark", id)).thenReturn(true);

        ToolUpdateRequest request = new ToolUpdateRequest("tool-wireshark", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOOL_CODE_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsDuplicateNameOnAnotherEntity() {
        UUID id = UUID.randomUUID();
        ToolEntity entity = new ToolEntity();
        entity.setCode("TOOL-NMAP");
        entity.setName("Nmap");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByNameIgnoreCaseAndIdNot("wireshark", id)).thenReturn(true);

        ToolUpdateRequest request = new ToolUpdateRequest(null, "wireshark", null, null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOOL_NAME_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        ToolUpdateRequest request = new ToolUpdateRequest(null, "Wireshark", null, null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOOL_NOT_FOUND);

        verify(repository, never()).save(any());
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        ToolEntity entity = new ToolEntity();
        entity.setCode("TOOL-NMAP");
        entity.setName("Nmap");
        entity.setDescription("old");
        entity.setPlatform("Linux");
        entity.setWebsite("https://nmap.org");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByCodeIgnoreCaseAndIdNot("TOOL-NMAP", id)).thenReturn(false);
        when(repository.existsByNameIgnoreCaseAndIdNot("Nmap", id)).thenReturn(false);
        when(repository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ToolUpdateRequest request = new ToolUpdateRequest("  TOOL-NMAP  ", "  Nmap  ", "  New description  ", null, "  ", false);

        service.update(id, request);

        assertThat(entity.getCode()).isEqualTo("TOOL-NMAP");
        assertThat(entity.getName()).isEqualTo("Nmap");
        assertThat(entity.getDescription()).isEqualTo("New description");
        // platform == null means "leave unchanged"; blank website clears to null
        assertThat(entity.getPlatform()).isEqualTo("Linux");
        assertThat(entity.getWebsite()).isNull();
        assertThat(entity.isEnabled()).isFalse();
    }
}
