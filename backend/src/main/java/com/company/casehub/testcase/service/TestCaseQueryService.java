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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

    public TestCaseQueryService(MasterTestCaseRepository masterRepository) {
        this.masterRepository = masterRepository;
    }

    public PagedResponse<TestCaseSummaryResponse> list(String q, UUID categoryId, List<UUID> tagIds, List<UUID> toolIds,
                                                       List<UUID> standardTaskTypeIds, String status, int page, int size,
                                                       String sort, UserPrincipal principal) {
        if (size < 1 || size > 100) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "Page size must be between 1 and 100.");
        }
        Sort requestedSort = parseSort(sort);
        TestCaseVersionStatus requestedStatus = parseStatus(status);
        List<ListEntry> entries = masterRepository.findAll().stream()
                .map(master -> selectListEntry(master, q, categoryId, tagIds, toolIds, standardTaskTypeIds,
                        requestedStatus, principal))
                .flatMap(Optional::stream)
                .sorted(listComparator(requestedSort))
                .toList();

        int normalizedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(normalizedPage, size);
        long offset = (long) normalizedPage * size;
        int fromIndex = offset >= entries.size() ? entries.size() : (int) offset;
        int toIndex = Math.min(fromIndex + size, entries.size());
        List<TestCaseSummaryResponse> content = entries.subList(fromIndex, toIndex).stream()
                .map(entry -> TestCaseSummaryResponse.from(entry.master(), entry.version()))
                .toList();
        return PagedResponse.from(new PageImpl<>(content, pageable, entries.size()));
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

    private Optional<ListEntry> selectListEntry(MasterTestCaseEntity master, String q, UUID categoryId,
                                                 List<UUID> tagIds, List<UUID> toolIds,
                                                 List<UUID> standardTaskTypeIds, TestCaseVersionStatus status,
                                                 UserPrincipal principal) {
        if (categoryId != null && !Objects.equals(master.getCategory().getId(), categoryId)) {
            return Optional.empty();
        }
        if (tagIds != null && !tagIds.isEmpty() && master.getTags().stream()
                .noneMatch(tag -> tagIds.contains(tag.getTag().getId()))) {
            return Optional.empty();
        }

        List<TestCaseVersionEntity> candidates = visibleVersions(master, principal);
        boolean hasVersionScopedConstraint = status != null || (toolIds != null && !toolIds.isEmpty())
                || (standardTaskTypeIds != null && !standardTaskTypeIds.isEmpty());
        if (StringUtils.hasText(q)) {
            String term = q.trim();
            boolean masterMatches = contains(master.getCaseCode(), term)
                    || master.getTags().stream().anyMatch(tag -> contains(tag.getTag().getName(), term));
            List<TestCaseVersionEntity> matchingVersions = candidates.stream()
                    .filter(version -> versionMatches(version, term)).toList();
            if (!matchingVersions.isEmpty()) {
                candidates = matchingVersions;
                hasVersionScopedConstraint = true;
            } else if (!masterMatches) {
                return Optional.empty();
            }
        }
        if (status != null) {
            candidates = candidates.stream().filter(version -> version.getStatus() == status).toList();
        }
        if (toolIds != null && !toolIds.isEmpty()) {
            candidates = candidates.stream().filter(version -> version.getTools().stream()
                    .anyMatch(tool -> toolIds.contains(tool.getTool().getId()))).toList();
        }
        if (standardTaskTypeIds != null && !standardTaskTypeIds.isEmpty()) {
            candidates = candidates.stream().filter(version -> version.getStandardMappings().stream()
                    .anyMatch(mapping -> standardTaskTypeIds.contains(mapping.getStandardTaskType().getId()))).toList();
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        TestCaseVersionEntity selected = hasVersionScopedConstraint
                ? preferredVersion(candidates)
                : visibleVersion(master, principal);
        return selected == null ? Optional.empty() : Optional.of(new ListEntry(master, selected));
    }

    private Comparator<ListEntry> listComparator(Sort sort) {
        Sort.Order order = sort.iterator().next();
        Comparator<ListEntry> comparator = switch (order.getProperty()) {
            case "caseName" -> Comparator.comparing(entry -> entry.version().getCaseName(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "updatedAt" -> Comparator.comparing(entry -> entry.version().getUpdatedAt(),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "createdAt" -> Comparator.comparing(entry -> entry.version().getCreatedAt(),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "caseCode" -> Comparator.comparing(entry -> entry.master().getCaseCode(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> throw new IllegalStateException("Unsupported parsed sort: " + order.getProperty());
        };
        if (order.isDescending()) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(entry -> entry.master().getCaseCode(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private TestCaseVersionEntity preferredVersion(List<TestCaseVersionEntity> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparing((TestCaseVersionEntity version) ->
                                version.isCurrentVersion() && version.getStatus() == TestCaseVersionStatus.PUBLISHED)
                        .reversed()
                        .thenComparing(Comparator.comparingInt(TestCaseVersionEntity::getVersionMajor).reversed())
                        .thenComparing(Comparator.comparingInt(TestCaseVersionEntity::getVersionMinor).reversed()))
                .findFirst().orElse(null);
    }

    private boolean versionMatches(TestCaseVersionEntity version, String term) {
        return contains(version.getCaseName(), term) || contains(version.getTestPurpose(), term)
                || version.getSteps().stream().anyMatch(step -> contains(step.getTitle(), term)
                || contains(step.getContent(), term))
                || version.getTools().stream().anyMatch(tool -> contains(tool.getTool().getName(), term));
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
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

    private record ListEntry(MasterTestCaseEntity master, TestCaseVersionEntity version) {
    }

    private static ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + id);
    }
}
