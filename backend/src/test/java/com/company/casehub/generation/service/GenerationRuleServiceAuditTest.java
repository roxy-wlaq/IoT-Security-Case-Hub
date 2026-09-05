package com.company.casehub.generation.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.capability.repository.CapabilityRepository;
import com.company.casehub.generation.dto.GenerationRuleRequest;
import com.company.casehub.generation.entity.GenerationRuleEntity;
import com.company.casehub.generation.entity.GenerationRuleMode;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import com.company.casehub.generation.entity.GroupOperator;
import com.company.casehub.generation.repository.GenerationRuleRepository;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerationRuleServiceAuditTest {

    @Mock private GenerationRuleRepository ruleRepository;
    @Mock private CapabilityRepository capabilityRepository;
    @Mock private StandardTaskTypeRepository standardRepository;
    @Mock private MasterTestCaseRepository masterRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @Test
    void updateEmitsOneGenerationRuleAuditEvent() {
        UUID ruleId = UUID.randomUUID();
        UUID outputId = UUID.randomUUID();
        GenerationRuleEntity rule = new GenerationRuleEntity();
        rule.setId(ruleId);
        rule.setRuleCode("RULE-1");
        rule.setName("Old");
        rule.setMode(GenerationRuleMode.FULL);
        rule.setStatus(GenerationRuleStatus.ENABLED);
        MasterTestCaseEntity output = new MasterTestCaseEntity();
        output.setId(outputId);
        UserPrincipal actor = new UserPrincipal(UUID.randomUUID(), "admin", "hash", "Admin", true, false,
                Set.of("ADMIN"), Set.of("generation_rule:manage"));
        when(ruleRepository.findWithGraphById(ruleId)).thenReturn(Optional.of(rule));
        when(masterRepository.findAllById(List.of(outputId))).thenReturn(List.of(output));
        when(ruleRepository.save(any(GenerationRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GenerationRuleRequest request = new GenerationRuleRequest("RULE-1", "New", null,
                GenerationRuleMode.FULL, GenerationRuleStatus.ENABLED,
                List.of(new GenerationRuleRequest.GroupRequest(null, GroupOperator.AND, 0, List.of())),
                List.of(outputId));

        new GenerationRuleService(ruleRepository, capabilityRepository, standardRepository, masterRepository,
                userRepository, auditService).update(ruleId, request, actor);

        verify(auditService).record(eq(AuditAction.GENERATION_RULE_UPDATE), eq(actor), eq("GENERATION_RULE"),
                eq(ruleId), eq("RULE-1"), anyMap());
    }
}
