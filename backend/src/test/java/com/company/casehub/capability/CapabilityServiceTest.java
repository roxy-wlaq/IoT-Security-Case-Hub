package com.company.casehub.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.capability.dto.CapabilityResponse;
import com.company.casehub.capability.dto.CapabilityTreeNode;
import com.company.casehub.capability.dto.CreateCapabilityRequest;
import com.company.casehub.capability.dto.UpdateCapabilityRequest;
import com.company.casehub.capability.entity.CapabilityEntity;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.capability.service.CapabilityService;
import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the Capability Library. Pure JUnit 5 + Mockito: no Spring context and
 * no database, so these run everywhere (Testcontainers/PostgreSQL is not reachable on
 * every machine).
 */
@ExtendWith(MockitoExtension.class)
class CapabilityServiceTest {

    private static final UUID BLUETOOTH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PAIRING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BREDR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GATT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID MISSING_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock
    private CapabilityRepository capabilityRepository;

    private CapabilityService capabilityService;

    @BeforeEach
    void setUp() {
        capabilityService = new CapabilityService(capabilityRepository);
        // The service mutates the entity and reads the save() result; in the slice under
        // test save() is a no-op that hands the same instance back.
        lenient().when(capabilityRepository.save(any(CapabilityEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("Create root capability: parent is null, enabled defaults to true")
    void createRootCapabilitySucceeds() {
        when(capabilityRepository.existsByCodeIgnoreCase("BLUETOOTH")).thenReturn(false);

        CapabilityResponse response = capabilityService.create(
                new CreateCapabilityRequest(null, "BLUETOOTH", "Bluetooth", "Short range radio", 5));

        assertThat(response.parentId()).isNull();
        assertThat(response.code()).isEqualTo("BLUETOOTH");
        assertThat(response.name()).isEqualTo("Bluetooth");
        assertThat(response.description()).isEqualTo("Short range radio");
        assertThat(response.sortOrder()).isEqualTo(5);
        assertThat(response.enabled()).isTrue();

        ArgumentCaptor<CapabilityEntity> saved = ArgumentCaptor.forClass(CapabilityEntity.class);
        verify(capabilityRepository).save(saved.capture());
        assertThat(saved.getValue().getParentId()).isNull();
        // A root create has nothing to look up.
        verify(capabilityRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Create child capability: parent is resolved and linked")
    void createChildCapabilitySucceeds() {
        givenStored(node(BLUETOOTH_ID, "BLUETOOTH", null));

        CapabilityResponse response = capabilityService.create(
                new CreateCapabilityRequest(BLUETOOTH_ID, "BLE", "BLE", "  ", null));

        assertThat(response.parentId()).isEqualTo(BLUETOOTH_ID);
        assertThat(response.code()).isEqualTo("BLE");
        // Blank descriptions are normalised to null rather than stored as whitespace.
        assertThat(response.description()).isNull();
        // sortOrder omitted -> default 0
        assertThat(response.sortOrder()).isZero();
    }

    @Test
    @DisplayName("Create with an unknown parent -> CAPABILITY_PARENT_INVALID")
    void createWithUnknownParentIsRejected() {
        when(capabilityRepository.existsByCodeIgnoreCase("ORPHAN")).thenReturn(false);
        when(capabilityRepository.existsById(MISSING_ID)).thenReturn(false);

        assertThatThrownBy(() -> capabilityService.create(
                new CreateCapabilityRequest(MISSING_ID, "ORPHAN", "Orphan", null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_PARENT_INVALID);
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("Update capability: code, name, description and sort order are replaced")
    void updateCapabilitySucceeds() {
        givenStored(
                node(BLUETOOTH_ID, "BLUETOOTH", null),
                node(BLE_ID, "BLE", BLUETOOTH_ID));

        CapabilityResponse response = capabilityService.update(BLE_ID, new UpdateCapabilityRequest(
                BLUETOOTH_ID, "BLE_LOW_ENERGY", "BLE (Low Energy)", "Updated description", 7));

        assertThat(response.id()).isEqualTo(BLE_ID);
        assertThat(response.parentId()).isEqualTo(BLUETOOTH_ID);
        assertThat(response.code()).isEqualTo("BLE_LOW_ENERGY");
        assertThat(response.name()).isEqualTo("BLE (Low Energy)");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.sortOrder()).isEqualTo(7);
    }

    @Test
    @DisplayName("Update an unknown capability -> CAPABILITY_NOT_FOUND")
    void updateUnknownCapabilityIsRejected() {
        when(capabilityRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> capabilityService.update(MISSING_ID,
                new UpdateCapabilityRequest(null, "X", "X", null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_NOT_FOUND);
    }

    // ---------------------------------------------------------------- enable / disable

    @Test
    @DisplayName("Enable capability: enabled becomes true")
    void enableCapabilitySucceeds() {
        CapabilityEntity disabled = node(BLE_ID, "BLE", BLUETOOTH_ID);
        disabled.setEnabled(false);
        givenStored(disabled);

        CapabilityResponse response = capabilityService.enable(BLE_ID);

        assertThat(response.enabled()).isTrue();

        ArgumentCaptor<CapabilityEntity> saved = ArgumentCaptor.forClass(CapabilityEntity.class);
        verify(capabilityRepository).save(saved.capture());
        assertThat(saved.getValue().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Disable capability: enabled becomes false and the row is never deleted")
    void disableCapabilityRetiresWithoutDeleting() {
        CapabilityEntity ble = node(BLE_ID, "BLE", BLUETOOTH_ID);
        CapabilityEntity pairing = node(PAIRING_ID, "PAIRING", BLE_ID);
        givenStored(ble, pairing);

        CapabilityResponse response = capabilityService.disable(BLE_ID);

        assertThat(response.enabled()).isFalse();

        ArgumentCaptor<CapabilityEntity> saved = ArgumentCaptor.forClass(CapabilityEntity.class);
        verify(capabilityRepository).save(saved.capture());
        assertThat(saved.getValue().isEnabled()).isFalse();

        // Retirement never cascades: only the node itself is written...
        verify(capabilityRepository, times(1)).save(any(CapabilityEntity.class));
        // ...so the child keeps its own flag.
        assertThat(pairing.isEnabled()).isTrue();

        // Capabilities are retired, never physically removed (historical references).
        verify(capabilityRepository, never()).delete(any(CapabilityEntity.class));
        verify(capabilityRepository, never()).deleteById(any(UUID.class));
        verify(capabilityRepository, never()).deleteAll(anyCollection());
    }

    @Test
    @DisplayName("Disable an unknown capability -> CAPABILITY_NOT_FOUND")
    void disableUnknownCapabilityIsRejected() {
        when(capabilityRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> capabilityService.disable(MISSING_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_NOT_FOUND);
    }

    // ---------------------------------------------------------------- cycles

    @Test
    @DisplayName("Self cycle: A.parent = A is rejected")
    void selfCycleIsRejected() {
        givenStored(node(BLUETOOTH_ID, "BLUETOOTH", null));

        assertThatThrownBy(() -> capabilityService.update(BLUETOOTH_ID,
                new UpdateCapabilityRequest(BLUETOOTH_ID, "BLUETOOTH", "Bluetooth", null, 0)))
                .isInstanceOf(ConflictException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_CYCLE_DETECTED);

        verify(capabilityRepository, never()).save(any(CapabilityEntity.class));
    }

    @Test
    @DisplayName("Two-node cycle: A -> B then A.parent = B is rejected")
    void twoNodeCycleIsRejected() {
        // Existing shape: BLUETOOTH (root) -> BLE
        givenStored(node(BLUETOOTH_ID, "BLUETOOTH", null), node(BLE_ID, "BLE", BLUETOOTH_ID));

        // Now try to make BLUETOOTH a child of its own descendant.
        assertThatThrownBy(() -> capabilityService.update(BLUETOOTH_ID,
                new UpdateCapabilityRequest(BLE_ID, "BLUETOOTH", "Bluetooth", null, 0)))
                .isInstanceOf(ConflictException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_CYCLE_DETECTED);

        verify(capabilityRepository, never()).save(any(CapabilityEntity.class));
    }

    @Test
    @DisplayName("Deep cycle: A -> B -> C then A.parent = C is rejected")
    void deepCycleIsRejected() {
        // Existing shape: BLUETOOTH -> BLE -> PAIRING
        givenStored(
                node(BLUETOOTH_ID, "BLUETOOTH", null),
                node(BLE_ID, "BLE", BLUETOOTH_ID),
                node(PAIRING_ID, "PAIRING", BLE_ID));

        // Moving the root under a grandchild would close the loop.
        assertThatThrownBy(() -> capabilityService.update(BLUETOOTH_ID,
                new UpdateCapabilityRequest(PAIRING_ID, "BLUETOOTH", "Bluetooth", null, 0)))
                .isInstanceOf(ConflictException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_CYCLE_DETECTED);

        verify(capabilityRepository, never()).save(any(CapabilityEntity.class));
    }

    @Test
    @DisplayName("Invalid parent: pointing at a non-existent capability -> CAPABILITY_PARENT_INVALID")
    void invalidParentIsRejected() {
        givenStored(node(BLE_ID, "BLE", BLUETOOTH_ID));
        when(capabilityRepository.existsById(MISSING_ID)).thenReturn(false);

        assertThatThrownBy(() -> capabilityService.update(BLE_ID,
                new UpdateCapabilityRequest(MISSING_ID, "BLE", "BLE", null, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_PARENT_INVALID);
    }

    @Test
    @DisplayName("A legal re-parent (moving a subtree elsewhere) is allowed")
    void legalMoveIsAllowed() {
        // BLUETOOTH is a sibling root; moving BLE under BR/EDR does not close a loop.
        givenStored(
                node(BLUETOOTH_ID, "BLUETOOTH", null),
                node(BLE_ID, "BLE", BLUETOOTH_ID),
                node(BREDR_ID, "BREDR", null));

        CapabilityResponse response = capabilityService.update(BLE_ID,
                new UpdateCapabilityRequest(BREDR_ID, "BLE", "BLE", null, 0));

        assertThat(response.parentId()).isEqualTo(BREDR_ID);
        verify(capabilityRepository).save(any(CapabilityEntity.class));
    }

    // ---------------------------------------------------------------- code uniqueness

    @Test
    @DisplayName("Duplicate code on create -> CAPABILITY_CODE_DUPLICATE")
    void duplicateCodeOnCreateIsRejected() {
        when(capabilityRepository.existsByCodeIgnoreCase("BLE")).thenReturn(true);

        assertThatThrownBy(() -> capabilityService.create(
                new CreateCapabilityRequest(null, "BLE", "BLE", null, null)))
                .isInstanceOf(ConflictException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_CODE_DUPLICATE);

        verify(capabilityRepository, never()).save(any(CapabilityEntity.class));
    }

    @Test
    @DisplayName("Duplicate code on update (case-insensitive, excluding self) -> CAPABILITY_CODE_DUPLICATE")
    void duplicateCodeOnUpdateIsRejected() {
        givenStored(
                node(BLUETOOTH_ID, "BLUETOOTH", null),
                node(BLE_ID, "BLE", BLUETOOTH_ID));
        // Another capability already owns 'ble' in a different case.
        when(capabilityRepository.existsByCodeIgnoreCaseAndIdNot("ble", BLE_ID)).thenReturn(true);

        assertThatThrownBy(() -> capabilityService.update(BLE_ID,
                new UpdateCapabilityRequest(BLUETOOTH_ID, "ble", "BLE", null, 0)))
                .isInstanceOf(ConflictException.class)
                .extracting(thrown -> ((CaseHubException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.CAPABILITY_CODE_DUPLICATE);

        verify(capabilityRepository, never()).save(any(CapabilityEntity.class));
    }

    @Test
    @DisplayName("A capability may keep its own code when other fields change")
    void keepingOwnCodeIsAllowed() {
        givenStored(
                node(BLUETOOTH_ID, "BLUETOOTH", null),
                node(BLE_ID, "BLE", BLUETOOTH_ID));
        when(capabilityRepository.existsByCodeIgnoreCaseAndIdNot("BLE", BLE_ID)).thenReturn(false);

        CapabilityResponse response = capabilityService.update(BLE_ID,
                new UpdateCapabilityRequest(BLUETOOTH_ID, "BLE", "BLE renamed", null, 2));

        assertThat(response.code()).isEqualTo("BLE");
        assertThat(response.name()).isEqualTo("BLE renamed");
    }

    // ---------------------------------------------------------------- tree

    @Test
    @DisplayName("getTree assembles arbitrary depth: Bluetooth -> BLE -> {Pairing, GATT} and BR/EDR")
    void getTreeBuildsNestedStructure() {
        // Mirrors findAllByOrderBySortOrderAscNameAsc(): equal sort_order -> name order.
        when(capabilityRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(
                node(BLUETOOTH_ID, "BLUETOOTH", null),
                node(BLE_ID, "BLE", BLUETOOTH_ID),
                node(BREDR_ID, "BREDR", BLUETOOTH_ID),
                node(GATT_ID, "GATT", BLE_ID),
                node(PAIRING_ID, "PAIRING", BLE_ID)));

        List<CapabilityTreeNode> tree = capabilityService.getTree();

        assertThat(tree).hasSize(1);
        CapabilityTreeNode bluetooth = tree.get(0);
        assertThat(bluetooth.code()).isEqualTo("BLUETOOTH");
        assertThat(bluetooth.parentId()).isNull();
        assertThat(bluetooth.children()).extracting(CapabilityTreeNode::code)
                .containsExactly("BLE", "BREDR");

        CapabilityTreeNode ble = bluetooth.children().get(0);
        assertThat(ble.parentId()).isEqualTo(BLUETOOTH_ID);
        assertThat(ble.children()).extracting(CapabilityTreeNode::code)
                .containsExactly("GATT", "PAIRING");
        // Leaves carry an empty list, never null.
        assertThat(ble.children().get(0).children()).isEmpty();
    }

    @Test
    @DisplayName("getTree returns an empty list when the library is empty")
    void getTreeReturnsEmptyListWhenLibraryIsEmpty() {
        when(capabilityRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of());

        assertThat(capabilityService.getTree()).isEmpty();
    }

    // ---------------------------------------------------------------- helpers

    private static CapabilityEntity node(UUID id, String code, UUID parentId) {
        CapabilityEntity entity = new CapabilityEntity(parentId, code, code, null, 0);
        entity.setId(id);
        return entity;
    }

    /**
     * Registers the nodes as readable through the repository. Stubs are lenient because
     * a single scenario only touches a subset of them.
     */
    private void givenStored(CapabilityEntity... nodes) {
        for (CapabilityEntity node : nodes) {
            lenient().when(capabilityRepository.findById(node.getId())).thenReturn(Optional.of(node));
            lenient().when(capabilityRepository.existsById(node.getId())).thenReturn(true);
        }
    }
}
