package com.company.casehub.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.tag.dto.TagCreateRequest;
import com.company.casehub.tag.dto.TagUpdateRequest;
import com.company.casehub.tag.entity.TagEntity;
import com.company.casehub.tag.repository.TagRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for {@link TagService} (no Spring context, no DB).
 *
 * <p>{@code code} and {@code name} are each unique case-insensitively and carry their
 * own frozen error codes so the client can point the user at the offending field.
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository repository;

    @InjectMocks
    private TagService service;

    @Test
    void createRejectsDuplicateCodeCaseInsensitively() {
        // service 原样传递 code，唯一性由数据库 lower 索引保证；
        // 这里用小写请求验证与已存在的大写 code 冲突（stub 模拟 DB 命中）。
        when(repository.existsByCodeIgnoreCase("tag-usb")).thenReturn(true);

        TagCreateRequest request = new TagCreateRequest("tag-usb", "USB", null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_CODE_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNameCaseInsensitively() {
        when(repository.existsByCodeIgnoreCase("TAG-USB")).thenReturn(false);
        when(repository.existsByNameIgnoreCase("usb")).thenReturn(true);

        TagCreateRequest request = new TagCreateRequest("TAG-USB", "usb", null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_NAME_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void createTrimsValuesAndDefaultsEnabledToTrue() {
        when(repository.existsByCodeIgnoreCase("TAG-USB")).thenReturn(false);
        when(repository.existsByNameIgnoreCase("USB")).thenReturn(false);
        when(repository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TagCreateRequest request = new TagCreateRequest("  TAG-USB  ", "  USB  ", "  ", null);

        var response = service.create(request);

        ArgumentCaptor<TagEntity> captor = ArgumentCaptor.forClass(TagEntity.class);
        verify(repository).save(captor.capture());
        TagEntity saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("TAG-USB");
        assertThat(saved.getName()).isEqualTo("USB");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(response.code()).isEqualTo("TAG-USB");
    }

    @Test
    void updateRejectsDuplicateCodeOnAnotherEntity() {
        UUID id = UUID.randomUUID();
        TagEntity entity = new TagEntity();
        entity.setCode("TAG-USB");
        entity.setName("USB");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByCodeIgnoreCaseAndIdNot("tag-spi", id)).thenReturn(true);

        TagUpdateRequest request = new TagUpdateRequest("tag-spi", null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_CODE_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsDuplicateNameOnAnotherEntity() {
        UUID id = UUID.randomUUID();
        TagEntity entity = new TagEntity();
        entity.setCode("TAG-USB");
        entity.setName("USB");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByNameIgnoreCaseAndIdNot("spi", id)).thenReturn(true);

        TagUpdateRequest request = new TagUpdateRequest(null, "spi", null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_NAME_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        TagUpdateRequest request = new TagUpdateRequest(null, "SPI", null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAG_NOT_FOUND);

        verify(repository, never()).save(any());
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        TagEntity entity = new TagEntity();
        entity.setCode("TAG-USB");
        entity.setName("USB");
        entity.setDescription("old");
        entity.setEnabled(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByCodeIgnoreCaseAndIdNot("TAG-USB", id)).thenReturn(false);
        when(repository.existsByNameIgnoreCaseAndIdNot("USB", id)).thenReturn(false);
        when(repository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TagUpdateRequest request = new TagUpdateRequest("  TAG-USB  ", "  USB  ", null, false);

        service.update(id, request);

        assertThat(entity.getCode()).isEqualTo("TAG-USB");
        assertThat(entity.getName()).isEqualTo("USB");
        // description == null means "leave unchanged"
        assertThat(entity.getDescription()).isEqualTo("old");
        assertThat(entity.isEnabled()).isFalse();
    }
}
