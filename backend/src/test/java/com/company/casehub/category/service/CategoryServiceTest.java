package com.company.casehub.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.category.dto.CategoryCreateRequest;
import com.company.casehub.category.dto.CategoryUpdateRequest;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for the two-level category hierarchy rules (no Spring context, no DB).
 *
 * <p>{@code level} is derived server-side from {@code parentId}: no parent ⇒ level 1,
 * a level-1 parent ⇒ level 2. Everything that would create a third level, a self
 * reference or a cycle must be rejected with {@code CATEGORY_PARENT_INVALID}.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryService service;

    private static CategoryEntity category(String code, String name, int level) {
        CategoryEntity entity = new CategoryEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setLevel(level);
        entity.setEnabled(true);
        return entity;
    }

    @Test
    void createWithoutParentProducesLevel1() {
        when(repository.existsByCodeIgnoreCase("CAT-ROOT")).thenReturn(false);
        when(repository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryCreateRequest request =
                new CategoryCreateRequest("CAT-ROOT", "Network Protocols", null, null, 0, null);

        service.create(request);

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(repository).save(captor.capture());
        CategoryEntity saved = captor.getValue();
        assertThat(saved.getParent()).isNull();
        assertThat(saved.getLevel()).isEqualTo(1);
        assertThat(saved.getSortOrder()).isZero();
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void createWithLevel1ParentProducesLevel2() {
        UUID parentId = UUID.randomUUID();
        CategoryEntity parent = category("CAT-ROOT", "Network Protocols", 1);
        parent.setId(parentId);
        when(repository.findById(parentId)).thenReturn(Optional.of(parent));
        when(repository.existsByCodeIgnoreCase("CAT-CHILD")).thenReturn(false);
        when(repository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryCreateRequest request =
                new CategoryCreateRequest("CAT-CHILD", "Modbus", parentId, null, 3, null);

        var response = service.create(request);

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(repository).save(captor.capture());
        CategoryEntity saved = captor.getValue();
        assertThat(saved.getParent()).isSameAs(parent);
        assertThat(saved.getLevel()).isEqualTo(2);
        assertThat(saved.getSortOrder()).isEqualTo(3);
        assertThat(response.level()).isEqualTo(2);
        assertThat(response.parentId()).isEqualTo(parentId);
    }

    @Test
    void createUnderLevel2ParentIsRejectedAsThirdLevel() {
        UUID parentId = UUID.randomUUID();
        CategoryEntity level2Parent = category("CAT-CHILD", "Modbus", 2);
        when(repository.findById(parentId)).thenReturn(Optional.of(level2Parent));

        CategoryCreateRequest request =
                new CategoryCreateRequest("CAT-DEEP", "Too Deep", parentId, null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_PARENT_INVALID);

        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateCodeCaseInsensitively() {
        when(repository.existsByCodeIgnoreCase("cat-root")).thenReturn(true);

        CategoryCreateRequest request =
                new CategoryCreateRequest("cat-root", "Network Protocols", null, null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_CODE_DUPLICATE);

        verify(repository, never()).save(any());
    }

    @Test
    void updateWithSelfAsParentIsRejected() {
        UUID id = UUID.randomUUID();
        CategoryEntity entity = category("CAT-ROOT", "Network Protocols", 1);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        CategoryUpdateRequest request = new CategoryUpdateRequest(null, null, id, null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_PARENT_INVALID);

        verify(repository, never()).save(any());
    }

    @Test
    void updateWithUnknownParentIsRejected() {
        UUID id = UUID.randomUUID();
        UUID unknownParentId = UUID.randomUUID();
        CategoryEntity entity = category("CAT-ROOT", "Network Protocols", 1);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findById(unknownParentId)).thenReturn(Optional.empty());

        CategoryUpdateRequest request =
                new CategoryUpdateRequest(null, null, unknownParentId, null, null, null);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(repository, never()).save(any());
    }

    @Test
    void updateMovingCategoryUnderItsOwnChildIsRejected() {
        // Chain: A (level 1) -> B (level 2). Moving A under B would make B A's ancestor.
        CategoryEntity a = category("CAT-A", "Root A", 1);
        CategoryEntity b = category("CAT-B", "Child B", 2);
        b.setParent(a);

        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        when(repository.findById(aId)).thenReturn(Optional.of(a));
        when(repository.findById(bId)).thenReturn(Optional.of(b));

        CategoryUpdateRequest request = new CategoryUpdateRequest(null, null, bId, null, null, null);

        assertThatThrownBy(() -> service.update(aId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_PARENT_INVALID);

        verify(repository, never()).save(any());
    }

    @Test
    void updateCreatingACycleThroughAncestorWalkIsRejected() {
        // Defensive guard: even if the two-level invariant slipped, the ancestor walk must
        // break the cycle. Hand-built state: A (level 1) is reachable as an ancestor of the
        // proposed level-1 parent P (P.parent == A). Re-parenting A under P would close a cycle.
        CategoryEntity a = category("CAT-A", "Root A", 1);
        CategoryEntity p = category("CAT-P", "Root P", 1);
        p.setParent(a);

        UUID aId = UUID.randomUUID();
        UUID pId = UUID.randomUUID();
        a.setId(aId);
        p.setId(pId);

        when(repository.findById(aId)).thenReturn(Optional.of(a));
        when(repository.findById(pId)).thenReturn(Optional.of(p));

        CategoryUpdateRequest request = new CategoryUpdateRequest(null, null, pId, null, null, null);

        assertThatThrownBy(() -> service.update(aId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_PARENT_INVALID);

        verify(repository, never()).save(any());
    }
}
