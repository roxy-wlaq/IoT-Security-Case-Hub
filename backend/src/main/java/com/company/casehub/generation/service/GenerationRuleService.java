package com.company.casehub.generation.service;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.entity.AuditResourceType;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.common.exception.ConflictException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.common.exception.ResourceNotFoundException;
import com.company.casehub.generation.dto.GenerationRuleRequest;
import com.company.casehub.generation.dto.GenerationRuleResponse;
import com.company.casehub.generation.entity.ConditionTargetType;
import com.company.casehub.generation.entity.GenerationConditionEntity;
import com.company.casehub.generation.entity.GenerationConditionGroupEntity;
import com.company.casehub.generation.entity.GenerationOperator;
import com.company.casehub.generation.entity.GenerationRuleEntity;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GenerationRuleOutputEntity;
import com.company.casehub.generation.entity.GroupOperator;
import com.company.casehub.generation.repository.GenerationRuleRepository;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationRuleService {

    private final GenerationRuleRepository ruleRepository;
    private final CapabilityRepository capabilityRepository;
    private final StandardTaskTypeRepository standardRepository;
    private final MasterTestCaseRepository masterRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public GenerationRuleService(GenerationRuleRepository ruleRepository,
                                 CapabilityRepository capabilityRepository,
                                 StandardTaskTypeRepository standardRepository,
                                 MasterTestCaseRepository masterRepository,
                                 UserRepository userRepository,
                                 AuditService auditService) {
        this.ruleRepository = ruleRepository;
        this.capabilityRepository = capabilityRepository;
        this.standardRepository = standardRepository;
        this.masterRepository = masterRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<GenerationRuleResponse> list() {
        return ruleRepository.findAllByOrderByRuleCodeAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GenerationRuleResponse get(UUID id) {
        return toResponse(ruleRepository.findWithGraphById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GENERATION_RULE_INVALID, "Generation Rule not found: " + id)));
    }

    @Transactional
    public GenerationRuleResponse create(GenerationRuleRequest request, UserPrincipal principal) {
        if (ruleRepository.existsByRuleCodeIgnoreCase(request.ruleCode().trim())) {
            throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "Rule code already exists");
        }
        GenerationRuleEntity rule = new GenerationRuleEntity();
        rule.setRuleCode(request.ruleCode().trim());
        rule.setName(request.name().trim());
        rule.setDescription(request.description());
        rule.setMode(request.mode() == null ? GenerationRuleMode.FULL : request.mode());
        rule.setStatus(request.status() == null ? GenerationRuleStatus.ENABLED : request.status());
        rule.setCreatedBy(userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found")));
        applyGraph(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public GenerationRuleResponse update(UUID id, GenerationRuleRequest request, UserPrincipal principal) {
        GenerationRuleEntity rule = ruleRepository.findWithGraphById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GENERATION_RULE_INVALID, "Generation Rule not found: " + id));
        if (!principal.getRoles().contains("ADMIN")) {
            throw new com.company.casehub.common.exception.ForbiddenOperationException(
                    ErrorCode.GENERATION_RULE_ACCESS_FORBIDDEN, "Only an Admin can update a Generation Rule");
        }
        rule.setName(request.name().trim());
        rule.setDescription(request.description());
        rule.setMode(request.mode() == null ? rule.getMode() : request.mode());
        rule.setStatus(request.status() == null ? rule.getStatus() : request.status());
        rule.getGroups().clear();
        rule.getOutputs().clear();
        applyGraph(rule, request);
        GenerationRuleResponse response = toResponse(ruleRepository.save(rule));
        auditService.record(AuditAction.GENERATION_RULE_UPDATE, principal, AuditResourceType.GENERATION_RULE,
                rule.getId(), rule.getRuleCode(), Map.of("status", rule.getStatus().name(), "mode", rule.getMode().name()));
        return response;
    }

    private void applyGraph(GenerationRuleEntity rule, GenerationRuleRequest request) {
        validateGroups(request.groups());
        List<GenerationConditionGroupEntity> groups = new ArrayList<>();
        for (int i = 0; i < request.groups().size(); i++) {
            GenerationRuleRequest.GroupRequest source = request.groups().get(i);
            GenerationConditionGroupEntity group = new GenerationConditionGroupEntity();
            group.setRule(rule);
            group.setLogicOperator(source.logicOperator() == null ? GroupOperator.AND : source.logicOperator());
            group.setSortOrder(source.sortOrder());
            if (source.parentGroupIndex() != null) {
                group.setParent(groups.get(source.parentGroupIndex()));
            }
            List<GenerationRuleRequest.ConditionRequest> conditions =
                    source.conditions() == null ? List.of() : source.conditions();
            for (GenerationRuleRequest.ConditionRequest conditionRequest : conditions) {
                validateCondition(conditionRequest);
                GenerationConditionEntity condition = new GenerationConditionEntity();
                condition.setGroup(group);
                condition.setTargetType(conditionRequest.targetType());
                condition.setCapability(conditionRequest.capabilityId() == null ? null
                        : capabilityRepository.findById(conditionRequest.capabilityId()).orElseThrow(
                                () -> new ResourceNotFoundException(ErrorCode.CAPABILITY_NOT_FOUND, "Capability not found")));
                condition.setStandardTaskType(conditionRequest.standardTaskTypeId() == null ? null
                        : standardRepository.findById(conditionRequest.standardTaskTypeId()).orElseThrow(
                                () -> new ResourceNotFoundException(ErrorCode.STANDARD_NOT_FOUND, "Standard not found")));
                condition.setOperator(conditionRequest.operator());
                condition.setSortOrder(conditionRequest.sortOrder());
                group.getConditions().add(condition);
            }
            groups.add(group);
            rule.getGroups().add(group);
        }
        List<MasterTestCaseEntity> outputs = masterRepository.findAllById(request.outputMasterTestCaseIds());
        if (outputs.size() != new HashSet<>(request.outputMasterTestCaseIds()).size()) {
            throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "One or more rule outputs are invalid or duplicated");
        }
        for (int i = 0; i < outputs.size(); i++) {
            GenerationRuleOutputEntity output = new GenerationRuleOutputEntity();
            output.setRule(rule);
            output.setMasterTestCase(outputs.get(i));
            output.setSortOrder(i);
            rule.getOutputs().add(output);
        }
    }

    private void validateGroups(List<GenerationRuleRequest.GroupRequest> groups) {
        long roots = groups.stream().filter(g -> g.parentGroupIndex() == null).count();
        if (roots != 1) {
            throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "A Rule must have exactly one Root Group");
        }
        for (int i = 0; i < groups.size(); i++) {
            Integer parent = groups.get(i).parentGroupIndex();
            if (parent != null && (parent < 0 || parent >= i || groups.get(parent).parentGroupIndex() != null)) {
                throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "Only one Child Group level is supported");
            }
        }
    }

    private void validateCondition(GenerationRuleRequest.ConditionRequest condition) {
        if (condition.targetType() == null || condition.operator() == null) {
            throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "Condition target and operator are required");
        }
        boolean capability = condition.targetType() == ConditionTargetType.CAPABILITY;
        if (capability != (condition.capabilityId() != null) || capability == (condition.standardTaskTypeId() != null)) {
            throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "Condition target IDs must be XOR");
        }
        if (!capability && condition.operator() != GenerationOperator.ANY && condition.operator() != GenerationOperator.PRESENT) {
            throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "Standard conditions use ANY or PRESENT");
        }
        if (capability && condition.operator() == GenerationOperator.ANY) {
            throw new ConflictException(ErrorCode.GENERATION_RULE_INVALID, "Capability conditions cannot use ANY");
        }
    }

    private GenerationRuleResponse toResponse(GenerationRuleEntity rule) {
        List<GenerationRuleResponse.GroupResponse> groups = rule.getGroups().stream()
                .map(group -> new GenerationRuleResponse.GroupResponse(group.getId(),
                        group.getParent() == null ? null : group.getParent().getId(), group.getLogicOperator(),
                        group.getSortOrder(), group.getConditions().stream()
                                .map(c -> new GenerationRuleResponse.ConditionResponse(c.getId(), c.getTargetType(),
                                        c.getCapability() == null ? null : c.getCapability().getId(),
                                        c.getStandardTaskType() == null ? null : c.getStandardTaskType().getId(),
                                        c.getOperator(), c.getSortOrder())).toList()))
                .toList();
        return new GenerationRuleResponse(rule.getId(), rule.getRuleCode(), rule.getName(), rule.getDescription(),
                rule.getMode(), rule.getStatus(), groups,
                rule.getOutputs().stream().map(o -> o.getMasterTestCase().getId()).toList());
    }
}
