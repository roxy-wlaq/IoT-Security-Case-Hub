package com.company.casehub.customcase.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.customcase.dto.CustomDecisionPointRequest;
import com.company.casehub.customcase.dto.CustomTestCaseRequest;
import com.company.casehub.customcase.dto.CustomTestCaseResponse;
import com.company.casehub.customcase.dto.CustomTestCaseResponse.DecisionPointResponse;
import com.company.casehub.customcase.dto.CustomTestCaseResponse.TargetResponse;
import com.company.casehub.customcase.dto.CustomTestCaseResponse.StepResponse;
import com.company.casehub.customcase.dto.CustomStepRequest;
import com.company.casehub.customcase.dto.LibrarySubmissionResponse;
import com.company.casehub.customcase.entity.CustomTestStepEntity;
import com.company.casehub.customcase.entity.ProjectCustomTestCaseEntity;
import com.company.casehub.customcase.repository.ProjectCustomTestCaseRepository;
import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import com.company.casehub.execution.entity.ProjectTestCaseEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.repository.ProjectTestCaseAssigneeRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.repository.ProjectTestCaseSourceRepository;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectCoordinatorRepository;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.RevisionContributorEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.entity.TransitionEntity;
import com.company.casehub.testcase.entity.TransitionTargetEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.RevisionContributorRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomTestCaseService {
    private final ProjectCustomTestCaseRepository customRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCoordinatorRepository coordinatorRepository;
    private final ProjectAccessPolicy accessPolicy;
    private final UserRepository userRepository;
    private final ProjectTestCaseRepository projectTestCaseRepository;
    private final ProjectTestCaseSourceRepository sourceRepository;
    private final ProjectTestCaseAssigneeRepository assigneeRepository;
    private final MasterTestCaseRepository masterRepository;
    private final CategoryRepository categoryRepository;
    private final RevisionContributorRepository contributorRepository;

    public CustomTestCaseService(ProjectCustomTestCaseRepository customRepository, ProjectRepository projectRepository,
                                 ProjectCoordinatorRepository coordinatorRepository, ProjectAccessPolicy accessPolicy,
                                 UserRepository userRepository, ProjectTestCaseRepository projectTestCaseRepository,
                                 ProjectTestCaseSourceRepository sourceRepository,
                                 ProjectTestCaseAssigneeRepository assigneeRepository,
                                 MasterTestCaseRepository masterRepository, CategoryRepository categoryRepository,
                                 RevisionContributorRepository contributorRepository) {
        this.customRepository = customRepository;
        this.projectRepository = projectRepository;
        this.coordinatorRepository = coordinatorRepository;
        this.accessPolicy = accessPolicy;
        this.userRepository = userRepository;
        this.projectTestCaseRepository = projectTestCaseRepository;
        this.sourceRepository = sourceRepository;
        this.assigneeRepository = assigneeRepository;
        this.masterRepository = masterRepository;
        this.categoryRepository = categoryRepository;
        this.contributorRepository = contributorRepository;
    }

    @Transactional
    public CustomTestCaseResponse create(UUID projectId, CustomTestCaseRequest request, UserPrincipal principal) {
        requireActor(projectId, principal);
        ProjectEntity project = requireProject(projectId);
        if (customRepository.existsByProjectIdAndCaseCodeIgnoreCase(projectId, request.caseCode().trim())) {
            throw new ConflictException(ErrorCode.CUSTOM_CASE_DUPLICATE, "Custom case code already exists in this Project");
        }
        UserEntity actor = currentUser(principal);
        ProjectCustomTestCaseEntity custom = new ProjectCustomTestCaseEntity();
        custom.setProject(project);
        custom.setCaseCode(request.caseCode().trim());
        apply(custom, request, actor);
        custom.setCreatedBy(actor);
        custom.setUpdatedBy(actor);
        replaceDefinition(custom, request);
        custom = customRepository.saveAndFlush(custom);
        ProjectTestCaseEntity ptc = new ProjectTestCaseEntity();
        ptc.setProject(project);
        ptc.setCustomTestCase(custom);
        ptc.setCreatedBy(actor);
        ptc.setLastModifiedBy(actor);
        ptc.setLastModifiedAt(Instant.now());
        ptc.setRoot(true);
        ptc = projectTestCaseRepository.saveAndFlush(ptc);
        addSource(ptc, ProjectTestCaseSourceType.CUSTOM);
        if (principal.getRoles().contains("TESTER")) {
            addAssignee(ptc, actor);
        }
        return response(custom, ptc.getId());
    }

    @Transactional(readOnly = true)
    public List<CustomTestCaseResponse> list(UUID projectId, UserPrincipal principal) {
        accessPolicy.requireView(projectId, principal);
        return customRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream().map(custom -> response(
                custom, projectTestCaseRepository.findByProjectIdAndCustomTestCaseId(projectId, custom.getId()).map(ProjectTestCaseEntity::getId).orElse(null))).toList();
    }

    @Transactional
    public CustomTestCaseResponse update(UUID projectId, UUID customId, CustomTestCaseRequest request, UserPrincipal principal) {
        ProjectCustomTestCaseEntity custom = requireCustom(projectId, customId);
        requireEditable(custom, principal);
        if (!custom.getCaseCode().equalsIgnoreCase(request.caseCode().trim())
                && customRepository.existsByProjectIdAndCaseCodeIgnoreCase(projectId, request.caseCode().trim())) {
            throw new ConflictException(ErrorCode.CUSTOM_CASE_DUPLICATE, "Custom case code already exists in this Project");
        }
        UserEntity actor = currentUser(principal);
        custom.setCaseCode(request.caseCode().trim());
        apply(custom, request, actor);
        replaceDefinition(custom, request);
        return response(customRepository.save(custom), projectTestCaseRepository.findByProjectIdAndCustomTestCaseId(projectId, customId).map(ProjectTestCaseEntity::getId).orElse(null));
    }

    @Transactional
    public void assign(UUID projectId, UUID customId, UUID userId, UserPrincipal principal) {
        ProjectCustomTestCaseEntity custom = requireCustom(projectId, customId);
        accessPolicy.requireManage(projectId, principal);
        UserEntity target = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        ProjectTestCaseEntity ptc = projectTestCaseRepository.findByProjectIdAndCustomTestCaseId(projectId, customId).orElseThrow();
        if (!assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), target.getId())) addAssignee(ptc, target);
    }

    @Transactional
    public LibrarySubmissionResponse submitToLibrary(UUID projectId, UUID customId, UserPrincipal principal) {
        ProjectCustomTestCaseEntity custom = requireCustom(projectId, customId);
        requireEditable(custom, principal);
        if (custom.getDecisionPoints().stream().flatMap(p -> p.getTransition() == null ? java.util.stream.Stream.empty() : p.getTransition().getTargets().stream())
                .anyMatch(target -> target.getTargetCustomTestCase() != null)) {
            throw new ConflictException(ErrorCode.CUSTOM_CASE_LIBRARY_TARGET_INVALID, "Custom targets must be converted to library cases before submission");
        }
        UserEntity submitter = currentUser(principal);
        UserEntity owner = coordinatorRepository.findByProjectIdAndPrimaryTrue(projectId).map(link -> link.getUser()).orElse(submitter);
        CategoryEntity category = categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream().filter(CategoryEntity::isEnabled).findFirst()
                .orElseThrow(() -> new ConflictException(ErrorCode.TEST_CASE_CATEGORY_INVALID, "No enabled Category exists"));
        String code = custom.getCaseCode();
        if (masterRepository.existsByCaseCodeIgnoreCase(code)) code = code + "-LIB-" + custom.getId().toString().substring(0, 8);
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setCaseCode(code);
        master.setCategory(category);
        master.setCreatedBy(owner);
        TestCaseVersionEntity version = new TestCaseVersionEntity();
        version.setMasterTestCase(master);
        version.setVersionMajor(1);
        version.setVersionMinor(0);
        version.setStatus(TestCaseVersionStatus.DRAFT);
        version.setCaseName(custom.getCaseName());
        version.setTestPurpose(custom.getTestPurpose());
        version.setPreconditions(custom.getPreconditions());
        version.setSelectionMode(custom.getSelectionMode());
        version.setEvidenceRequired(custom.isEvidenceRequired());
        version.setEvidenceRequirement(custom.getEvidenceRequirement());
        version.setRemarkRequirement(custom.getRemarkRequirement());
        version.setCreatedBy(owner);
        version.setRevisionClosed(false);
        copyDefinition(custom, version);
        master.getVersions().add(version);
        masterRepository.saveAndFlush(master);
        if (!owner.getId().equals(submitter.getId())) {
            RevisionContributorEntity contributor = new RevisionContributorEntity();
            contributor.setTestCaseVersion(version);
            contributor.setUser(submitter);
            contributor.setAddedBy(owner);
            contributorRepository.save(contributor);
        }
        return new LibrarySubmissionResponse(customId, master.getId(), version.getId(),
                owner.getId().equals(submitter.getId()) ? null : submitter.getId());
    }

    private void copyDefinition(ProjectCustomTestCaseEntity custom, TestCaseVersionEntity version) {
        int seq = 1;
        for (CustomTestStepEntity source : custom.getSteps().stream().sorted(Comparator.comparingInt(CustomTestStepEntity::getSequenceNo)).toList()) {
            com.company.casehub.testcase.entity.TestStepEntity step = new com.company.casehub.testcase.entity.TestStepEntity();
            step.setTestCaseVersion(version); step.setSequenceNo(seq++); step.setTitle(source.getTitle()); step.setContent(source.getContent()); version.getSteps().add(step);
        }
        for (DecisionPointEntity source : custom.getDecisionPoints().stream().sorted(Comparator.comparingInt(DecisionPointEntity::getDisplayOrder)).toList()) {
            DecisionPointEntity point = new DecisionPointEntity(); point.setTestCaseVersion(version); point.setDisplayOrder(source.getDisplayOrder()); point.setName(source.getName()); point.setDescription(source.getDescription());
            if (source.getTransition() != null) {
                TransitionEntity transition = new TransitionEntity(); transition.setDecisionPoint(point); transition.setType(source.getTransition().getType()); int order = 1;
                for (TransitionTargetEntity target : source.getTransition().getTargets().stream().sorted(Comparator.comparingInt(TransitionTargetEntity::getTargetOrder)).toList()) {
                    TransitionTargetEntity link = new TransitionTargetEntity(); link.setTransition(transition); link.setTargetOrder(order++); link.setTargetMasterTestCase(target.getTargetMasterTestCase()); transition.getTargets().add(link);
                }
                point.setTransition(transition);
            }
            version.getDecisionPoints().add(point);
        }
    }

    private void replaceDefinition(ProjectCustomTestCaseEntity custom, CustomTestCaseRequest request) {
        custom.getSteps().clear();
        int seq = 1;
        for (CustomStepRequest item : request.steps() == null ? List.<CustomStepRequest>of() : request.steps()) {
            CustomTestStepEntity step = new CustomTestStepEntity(); step.setCustomTestCase(custom); step.setSequenceNo(seq++); step.setTitle(trimToNull(item.title())); step.setContent(item.content().trim()); custom.getSteps().add(step);
        }
        custom.getDecisionPoints().clear();
        for (CustomDecisionPointRequest item : request.decisionPoints() == null ? List.<CustomDecisionPointRequest>of() : request.decisionPoints()) {
            DecisionPointEntity point = new DecisionPointEntity(); point.setCustomTestCase(custom); point.setDisplayOrder(item.displayOrder()); point.setName(item.name().trim()); point.setDescription(trimToNull(item.description()));
            TransitionEntity transition = new TransitionEntity(); transition.setDecisionPoint(point); transition.setType(item.transitionType());
            List<UUID> masterIds = item.targetMasterTestCaseIds() == null ? List.of() : item.targetMasterTestCaseIds();
            List<UUID> customIds = item.targetCustomTestCaseIds() == null ? List.of() : item.targetCustomTestCaseIds();
            if (!masterIds.isEmpty() && !customIds.isEmpty()) throw new ConflictException(ErrorCode.CUSTOM_CASE_TARGET_INVALID, "A target must be Master-based or Custom-based");
            int order = 1;
            for (UUID id : masterIds) { TransitionTargetEntity target = new TransitionTargetEntity(); target.setTransition(transition); target.setTargetOrder(order++); target.setTargetMasterTestCase(masterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Master target not found"))); transition.getTargets().add(target); }
            for (UUID id : customIds) { TransitionTargetEntity target = new TransitionTargetEntity(); target.setTransition(transition); target.setTargetOrder(order++); target.setTargetCustomTestCase(requireCustom(custom.getProject().getId(), id)); transition.getTargets().add(target); }
            point.setTransition(transition); custom.getDecisionPoints().add(point);
        }
    }

    private void addSource(ProjectTestCaseEntity ptc, ProjectTestCaseSourceType type) { ProjectTestCaseSourceEntity source = new ProjectTestCaseSourceEntity(); source.setProjectTestCase(ptc); source.setSourceType(type); sourceRepository.save(source); }
    private void addAssignee(ProjectTestCaseEntity ptc, UserEntity user) { ProjectTestCaseAssigneeEntity assignment = new ProjectTestCaseAssigneeEntity(); assignment.setProjectTestCase(ptc); assignment.setUser(user); assignment.setAssignedAt(Instant.now()); assigneeRepository.save(assignment); }
    private void apply(ProjectCustomTestCaseEntity custom, CustomTestCaseRequest request, UserEntity actor) { custom.setCaseName(request.caseName().trim()); custom.setTestPurpose(trimToNull(request.testPurpose())); custom.setPreconditions(trimToNull(request.preconditions())); custom.setSelectionMode(request.selectionMode()); custom.setEvidenceRequired(Boolean.TRUE.equals(request.evidenceRequired())); custom.setEvidenceRequirement(trimToNull(request.evidenceRequirement())); custom.setRemarkRequirement(trimToNull(request.remarkRequirement())); custom.setUpdatedBy(actor); }
    private void requireActor(UUID projectId, UserPrincipal principal) { if (!(principal.getRoles().contains("TESTER") || principal.getRoles().contains("TEST_COORDINATOR") || principal.getRoles().contains("ADMIN")) || !accessPolicy.canView(projectId, principal)) throw new ForbiddenOperationException(ErrorCode.CUSTOM_CASE_ACCESS_FORBIDDEN, "You are not an authorised member of this Project"); }
    private void requireEditable(ProjectCustomTestCaseEntity custom, UserPrincipal principal) {
        boolean assigned = projectTestCaseRepository.findByProjectIdAndCustomTestCaseId(custom.getProject().getId(), custom.getId())
                .map(ptc -> assigneeRepository.existsByProjectTestCaseIdAndUserId(ptc.getId(), principal.getId())).orElse(false);
        if (!(accessPolicy.canManage(custom.getProject().getId(), principal) || custom.getCreatedBy().getId().equals(principal.getId()) || assigned)) {
            throw new ForbiddenOperationException(ErrorCode.CUSTOM_CASE_EDIT_FORBIDDEN, "You cannot edit this Custom Test Case");
        }
    }
    private ProjectCustomTestCaseEntity requireCustom(UUID projectId, UUID id) { return customRepository.findByIdAndProjectId(id, projectId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOM_CASE_NOT_FOUND, "Custom Test Case not found")); }
    private ProjectEntity requireProject(UUID id) { return projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found")); }
    private UserEntity currentUser(UserPrincipal principal) { return userRepository.findById(principal.getId()).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found")); }
    private CustomTestCaseResponse response(ProjectCustomTestCaseEntity custom, UUID ptcId) { return new CustomTestCaseResponse(custom.getId(), custom.getProject().getId(), custom.getCaseCode(), custom.getCaseName(), custom.getTestPurpose(), custom.getPreconditions(), custom.getSelectionMode(), custom.isEvidenceRequired(), custom.getEvidenceRequirement(), custom.getRemarkRequirement(), ptcId, custom.getCreatedBy().getId(), custom.getSteps().stream().sorted(Comparator.comparingInt(CustomTestStepEntity::getSequenceNo)).map(s -> new StepResponse(s.getId(), s.getSequenceNo(), s.getTitle(), s.getContent())).toList(), custom.getDecisionPoints().stream().sorted(Comparator.comparingInt(DecisionPointEntity::getDisplayOrder)).map(p -> new DecisionPointResponse(p.getId(), p.getDisplayOrder(), p.getName(), p.getDescription(), p.getTransition() == null ? null : p.getTransition().getType(), p.getTransition() == null ? List.of() : p.getTransition().getTargets().stream().sorted(Comparator.comparingInt(TransitionTargetEntity::getTargetOrder)).map(t -> new TargetResponse(t.getTargetMasterTestCase() == null ? null : t.getTargetMasterTestCase().getId(), t.getTargetCustomTestCase() == null ? null : t.getTargetCustomTestCase().getId(), t.getTargetMasterTestCase() == null ? t.getTargetCustomTestCase().getCaseCode() : t.getTargetMasterTestCase().getCaseCode())).toList())).toList(), custom.getCreatedAt(), custom.getUpdatedAt()); }
    private static String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
