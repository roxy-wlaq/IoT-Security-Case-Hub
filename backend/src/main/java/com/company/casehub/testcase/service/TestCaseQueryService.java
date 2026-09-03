package com.company.casehub.testcase.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.common.exception.ValidationException;
import com.company.casehub.testcase.dto.AllowedActions;
import com.company.casehub.testcase.dto.PagedResponse;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.dto.TestCaseSummaryResponse;
import com.company.casehub.testcase.dto.TestCaseVersionResponse;
import com.company.casehub.testcase.dto.VersionSummaryResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.ReviewRecordAction;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseLibraryQueryRepository;
import com.company.casehub.testcase.repository.TestCaseReviewRecordRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class TestCaseQueryService {

    private static final List<String> SORT_FIELDS = List.of("updatedAt", "createdAt", "caseCode", "caseName");
    private final MasterTestCaseRepository masterRepository;
    private final TestCaseVersionRepository versionRepository;
    private final TestCaseLibraryQueryRepository libraryRepository;
    private final TestCaseAccessPolicy accessPolicy;
    private final TestCaseReviewRecordRepository reviewRecordRepository;

    public TestCaseQueryService(MasterTestCaseRepository masterRepository, TestCaseVersionRepository versionRepository,
                                TestCaseLibraryQueryRepository libraryRepository, TestCaseAccessPolicy accessPolicy,
                                TestCaseReviewRecordRepository reviewRecordRepository) {
        this.masterRepository = masterRepository;
        this.versionRepository = versionRepository;
        this.libraryRepository = libraryRepository;
        this.accessPolicy = accessPolicy;
        this.reviewRecordRepository = reviewRecordRepository;
    }

    public PagedResponse<TestCaseSummaryResponse> list(String q, UUID categoryId, List<UUID> tagIds, List<UUID> toolIds,
                                                       List<UUID> standardTaskTypeIds, String status, int page, int size,
                                                       String sort, UserPrincipal principal) {
        if (size < 1 || size > 100) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "Page size must be between 1 and 100.");
        }
        Sort requestedSort = parseSort(sort);
        TestCaseVersionStatus requestedStatus = parseStatus(status);
        int normalizedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(normalizedPage, size);
        TestCaseLibraryQueryRepository.PageResult result = libraryRepository.search(
                new TestCaseLibraryQueryRepository.Query(q, categoryId, tagIds, toolIds, standardTaskTypeIds,
                        requestedStatus, principal.getId(), isAdmin(principal), normalizedPage, size,
                        requestedSort.iterator().next()));
        List<UUID> masterIds = result.rows().stream().map(TestCaseLibraryQueryRepository.Row::masterId).distinct().toList();
        List<UUID> versionIds = result.rows().stream().map(TestCaseLibraryQueryRepository.Row::versionId).distinct().toList();
        Map<UUID, MasterTestCaseEntity> masters = masterIds.isEmpty() ? Map.of() : masterRepository.findAllById(masterIds).stream()
                .collect(Collectors.toMap(MasterTestCaseEntity::getId, Function.identity()));
        Map<UUID, TestCaseVersionEntity> versions = versionIds.isEmpty() ? Map.of() : versionRepository.findAllById(versionIds).stream()
                .collect(Collectors.toMap(TestCaseVersionEntity::getId, Function.identity()));
        List<TestCaseSummaryResponse> content = result.rows().stream()
                .map(row -> TestCaseSummaryResponse.from(requiredMaster(masters, row.masterId()),
                        requiredVersion(versions, row.versionId())))
                .toList();
        return PagedResponse.from(new PageImpl<>(content, pageable, result.totalElements()));
    }

    public TestCaseDetailResponse detail(UUID masterId, UserPrincipal principal) {
        MasterTestCaseEntity master = masterRepository.findById(masterId)
                .orElseThrow(() -> notFound(masterId));
        TestCaseVersionEntity visible = visibleVersion(master, principal);
        if (visible == null) {
            throw notFound(masterId);
        }
        TestCaseVersionEntity draft = latestVisible(master, principal, TestCaseVersionStatus.DRAFT);
        return toDetail(master, draft, visible, principal);
    }

    public List<VersionSummaryResponse> versions(UUID masterId, UserPrincipal principal) {
        MasterTestCaseEntity master = masterRepository.findById(masterId)
                .orElseThrow(() -> notFound(masterId));
        if (visibleVersion(master, principal) == null) {
            throw notFound(masterId);
        }
        return visibleVersions(master, principal).stream().map(VersionSummaryResponse::from).toList();
    }

    public TestCaseVersionResponse version(UUID masterId, UUID versionId, UserPrincipal principal) {
        MasterTestCaseEntity master = masterRepository.findById(masterId)
                .orElseThrow(() -> notFound(masterId));
        TestCaseVersionEntity version = master.getVersions().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), versionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_VERSION_NOT_FOUND,
                        "Test case version not found: " + versionId));
        if (!visibleVersions(master, principal).contains(version)) {
            throw new ResourceNotFoundException(ErrorCode.TEST_CASE_VERSION_NOT_FOUND,
                    "Test case version not found: " + versionId);
        }
        return TestCaseVersionResponse.from(version, latestAction(version));
    }

    public TestCaseDetailResponse toDetail(MasterTestCaseEntity master, TestCaseVersionEntity draft,
                                           TestCaseVersionEntity visible, UserPrincipal principal) {
        TestCaseVersionEntity current = master.getVersions().stream()
                .filter(version -> version.isCurrentVersion() && version.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .findFirst().orElse(null);
        List<VersionSummaryResponse> summaries = visibleVersions(master, principal).stream()
                .map(VersionSummaryResponse::from).toList();
        AllowedActions actions = accessPolicy.buildAllowedActions(master, draft, visible, principal);
        return new TestCaseDetailResponse(master.getId(), master.getCaseCode(), master.getCategory().getId(), master.getCategory().getName(),
                master.getCreatedBy().getId(), master.isEnabled(), master.getCreatedAt(), master.getUpdatedAt(),
                master.getTags().stream().map(tag -> new com.company.casehub.testcase.dto.TagRef(tag.getTag().getId(), tag.getTag().getCode(), tag.getTag().getName())).toList(),
                current == null ? null : TestCaseVersionResponse.from(current, latestAction(current)),
                draft == null ? null : TestCaseVersionResponse.from(draft, latestAction(draft)),
                visible == null ? null : TestCaseVersionResponse.from(visible, latestAction(visible)), summaries, actions);
    }

    private ReviewRecordAction latestAction(TestCaseVersionEntity version) {
        return reviewRecordRepository.findFirstByTestCaseVersionIdOrderByCreatedAtDescIdDesc(version.getId())
                .map(TestCaseReviewRecordEntity -> TestCaseReviewRecordEntity.getAction()).orElse(null);
    }

    private Sort parseSort(String sort) {
        String value = StringUtils.hasText(sort) ? sort.trim() : "updatedAt,desc";
        String[] parts = value.split(",", 2);
        if (!SORT_FIELDS.contains(parts[0]) || (parts.length == 2 && !List.of("asc", "desc").contains(parts[1].toLowerCase()))) {
            throw new ValidationException(ErrorCode.TEST_CASE_SORT_FIELD_INVALID, "Unsupported test case sort: " + value);
        }
        Sort.Direction direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }

    private TestCaseVersionStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return TestCaseVersionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "Unsupported test case status: " + status);
        }
    }

    private TestCaseVersionEntity visibleVersion(MasterTestCaseEntity master, UserPrincipal principal) {
        List<TestCaseVersionEntity> visible = visibleVersions(master, principal);
        if (visible.isEmpty()) return null;
        // The current PUBLISHED version is the primary version. Once it is
        // deprecated, no historical PUBLISHED version may reclaim that role;
        // select the newest visible version instead. This is the same ordering
        // used by the database-backed library query.
        return visible.stream().filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .findFirst().orElseGet(() -> latest(visible));
    }

    private TestCaseVersionEntity latestVisible(MasterTestCaseEntity master, UserPrincipal principal, TestCaseVersionStatus status) {
        return visibleVersions(master, principal).stream().filter(v -> v.getStatus() == status).findFirst().orElse(null);
    }

    private List<TestCaseVersionEntity> visibleVersions(MasterTestCaseEntity master, UserPrincipal principal) {
        return master.getVersions().stream()
                .filter(v -> accessPolicy.isVersionVisible(master, v, principal))
                .sorted(Comparator.comparingInt(TestCaseVersionEntity::getVersionMajor).reversed()
                        .thenComparing(Comparator.comparingInt(TestCaseVersionEntity::getVersionMinor).reversed()))
                .toList();
    }

    private static TestCaseVersionEntity latest(List<TestCaseVersionEntity> versions) {
        return versions.stream().max(Comparator.comparingInt(TestCaseVersionEntity::getVersionMajor)
                .thenComparingInt(TestCaseVersionEntity::getVersionMinor)).orElse(null);
    }

    private static boolean isAdmin(UserPrincipal principal) {
        return principal.getRoles().contains("ADMIN");
    }

    private static MasterTestCaseEntity requiredMaster(Map<UUID, MasterTestCaseEntity> masters, UUID masterId) {
        MasterTestCaseEntity master = masters.get(masterId);
        if (master == null) {
            throw new IllegalStateException("Test case query result master could not be hydrated: " + masterId);
        }
        return master;
    }

    private static TestCaseVersionEntity requiredVersion(Map<UUID, TestCaseVersionEntity> versions, UUID versionId) {
        TestCaseVersionEntity version = versions.get(versionId);
        if (version == null) {
            throw new IllegalStateException("Test case query result version could not be hydrated: " + versionId);
        }
        return version;
    }

    private static ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + id);
    }
}
