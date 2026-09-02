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
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class TestCaseQueryService {

    private static final List<String> SORT_FIELDS = List.of("updatedAt", "createdAt", "caseCode", "caseName");
    private final MasterTestCaseRepository masterRepository;
    private final TestCaseVersionRepository versionRepository;

    public TestCaseQueryService(MasterTestCaseRepository masterRepository, TestCaseVersionRepository versionRepository) {
        this.masterRepository = masterRepository;
        this.versionRepository = versionRepository;
    }

    public PagedResponse<TestCaseSummaryResponse> list(String q, UUID categoryId, List<UUID> tagIds, List<UUID> toolIds,
                                                       List<UUID> standardTaskTypeIds, String status, int page, int size,
                                                       String sort, UserPrincipal principal) {
        if (size < 1 || size > 100) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "Page size must be between 1 and 100.");
        }
        Sort requestedSort = parseSort(sort);
        boolean sortByVersionName = requestedSort.getOrderFor("caseName") != null;
        Pageable pageable = PageRequest.of(Math.max(page, 0), size,
                sortByVersionName ? Sort.unsorted() : requestedSort);
        TestCaseVersionStatus requestedStatus = parseStatus(status);
        Page<MasterTestCaseEntity> masters = masterRepository.findAll(specification(q, categoryId, tagIds, toolIds,
                standardTaskTypeIds, requestedStatus, requestedSort, principal), pageable);
        Page<TestCaseSummaryResponse> response = masters.map(master -> {
            TestCaseVersionEntity visible = visibleVersion(master, principal);
            return TestCaseSummaryResponse.from(master, visible);
        });
        return PagedResponse.from(response);
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
        return TestCaseVersionResponse.from(version);
    }

    public TestCaseDetailResponse toDetail(MasterTestCaseEntity master, TestCaseVersionEntity draft,
                                           TestCaseVersionEntity visible, UserPrincipal principal) {
        TestCaseVersionEntity current = master.getVersions().stream()
                .filter(version -> version.isCurrentVersion() && version.getStatus() == TestCaseVersionStatus.PUBLISHED)
                .findFirst().orElse(null);
        List<VersionSummaryResponse> summaries = visibleVersions(master, principal).stream()
                .map(VersionSummaryResponse::from).toList();
        boolean editDraft = draft != null && draft.getStatus() == TestCaseVersionStatus.DRAFT
                && principal.getPermissions().contains("test_case:draft_edit")
                && (isAdmin(principal) || (draft.getCreatedBy() != null && Objects.equals(draft.getCreatedBy().getId(), principal.getId())));
        return new TestCaseDetailResponse(master.getId(), master.getCaseCode(), master.getCategory().getId(), master.getCategory().getName(),
                master.getCreatedBy().getId(), master.isEnabled(), master.getCreatedAt(), master.getUpdatedAt(),
                master.getTags().stream().map(tag -> new com.company.casehub.testcase.dto.TagRef(tag.getTag().getId(), tag.getTag().getCode(), tag.getTag().getName())).toList(),
                current == null ? null : TestCaseVersionResponse.from(current),
                draft == null ? null : TestCaseVersionResponse.from(draft), TestCaseVersionResponse.from(visible), summaries,
                new AllowedActions(editDraft, principal.getPermissions().contains("test_case:draft_create")));
    }

    private Specification<MasterTestCaseEntity> specification(String q, UUID categoryId, List<UUID> tagIds,
                                                               List<UUID> toolIds, List<UUID> standardIds,
                                                               TestCaseVersionStatus status, Sort sort,
                                                               UserPrincipal principal) {
        return (root, query, cb) -> {
            query.distinct(true);
            var versions = root.join("versions", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                var steps = versions.join("steps", JoinType.LEFT);
                var tags = root.join("tags", JoinType.LEFT).join("tag", JoinType.LEFT);
                var tools = versions.join("tools", JoinType.LEFT).join("tool", JoinType.LEFT);
                predicates.add(cb.or(cb.like(cb.lower(root.get("caseCode")), pattern),
                        cb.like(cb.lower(versions.get("caseName")), pattern), cb.like(cb.lower(versions.get("testPurpose")), pattern),
                        cb.like(cb.lower(steps.get("content")), pattern), cb.like(cb.lower(tags.get("name")), pattern),
                        cb.like(cb.lower(tools.get("name")), pattern)));
            }
            if (status != null) predicates.add(cb.equal(versions.get("status"), status));
            if (!isAdmin(principal)) {
                predicates.add(cb.or(cb.equal(versions.get("status"), TestCaseVersionStatus.PUBLISHED),
                        cb.and(cb.equal(versions.get("status"), TestCaseVersionStatus.DRAFT),
                                cb.equal(versions.get("createdBy").get("id"), principal.getId()))));
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                var tags = root.join("tags", JoinType.LEFT).join("tag", JoinType.LEFT);
                predicates.add(tags.get("id").in(tagIds.stream().distinct().toList()));
            }
            if (toolIds != null && !toolIds.isEmpty()) {
                var tools = versions.join("tools", JoinType.LEFT).join("tool", JoinType.LEFT);
                predicates.add(tools.get("id").in(toolIds.stream().distinct().toList()));
            }
            if (standardIds != null && !standardIds.isEmpty()) {
                var mappings = versions.join("standardMappings", JoinType.LEFT).join("standardTaskType", JoinType.LEFT);
                predicates.add(mappings.get("id").in(standardIds.stream().distinct().toList()));
            }
            Sort.Order caseNameOrder = sort.getOrderFor("caseName");
            if (caseNameOrder != null) {
                query.distinct(false);
                query.groupBy(root);
                var versionName = cb.min(versions.get("caseName"));
                query.orderBy(caseNameOrder.isAscending() ? cb.asc(versionName) : cb.desc(versionName), cb.asc(root.get("caseCode")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
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
        if (isAdmin(principal)) {
            return master.getVersions().stream().filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED)
                    .findFirst().orElseGet(() -> latest(master.getVersions()));
        }
        TestCaseVersionEntity published = master.getVersions().stream()
                .filter(v -> v.isCurrentVersion() && v.getStatus() == TestCaseVersionStatus.PUBLISHED).findFirst().orElse(null);
        if (published != null) return published;
        return latestVisible(master, principal, TestCaseVersionStatus.DRAFT);
    }

    private TestCaseVersionEntity latestVisible(MasterTestCaseEntity master, UserPrincipal principal, TestCaseVersionStatus status) {
        return visibleVersions(master, principal).stream().filter(v -> v.getStatus() == status).findFirst().orElse(null);
    }

    private List<TestCaseVersionEntity> visibleVersions(MasterTestCaseEntity master, UserPrincipal principal) {
        return master.getVersions().stream()
                .filter(v -> isAdmin(principal) || v.getStatus() == TestCaseVersionStatus.PUBLISHED
                        || (v.getStatus() == TestCaseVersionStatus.DRAFT && v.getCreatedBy() != null
                        && Objects.equals(v.getCreatedBy().getId(), principal.getId())))
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

    private static ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + id);
    }
}
