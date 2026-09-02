package com.company.casehub.testcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
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
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class TestCaseQueryServiceTest {

    @Mock private MasterTestCaseRepository masterRepository;
    @Mock private TestCaseVersionRepository versionRepository;
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
