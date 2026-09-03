package com.company.casehub.testcase.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.testcase.entity.TransitionType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class DagValidationServiceTest {

    @Mock private com.company.casehub.testcase.repository.DecisionPointRepository repository;
    private DagValidationService service;

    @BeforeEach
    void setUp() {
        service = new DagValidationService(repository);
    }

    @Test
    void terminalTransitionsRequireNoTargets() {
        for (TransitionType type : List.of(TransitionType.PASS, TransitionType.FAIL, TransitionType.N_A)) {
            assertThatCode(() -> service.validateTransitionTargets(type, List.of())).doesNotThrowAnyException();
            assertThatThrownBy(() -> service.validateTransitionTargets(type, List.of(UUID.randomUUID())))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_TRANSITION_TARGET_COUNT_INVALID);
        }
    }

    @Test
    void nextCaseRequiresExactlyOneAndNextCasesRequiresAtLeastOne() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThatThrownBy(() -> service.validateTransitionTargets(TransitionType.NEXT_CASE, List.of()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatCode(() -> service.validateTransitionTargets(TransitionType.NEXT_CASE, List.of(a))).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateTransitionTargets(TransitionType.NEXT_CASE, List.of(a, b)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.validateTransitionTargets(TransitionType.NEXT_CASES, List.of()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatCode(() -> service.validateTransitionTargets(TransitionType.NEXT_CASES, List.of(a, b))).doesNotThrowAnyException();
    }

    @Test
    void rejectsSelfTwoNodeAndThreeNodeCycles() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertThatThrownBy(() -> service.validateEdges(List.of(new DagValidationService.GraphEdge(a, a)), a))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DAG_CYCLE_DETECTED);
        assertThatThrownBy(() -> service.validateEdges(List.of(new DagValidationService.GraphEdge(a, b), new DagValidationService.GraphEdge(b, a)), a))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DAG_CYCLE_DETECTED);
        assertThatThrownBy(() -> service.validateEdges(List.of(new DagValidationService.GraphEdge(a, b), new DagValidationService.GraphEdge(b, c), new DagValidationService.GraphEdge(c, a)), a))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEST_CASE_DAG_CYCLE_DETECTED);
    }

    @Test
    void allowsBranchingAndConvergingDag() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        assertThatCode(() -> service.validateEdges(List.of(
                new DagValidationService.GraphEdge(a, b), new DagValidationService.GraphEdge(a, c),
                new DagValidationService.GraphEdge(b, d), new DagValidationService.GraphEdge(c, d)), a))
                .doesNotThrowAnyException();
    }
}
