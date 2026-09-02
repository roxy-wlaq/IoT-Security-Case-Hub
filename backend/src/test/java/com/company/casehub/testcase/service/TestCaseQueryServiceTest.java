package com.company.casehub.testcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.user.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TestCaseQueryServiceTest {

    @Mock private MasterTestCaseRepository masterRepository;
    @InjectMocks private TestCaseQueryService service;

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

        TestCaseDetailResponse response = service.detail(master.getId(), principal(userId, "TEST_COORDINATOR"));

        assertThat(response.visibleVersion().caseName()).isEqualTo("Published");
        assertThat(response.currentVersion().caseName()).isEqualTo("Published");
    }

    @Test
    void multiVersionCaseNameSortSemantics() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        MasterTestCaseEntity masterA = master(user, "CASE-A");
        TestCaseVersionEntity oldPublished = version(user, masterA, TestCaseVersionStatus.PUBLISHED, "Alpha");
        oldPublished.setVersionMinor(0);
        TestCaseVersionEntity currentPublished = version(user, masterA, TestCaseVersionStatus.PUBLISHED, "Zulu");
        currentPublished.setVersionMinor(1);
        currentPublished.setCurrentVersion(true);
        masterA.setVersions(List.of(oldPublished, currentPublished));
        MasterTestCaseEntity masterB = master(user, "CASE-B");
        TestCaseVersionEntity beta = version(user, masterB, TestCaseVersionStatus.PUBLISHED, "Beta");
        beta.setCurrentVersion(true);
        masterB.setVersions(List.of(beta));
        stubList(masterA, masterB);

        assertThat(service.list(null, null, null, null, null, null, 0, 20, "caseName,asc", principal(userId, "TESTER"))
                .content()).extracting("caseName").containsExactly("Beta", "Zulu");
        assertThat(service.list(null, null, null, null, null, null, 0, 20, "caseName,desc", principal(userId, "TESTER"))
                .content()).extracting("caseName").containsExactly("Zulu", "Beta");
    }

    @Test
    void statusFilterReturnsMatchingVersion() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        MasterTestCaseEntity master = master(user, "CASE-STATUS");
        TestCaseVersionEntity draft = version(user, master, TestCaseVersionStatus.DRAFT, "Draft version");
        draft.setVersionMinor(1);
        TestCaseVersionEntity published = version(user, master, TestCaseVersionStatus.PUBLISHED, "Published version");
        published.setVersionMinor(0);
        published.setCurrentVersion(true);
        master.setVersions(List.of(draft, published));
        stubList(master);

        var draftResponse = service.list(null, null, null, null, null, "DRAFT", 0, 20, "updatedAt,desc", principal(userId, "TEST_COORDINATOR"));
        var publishedResponse = service.list(null, null, null, null, null, "PUBLISHED", 0, 20, "updatedAt,desc", principal(userId, "TEST_COORDINATOR"));
        assertThat(draftResponse.content()).singleElement().satisfies(summary -> {
            assertThat(summary.status()).isEqualTo("DRAFT");
            assertThat(summary.versionLabel()).isEqualTo("1.1");
            assertThat(summary.caseName()).isEqualTo("Draft version");
        });
        assertThat(publishedResponse.content()).singleElement().satisfies(summary -> {
            assertThat(summary.status()).isEqualTo("PUBLISHED");
            assertThat(summary.versionLabel()).isEqualTo("1.0");
            assertThat(summary.caseName()).isEqualTo("Published version");
        });
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
        master.setVersions(List.of(published));
        stubList(master);

        assertThat(service.list(null, null, null, null, null, null, 0, 20, "updatedAt,desc", principal(userId, "TESTER"))
                .content()).singleElement().extracting("updatedAt")
                .isEqualTo(java.time.Instant.parse("2026-02-01T00:00:00Z"));
    }

    private void stubList(MasterTestCaseEntity... masters) {
        when(masterRepository.findAll()).thenReturn(List.of(masters));
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
