package com.company.casehub.testcase.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.common.exception.ValidationException;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.tag.entity.TagEntity;
import com.company.casehub.tag.repository.TagRepository;
import com.company.casehub.testcase.dto.AllowedActions;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.StandardMappingRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.dto.TestCaseVersionResponse;
import com.company.casehub.testcase.dto.TestStepResponse;
import com.company.casehub.testcase.dto.ToolRef;
import com.company.casehub.testcase.dto.UpdateDraftRequest;
import com.company.casehub.testcase.dto.VersionSummaryResponse;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseStandardMappingEntity;
import com.company.casehub.testcase.entity.TestCaseTagEntity;
import com.company.casehub.testcase.entity.TestCaseToolEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TestStepEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseAttachmentRepository;
import com.company.casehub.testcase.repository.TestCaseStandardMappingRepository;
import com.company.casehub.testcase.repository.TestCaseTagRepository;
import com.company.casehub.testcase.repository.TestCaseToolRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.TestStepRepository;
import com.company.casehub.tool.entity.ToolEntity;
import com.company.casehub.tool.repository.ToolRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TestCaseDraftService {

    private final MasterTestCaseRepository masterRepository;
    private final TestCaseVersionRepository versionRepository;
    private final TestStepRepository stepRepository;
    private final TestCaseTagRepository caseTagRepository;
    private final TestCaseToolRepository caseToolRepository;
    private final TestCaseStandardMappingRepository mappingRepository;
    private final TestCaseAttachmentRepository attachmentRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ToolRepository toolRepository;
    private final StandardTaskTypeRepository standardRepository;
    private final UserRepository userRepository;
    private final TestCaseAccessPolicy accessPolicy;

    public TestCaseDraftService(MasterTestCaseRepository masterRepository, TestCaseVersionRepository versionRepository,
                                TestStepRepository stepRepository, TestCaseTagRepository caseTagRepository,
                                TestCaseToolRepository caseToolRepository, TestCaseStandardMappingRepository mappingRepository,
                                TestCaseAttachmentRepository attachmentRepository, CategoryRepository categoryRepository,
                                TagRepository tagRepository, ToolRepository toolRepository,
                                StandardTaskTypeRepository standardRepository, UserRepository userRepository,
                                TestCaseAccessPolicy accessPolicy) {
        this.masterRepository = masterRepository;
        this.versionRepository = versionRepository;
        this.stepRepository = stepRepository;
        this.caseTagRepository = caseTagRepository;
        this.caseToolRepository = caseToolRepository;
        this.mappingRepository = mappingRepository;
        this.attachmentRepository = attachmentRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.toolRepository = toolRepository;
        this.standardRepository = standardRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public TestCaseDetailResponse createDraft(CreateDraftRequest request, UserPrincipal principal) {
        String caseCode = request.caseCode().trim();
        if (masterRepository.existsByCaseCodeIgnoreCase(caseCode)) {
            throw new ConflictException(ErrorCode.TEST_CASE_CODE_DUPLICATE, "Test case code already exists: " + caseCode);
        }
        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .filter(CategoryEntity::isEnabled)
                .orElseThrow(() -> new ValidationException(ErrorCode.TEST_CASE_CATEGORY_INVALID,
                        "Category does not exist or is disabled: " + request.categoryId()));
        UserEntity user = currentUser(principal);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setCaseCode(caseCode);
        master.setCategory(category);
        master.setCreatedBy(user);
        master.setEnabled(true);

        TestCaseVersionEntity version = newVersion(master, user, request.caseName(), request.testPurpose(), request.preconditions(),
                request.selectionMode(), request.evidenceRequired(), request.evidenceRequirement(), request.remarkRequirement(),
                request.progressiveRole());
        replaceSteps(version, request.steps());
        replaceTags(master, request.tagIds());
        replaceTools(version, request.toolIds());
        replaceMappings(version, request.standardMappings());
        master.getVersions().add(version);
        masterRepository.save(master);
        return detail(master, version, version, principal);
    }

    @Transactional
    public TestCaseDetailResponse updateDraft(UUID masterId, UpdateDraftRequest request, UserPrincipal principal) {
        MasterTestCaseEntity master = masterRepository.findById(masterId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + masterId));
        TestCaseVersionEntity draft = versionRepository
                .findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(masterId, TestCaseVersionStatus.DRAFT)
                .orElseThrow(() -> new ConflictException(ErrorCode.TEST_CASE_DRAFT_REQUIRED, "No Draft exists for: " + masterId));
        // Published Immutable (Service-layer, not just DB CHECK): a closed or non-DRAFT
        // version is rejected here even if a caller somehow targets it via the Draft API.
        if (draft.isRevisionClosed() || draft.getStatus() != TestCaseVersionStatus.DRAFT) {
            throw new ConflictException(ErrorCode.TEST_CASE_VERSION_IMMUTABLE, "Only open Draft versions can be edited.");
        }
        if (!accessPolicy.canEditDraft(draft, principal)) {
            throw new ForbiddenOperationException(ErrorCode.TEST_CASE_DRAFT_EDIT_FORBIDDEN,
                    "Only the Draft owner, a contributor or an administrator may edit this test case.");
        }
        draft.setCaseName(request.caseName().trim());
        draft.setTestPurpose(trimToNull(request.testPurpose()));
        draft.setPreconditions(trimToNull(request.preconditions()));
        draft.setSelectionMode(request.selectionMode());
        draft.setEvidenceRequired(Boolean.TRUE.equals(request.evidenceRequired()));
        draft.setEvidenceRequirement(trimToNull(request.evidenceRequirement()));
        draft.setRemarkRequirement(trimToNull(request.remarkRequirement()));
        draft.setProgressiveRole(request.progressiveRole());
        replaceSteps(draft, request.steps());
        replaceTags(master, request.tagIds());
        replaceTools(draft, request.toolIds());
        replaceMappings(draft, request.standardMappings());
        versionRepository.save(draft);
        return detail(master, draft, draft, principal);
    }

    private TestCaseVersionEntity newVersion(MasterTestCaseEntity master, UserEntity user, String caseName,
                                             String purpose, String preconditions,
                                             com.company.casehub.testcase.entity.SelectionMode selectionMode,
                                             Boolean evidenceRequired, String evidenceRequirement, String remarkRequirement,
                                             com.company.casehub.testcase.entity.ProgressiveRole progressiveRole) {
        TestCaseVersionEntity version = new TestCaseVersionEntity();
        version.setMasterTestCase(master);
        version.setVersionMajor(1);
        version.setVersionMinor(0);
        version.setStatus(TestCaseVersionStatus.DRAFT);
        version.setCurrentVersion(false);
        version.setCaseName(caseName.trim());
        version.setTestPurpose(trimToNull(purpose));
        version.setPreconditions(trimToNull(preconditions));
        version.setSelectionMode(selectionMode);
        version.setEvidenceRequired(Boolean.TRUE.equals(evidenceRequired));
        version.setEvidenceRequirement(trimToNull(evidenceRequirement));
        version.setRemarkRequirement(trimToNull(remarkRequirement));
        version.setProgressiveRole(progressiveRole);
        version.setCreatedBy(user);
        version.setRevisionClosed(false);
        return version;
    }

    private void replaceSteps(TestCaseVersionEntity version, List<StepRequest> requests) {
        if (version.getId() != null) {
            stepRepository.deleteByTestCaseVersionId(version.getId());
            stepRepository.flush();
        }
        version.getSteps().clear();
        int sequence = 1;
        for (StepRequest request : safeList(requests)) {
            if (!StringUtils.hasText(request.content())) {
                throw new ValidationException(ErrorCode.TEST_CASE_STEP_CONTENT_REQUIRED, "Step content is required.");
            }
            TestStepEntity step = new TestStepEntity();
            step.setTestCaseVersion(version);
            step.setSequenceNo(sequence++);
            step.setTitle(trimToNull(request.title()));
            step.setContent(request.content().trim());
            version.getSteps().add(step);
        }
    }

    private void replaceTags(MasterTestCaseEntity master, List<UUID> ids) {
        if (master.getId() != null) {
            caseTagRepository.deleteByMasterTestCaseId(master.getId());
            caseTagRepository.flush();
        }
        master.getTags().clear();
        for (TagEntity tag : findAllRequired(ids,
                values -> tagRepository.findAllById(values).stream().filter(TagEntity::isEnabled).toList(),
                ErrorCode.TEST_CASE_TAG_INVALID, "tag")) {
            TestCaseTagEntity relation = new TestCaseTagEntity();
            relation.setMasterTestCase(master);
            relation.setTag(tag);
            master.getTags().add(relation);
        }
    }

    private void replaceTools(TestCaseVersionEntity version, List<UUID> ids) {
        if (version.getId() != null) {
            caseToolRepository.deleteByTestCaseVersionId(version.getId());
            caseToolRepository.flush();
        }
        version.getTools().clear();
        int sortOrder = 0;
        for (ToolEntity tool : findAllRequired(ids,
                values -> toolRepository.findAllById(values).stream().filter(ToolEntity::isEnabled).toList(),
                ErrorCode.TEST_CASE_TOOL_INVALID, "tool")) {
            TestCaseToolEntity relation = new TestCaseToolEntity();
            relation.setTestCaseVersion(version);
            relation.setTool(tool);
            relation.setSortOrder(sortOrder++);
            version.getTools().add(relation);
        }
    }

    private void replaceMappings(TestCaseVersionEntity version, List<StandardMappingRequest> requests) {
        if (version.getId() != null) {
            mappingRepository.deleteByTestCaseVersionId(version.getId());
            mappingRepository.flush();
        }
        version.getStandardMappings().clear();
        Set<UUID> seen = new LinkedHashSet<>();
        for (StandardMappingRequest request : safeList(requests)) {
            if (!seen.add(request.standardTaskTypeId())) {
                continue;
            }
            StandardTaskTypeEntity standard = standardRepository.findById(request.standardTaskTypeId())
                    .filter(StandardTaskTypeEntity::isEnabled)
                    .orElseThrow(() -> new ValidationException(ErrorCode.TEST_CASE_STANDARD_INVALID,
                            "Standard/Task Type does not exist or is disabled: " + request.standardTaskTypeId()));
            TestCaseStandardMappingEntity relation = new TestCaseStandardMappingEntity();
            relation.setTestCaseVersion(version);
            relation.setStandardTaskType(standard);
            relation.setMappingNote(trimToNull(request.mappingNote()));
            version.getStandardMappings().add(relation);
        }
    }

    private <T> List<T> findAllRequired(List<UUID> ids, Function<Iterable<UUID>, List<T>> finder,
                                         ErrorCode errorCode, String kind) {
        List<UUID> distinctIds = new ArrayList<>(new LinkedHashSet<>(safeList(ids)));
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        List<T> values = finder.apply(distinctIds);
        if (values.size() != distinctIds.size()) {
            throw new ValidationException(errorCode, "One or more " + kind + " references do not exist.");
        }
        Map<UUID, T> byId = values.stream().collect(Collectors.toMap(this::idOf, Function.identity()));
        return distinctIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private UUID idOf(Object entity) {
        if (entity instanceof TagEntity tag) return tag.getId();
        if (entity instanceof ToolEntity tool) return tool.getId();
        throw new IllegalArgumentException("Unsupported reference entity: " + entity.getClass());
    }

    private TestCaseDetailResponse detail(MasterTestCaseEntity master, TestCaseVersionEntity draft,
                                          TestCaseVersionEntity visible, UserPrincipal principal) {
        return new TestCaseDetailResponse(master.getId(), master.getCaseCode(),
                master.getCategory() == null ? null : master.getCategory().getId(),
                master.getCategory() == null ? null : master.getCategory().getName(),
                master.getCreatedBy() == null ? null : master.getCreatedBy().getId(), master.isEnabled(), master.getCreatedAt(),
                master.getUpdatedAt(), master.getTags().stream().map(t -> new com.company.casehub.testcase.dto.TagRef(t.getTag().getId(), t.getTag().getCode(), t.getTag().getName())).toList(),
                null, draft == null ? null : TestCaseVersionResponse.from(draft), visible == null ? null : TestCaseVersionResponse.from(visible),
                master.getVersions().stream().sorted((a, b) -> Integer.compare(b.getVersionMajor() * 10000 + b.getVersionMinor(), a.getVersionMajor() * 10000 + a.getVersionMinor()))
                        .map(VersionSummaryResponse::from).toList(),
                accessPolicy.buildAllowedActions(master, draft, visible, principal));
    }

    private UserEntity currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow(() ->
                new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Current user was not found."));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
