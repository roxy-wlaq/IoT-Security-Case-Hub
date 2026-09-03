package com.company.casehub.testcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.testcase.dto.AllowedActions;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseLibraryQueryRepository;
import com.company.casehub.testcase.repository.TestCaseReviewRecordRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.user.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TestCaseQueryServiceTest {

    @Mock private MasterTestCaseRepository masterRepository;
    @Mock private TestCaseLibraryQueryRepository libraryRepository;
    @Mock private TestCaseVersionRepository versionRepository;
    @Mock private TestCaseAccessPolicy accessPolicy;
    @Mock private TestCaseReviewRecordRepository reviewRecordRepository;
    @InjectMocks private TestCaseQueryService service;

    @Test
    void listDelegatesParsedFiltersAndPaginationToDatabaseQuery() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        List<UUID> tagIds = List.of(UUID.randomUUID());
        List<UUID> toolIds = List.of(UUID.randomUUID());
        List<UUID> standardTaskTypeIds = List.of(UUID.randomUUID());
        when(libraryRepository.search(any())).thenReturn(new TestCaseLibraryQueryRepository.PageResult(List.of(), 37));

        var response = service.list("needle", categoryId, tagIds, toolIds, standardTaskTypeIds, "draft", -2, 20,
                "caseName,asc", principal(userId, "TESTER"));

        ArgumentCaptor<TestCaseLibraryQueryRepository.Query> queryCaptor =
                ArgumentCaptor.forClass(TestCaseLibraryQueryRepository.Query.class);
        verify(libraryRepository).search(queryCaptor.capture());
        TestCaseLibraryQueryRepository.Query query = queryCaptor.getValue();
        assertThat(query.q()).isEqualTo("needle");
        assertThat(query.categoryId()).isEqualTo(categoryId);
        assertThat(query.tagIds()).containsExactlyElementsOf(tagIds);
        assertThat(query.toolIds()).containsExactlyElementsOf(toolIds);
        assertThat(query.standardTaskTypeIds()).containsExactlyElementsOf(standardTaskTypeIds);
        assertThat(query.status()).isEqualTo(TestCaseVersionStatus.DRAFT);
        assertThat(query.principalId()).isEqualTo(userId);
        assertThat(query.admin()).isFalse();
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.order().getProperty()).isEqualTo("caseName");
        assertThat(query.order().isAscending()).isTrue();
        assertThat(response).extracting("content", "page", "size", "totalElements", "totalPages")
                .containsExactly(List.of(), 0, 20, 37L, 2);
        verify(masterRepository, never()).findAll();
        verify(masterRepository, never()).findAllById(any());
        verify(versionRepository, never()).findAllById(any());
    }

    @Test
    void listPreservesDatabaseOrderAfterBatchHydration() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        MasterTestCaseEntity masterA = master(user, "CASE-A");
        MasterTestCaseEntity masterB = master(user, "CASE-B");
        TestCaseVersionEntity versionA = version(user, masterA, TestCaseVersionStatus.PUBLISHED, "Name A");
        TestCaseVersionEntity versionB = version(user, masterB, TestCaseVersionStatus.PUBLISHED, "Name B");
        versionA.setId(UUID.randomUUID());
        versionB.setId(UUID.randomUUID());
        versionA.setCurrentVersion(true);
        versionB.setCurrentVersion(true);
        when(libraryRepository.search(any())).thenReturn(new TestCaseLibraryQueryRepository.PageResult(
                List.of(new TestCaseLibraryQueryRepository.Row(masterB.getId(), versionB.getId()),
                        new TestCaseLibraryQueryRepository.Row(masterA.getId(), versionA.getId())), 47));
        when(masterRepository.findAllById(any())).thenReturn(List.of(masterA, masterB));
        when(versionRepository.findAllById(any())).thenReturn(List.of(versionA, versionB));

        var response = service.list(null, null, null, null, null, null, 0, 20, "updatedAt,desc",
                principal(userId, "TESTER"));

        assertThat(response.content()).extracting("caseCode").containsExactly("CASE-B", "CASE-A");
        assertThat(response.content()).extracting("caseName", "status")
                .containsExactly(tuple("Name B", "PUBLISHED"), tuple("Name A", "PUBLISHED"));
        assertThat(response).extracting("page", "size", "totalElements", "totalPages")
                .containsExactly(0, 20, 47L, 3);
        verify(masterRepository).findAllById(List.of(masterB.getId(), masterA.getId()));
        verify(versionRepository).findAllById(List.of(versionB.getId(), versionA.getId()));
        verify(masterRepository, never()).findAll();
    }

    @Test
    void ownDraftIsVisibleWhenNoPublishedVersionExists() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("owner", "Owner", "hash");
        user.setId(userId);
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID());
        category.setName("Network");
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setId(UUID.randomUUID());
        master.setCaseCode("BLE-001");
        master.setCategory(category);
        master.setCreatedBy(user);
        TestCaseVersionEntity draft = version(user, master, TestCaseVersionStatus.DRAFT, "Draft name");
        master.setVersions(List.of(draft));
        when(masterRepository.findById(master.getId())).thenReturn(Optional.of(master));
        when(reviewRecordRepository.findFirstByTestCaseVersionIdOrderByCreatedAtDescIdDesc(any())).thenReturn(Optional.empty());
        when(accessPolicy.isVersionVisible(any(), any(), any())).thenReturn(true);
        when(accessPolicy.buildAllowedActions(any(), any(), any(), any()))
                .thenReturn(new AllowedActions(true, true, true, false, false, false, false, false, false));

        TestCaseDetailResponse response = service.detail(master.getId(), principal(userId, "TEST_COORDINATOR"));

        assertThat(response.visibleVersion().caseName()).isEqualTo("Draft name");
        assertThat(response.allowedActions().editDraft()).isTrue();
    }

    @Test
    void publishedVersionWinsOverOwnDraft() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("owner", "Owner", "hash");
        user.setId(userId);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setId(UUID.randomUUID());
        master.setCaseCode("BLE-001");
        master.setCreatedBy(user);
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID());
        category.setName("Network");
        master.setCategory(category);
        TestCaseVersionEntity draft = version(user, master, TestCaseVersionStatus.DRAFT, "Draft");
        TestCaseVersionEntity published = version(user, master, TestCaseVersionStatus.PUBLISHED, "Published");
        published.setCurrentVersion(true);
        master.setVersions(List.of(draft, published));
        when(masterRepository.findById(master.getId())).thenReturn(Optional.of(master));
        when(reviewRecordRepository.findFirstByTestCaseVersionIdOrderByCreatedAtDescIdDesc(any())).thenReturn(Optional.empty());
        when(accessPolicy.isVersionVisible(any(), any(), any())).thenReturn(true);

        TestCaseDetailResponse response = service.detail(master.getId(), principal(userId, "TEST_COORDINATOR"));

        assertThat(response.visibleVersion().caseName()).isEqualTo("Published");
        assertThat(response.currentVersion().caseName()).isEqualTo("Published");
    }

    @Test
    void newestVisibleVersionWinsWhenHistoricalPublishedIsNotCurrent() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        MasterTestCaseEntity master = master(user, "BLE-002");
        TestCaseVersionEntity historicalPublished = version(user, master, TestCaseVersionStatus.PUBLISHED, "Historical Published");
        historicalPublished.setCurrentVersion(false);
        historicalPublished.setVersionMinor(0);
        TestCaseVersionEntity deprecated = version(user, master, TestCaseVersionStatus.DEPRECATED, "Newest Deprecated");
        deprecated.setCurrentVersion(false);
        deprecated.setVersionMinor(1);
        master.setVersions(List.of(historicalPublished, deprecated));
        when(masterRepository.findById(master.getId())).thenReturn(Optional.of(master));
        when(reviewRecordRepository.findFirstByTestCaseVersionIdOrderByCreatedAtDescIdDesc(any())).thenReturn(Optional.empty());
        when(accessPolicy.isVersionVisible(any(), any(), any())).thenReturn(true);

        TestCaseDetailResponse response = service.detail(master.getId(), principal(userId, "TESTER"));

        assertThat(response.currentVersion()).isNull();
        assertThat(response.visibleVersion().caseName()).isEqualTo("Newest Deprecated");
        assertThat(response.versions()).extracting("versionLabel", "status")
                .containsExactly(tuple("1.1", "DEPRECATED"), tuple("1.0", "PUBLISHED"));
    }

    @Test
    void listFailsWhenDatabaseRowCannotBeHydrated() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        MasterTestCaseEntity master = master(user, "CASE-MISSING");
        UUID missingVersionId = UUID.randomUUID();
        when(libraryRepository.search(any())).thenReturn(new TestCaseLibraryQueryRepository.PageResult(
                List.of(new TestCaseLibraryQueryRepository.Row(master.getId(), missingVersionId)), 1));
        when(masterRepository.findAllById(any())).thenReturn(List.of(master));
        when(versionRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.list(null, null, null, null, null, null, 0, 20, "updatedAt,desc",
                principal(userId, "TESTER")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missingVersionId.toString());
    }

    @Test
    void summaryUpdatedAtUsesVersion() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        MasterTestCaseEntity master = master(user, "CASE-TIME");
        TestCaseVersionEntity published = version(user, master, TestCaseVersionStatus.PUBLISHED, "Published");
        published.setCurrentVersion(true);
        ReflectionTestUtils.setField(master, "updatedAt", java.time.Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(published, "updatedAt", java.time.Instant.parse("2026-02-01T00:00:00Z"));
        published.setId(UUID.randomUUID());
        when(libraryRepository.search(any())).thenReturn(new TestCaseLibraryQueryRepository.PageResult(
                List.of(new TestCaseLibraryQueryRepository.Row(master.getId(), published.getId())), 1));
        when(masterRepository.findAllById(any())).thenReturn(List.of(master));
        when(versionRepository.findAllById(any())).thenReturn(List.of(published));

        assertThat(service.list(null, null, null, null, null, null, 0, 20, "updatedAt,desc", principal(userId, "TESTER"))
                .content()).singleElement().extracting("updatedAt")
                .isEqualTo(java.time.Instant.parse("2026-02-01T00:00:00Z"));
    }

    private static MasterTestCaseEntity master(UserEntity user, String code) {
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setId(UUID.randomUUID());
        master.setCaseCode(code);
        master.setCreatedBy(user);
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID());
        category.setName("Network");
        master.setCategory(category);
        return master;
    }

    private static UserEntity user(UUID id) {
        UserEntity user = new UserEntity("owner", "Owner", "hash");
        user.setId(id);
        return user;
    }

    private static TestCaseVersionEntity version(UserEntity user, MasterTestCaseEntity master,
                                                  TestCaseVersionStatus status, String name) {
        TestCaseVersionEntity version = new TestCaseVersionEntity();
        version.setMasterTestCase(master);
        version.setCreatedBy(user);
        version.setStatus(status);
        version.setVersionMajor(1);
        version.setVersionMinor(status == TestCaseVersionStatus.PUBLISHED ? 1 : 0);
        version.setCaseName(name);
        version.setSelectionMode(SelectionMode.SINGLE);
        return version;
    }

    private static UserPrincipal principal(UUID id, String role) {
        return new UserPrincipal(id, "user", "hash", "User", true, false, Set.of(role), Set.of("test_case:read", "test_case:draft_edit"));
    }
}
