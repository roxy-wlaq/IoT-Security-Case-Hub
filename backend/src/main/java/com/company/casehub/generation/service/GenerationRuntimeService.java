package com.company.casehub.generation.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ForbiddenOperationException;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.execution.entity.ProjectTestCasePreferenceEntity;
import com.company.casehub.execution.entity.ProjectTestCaseSourceType;
import com.company.casehub.execution.repository.ProjectTestCasePreferenceRepository;
import com.company.casehub.execution.repository.ProjectTestCaseRepository;
import com.company.casehub.execution.service.ProjectTestPlanService;
import com.company.casehub.generation.dto.GenerationRecommendationResponse;
import com.company.casehub.generation.dto.GenerationRunRequest;
import com.company.casehub.generation.dto.GenerationRunResponse;
import com.company.casehub.generation.entity.GenerationRecommendationEntity;
import com.company.casehub.generation.entity.GenerationRecommendationRuleEntity;
import com.company.casehub.generation.entity.GenerationRunEntity;
import com.company.casehub.generation.entity.GenerationTriggerType;
import com.company.casehub.generation.entity.GenerationRunMode;
import com.company.casehub.generation.entity.RecommendationStatus;
import com.company.casehub.generation.model.GenerationResult;
import com.company.casehub.generation.repository.GenerationRecommendationRepository;
import com.company.casehub.generation.repository.GenerationRecommendationRuleRepository;
import com.company.casehub.generation.repository.GenerationRunRepository;
import com.company.casehub.project.entity.GenerationMode;
import com.company.casehub.project.entity.ProjectEntity;
import com.company.casehub.project.repository.ProjectRepository;
import com.company.casehub.project.repository.ProjectStandardRepository;
import com.company.casehub.project.service.CapabilityEngine;
import com.company.casehub.project.service.ProjectAccessPolicy;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationRuntimeService {

    private final ProjectRepository projectRepository;
    private final ProjectStandardRepository standardRepository;
    private final GenerationRunRepository runRepository;
    private final GenerationRecommendationRepository recommendationRepository;
    private final GenerationRecommendationRuleRepository recommendationRuleRepository;
    private final ProjectTestCasePreferenceRepository preferenceRepository;
    private final ProjectTestCaseRepository projectTestCaseRepository;
    private final UserRepository userRepository;
    private final GenerationEngine engine;
    private final CapabilityEngine capabilityEngine;
    private final ProjectAccessPolicy accessPolicy;
    private final ProjectTestPlanService testPlanService;

    public GenerationRuntimeService(ProjectRepository projectRepository, ProjectStandardRepository standardRepository,
                                    GenerationRunRepository runRepository,
                                    GenerationRecommendationRepository recommendationRepository,
                                    GenerationRecommendationRuleRepository recommendationRuleRepository,
                                    ProjectTestCasePreferenceRepository preferenceRepository,
                                    ProjectTestCaseRepository projectTestCaseRepository, UserRepository userRepository,
                                    GenerationEngine engine, CapabilityEngine capabilityEngine,
                                    ProjectAccessPolicy accessPolicy, ProjectTestPlanService testPlanService) {
        this.projectRepository = projectRepository;
        this.standardRepository = standardRepository;
        this.runRepository = runRepository;
        this.recommendationRepository = recommendationRepository;
        this.recommendationRuleRepository = recommendationRuleRepository;
        this.preferenceRepository = preferenceRepository;
        this.projectTestCaseRepository = projectTestCaseRepository;
        this.userRepository = userRepository;
        this.engine = engine;
        this.capabilityEngine = capabilityEngine;
        this.accessPolicy = accessPolicy;
        this.testPlanService = testPlanService;
    }

    @Transactional
    public GenerationRunResponse run(UUID projectId, GenerationRunRequest request, UserPrincipal principal) {
        accessPolicy.requireManage(projectId, principal);
        ProjectEntity project = requireProject(projectId);
        GenerationRunMode mode = request == null || request.mode() == null
                ? (project.getGenerationMode() == GenerationMode.PROGRESSIVE
                ? GenerationRunMode.PROGRESSIVE_INITIAL : GenerationRunMode.FULL) : request.mode();
        GenerationTriggerType trigger = request == null || request.triggerType() == null
                ? GenerationTriggerType.MANUAL_REGENERATE : request.triggerType();
        GenerationRunEntity run = new GenerationRunEntity();
        run.setProject(project);
        run.setMode(mode);
        run.setTriggerType(trigger);
        run.setExecutedBy(requireUser(principal.getId()));
        run.setExecutedAt(Instant.now());
        run = runRepository.saveAndFlush(run);
        GenerationResult result = engine.evaluate(project, mode, standardRepository.findByProjectId(projectId), capabilityEngine);
        for (GenerationResult.Match match : result.matches()) {
            GenerationRecommendationEntity recommendation = new GenerationRecommendationEntity();
            recommendation.setGenerationRun(run);
            recommendation.setMasterTestCase(match.masterTestCase());
            recommendation.setResolvedVersion(match.version());
            recommendation.setStatus(preferenceRepository.findByProjectIdAndMasterTestCaseId(projectId, match.masterTestCase().getId()).isPresent()
                    ? RecommendationStatus.IGNORED : RecommendationStatus.NEW);
            recommendation = recommendationRepository.saveAndFlush(recommendation);
            for (var rule : match.matchingRules()) {
                GenerationRecommendationRuleEntity matchedRule = new GenerationRecommendationRuleEntity();
                matchedRule.setRecommendation(recommendation);
                matchedRule.setRule(rule);
                recommendation.getMatchedRules().add(matchedRule);
                recommendationRuleRepository.save(matchedRule);
            }
        }
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public List<GenerationRunResponse> listRuns(UUID projectId, UserPrincipal principal) {
        accessPolicy.requireManage(projectId, principal);
        requireProject(projectId);
        return runRepository.findByProjectIdOrderByExecutedAtDesc(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GenerationRecommendationResponse> listRecommendations(UUID runId, UserPrincipal principal) {
        GenerationRunEntity run = requireRun(runId);
        accessPolicy.requireManage(run.getProject().getId(), principal);
        return recommendationRepository.findByGenerationRunIdOrderByCreatedAtAsc(runId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GenerationRecommendationResponse add(UUID recommendationId, UserPrincipal principal) {
        GenerationRecommendationEntity recommendation = requireRecommendation(recommendationId);
        accessPolicy.requireManage(recommendation.getGenerationRun().getProject().getId(), principal);
        if (recommendation.getStatus() == RecommendationStatus.IGNORED) {
            throw new ConflictException(ErrorCode.GENERATION_RECOMMENDATION_NOT_FOUND, "Ignored recommendation cannot be added");
        }
        var added = testPlanService.addMasterCase(recommendation.getGenerationRun().getProject().getId(),
                recommendation.getMasterTestCase().getId(), ProjectTestCaseSourceType.GENERATED, principal);
        projectTestCaseRepository.findById(added.id()).ifPresent(recommendation::setAddedProjectTestCase);
        recommendation.setStatus(RecommendationStatus.ADDED);
        recommendationRepository.save(recommendation);
        return toResponse(recommendation);
    }

    @Transactional
    public GenerationRecommendationResponse ignore(UUID recommendationId, boolean ignored, UserPrincipal principal) {
        GenerationRecommendationEntity recommendation = requireRecommendation(recommendationId);
        UUID projectId = recommendation.getGenerationRun().getProject().getId();
        accessPolicy.requireManage(projectId, principal);
        if (ignored) {
            ProjectTestCasePreferenceEntity preference = preferenceRepository.findByProjectIdAndMasterTestCaseId(
                    projectId, recommendation.getMasterTestCase().getId()).orElseGet(ProjectTestCasePreferenceEntity::new);
            preference.setProject(recommendation.getGenerationRun().getProject());
            preference.setMasterTestCase(recommendation.getMasterTestCase());
            preference.setState("IGNORED");
            preference.setUpdatedBy(requireUser(principal.getId()));
            preferenceRepository.save(preference);
            recommendation.setStatus(RecommendationStatus.IGNORED);
        } else if (recommendation.getStatus() == RecommendationStatus.IGNORED) {
            preferenceRepository.findByProjectIdAndMasterTestCaseId(projectId, recommendation.getMasterTestCase().getId())
                    .ifPresent(preferenceRepository::delete);
            recommendation.setStatus(RecommendationStatus.NEW);
        }
        return toResponse(recommendationRepository.save(recommendation));
    }

    private GenerationRunResponse toResponse(GenerationRunEntity run) {
        List<GenerationRecommendationResponse> recommendations = recommendationRepository
                .findByGenerationRunIdOrderByCreatedAtAsc(run.getId()).stream().map(this::toResponse).toList();
        return new GenerationRunResponse(run.getId(), run.getProject().getId(), run.getMode(),
                run.getTriggerType(), run.getExecutedAt(), recommendations);
    }

    private GenerationRecommendationResponse toResponse(GenerationRecommendationEntity recommendation) {
        return new GenerationRecommendationResponse(recommendation.getId(), recommendation.getGenerationRun().getId(),
                recommendation.getMasterTestCase().getId(), recommendation.getMasterTestCase().getCaseCode(),
                recommendation.getResolvedVersion().getId(), recommendation.getStatus(),
                recommendation.getMatchedRules().stream().map(GenerationRecommendationRuleEntity::getRule)
                        .map(rule -> new GenerationRecommendationResponse.MatchedRule(rule.getId(), rule.getRuleCode(), rule.getName())).toList());
    }

    private ProjectEntity requireProject(UUID id) { return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found")); }
    private GenerationRunEntity requireRun(UUID id) { return runRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GENERATION_RECOMMENDATION_NOT_FOUND, "Generation run not found")); }
    private GenerationRecommendationEntity requireRecommendation(UUID id) { return recommendationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GENERATION_RECOMMENDATION_NOT_FOUND, "Recommendation not found")); }
    private UserEntity requireUser(UUID id) { return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found")); }
}
