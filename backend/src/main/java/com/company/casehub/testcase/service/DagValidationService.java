package com.company.casehub.testcase.service;

import com.company.casehub.common.exception.BusinessRuleException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.testcase.entity.DecisionPointEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TransitionTargetEntity;
import com.company.casehub.testcase.entity.TransitionType;
import com.company.casehub.testcase.repository.DecisionPointRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Validates the Master Test Case graph, independently of Project Runtime. */
@Service
public class DagValidationService {

    private final DecisionPointRepository decisionPointRepository;

    public DagValidationService(DecisionPointRepository decisionPointRepository) {
        this.decisionPointRepository = decisionPointRepository;
    }

    public void validateTransitionTargets(TransitionType type, Collection<UUID> targetIds) {
        int count = targetIds == null ? 0 : targetIds.size();
        boolean valid = switch (type) {
            case PASS, FAIL, N_A -> count == 0;
            case NEXT_CASE -> count == 1;
            case NEXT_CASES -> count >= 1;
        };
        if (!valid) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_TRANSITION_TARGET_COUNT_INVALID,
                    "Transition " + type + " requires a different number of targets (received " + count + ").");
        }
    }

    /** Validates every edge reachable from the edited version's Master node. */
    public void validateVersion(TestCaseVersionEntity version) {
        List<DecisionPointEntity> points = decisionPointRepository.findAllWithGraphBy();
        Map<UUID, Set<UUID>> graph = toGraph(points);
        ensureAcyclicFrom(version.getMasterTestCase().getId(), graph);
    }

    public void validateEdges(Collection<GraphEdge> edges, UUID root) {
        Map<UUID, Set<UUID>> graph = new HashMap<>();
        for (GraphEdge edge : edges) {
            graph.computeIfAbsent(edge.source(), ignored -> new HashSet<>()).add(edge.target());
        }
        ensureAcyclicFrom(root, graph);
    }

    public static Map<UUID, Set<UUID>> toGraph(Collection<DecisionPointEntity> points) {
        Map<UUID, Set<UUID>> graph = new HashMap<>();
        for (DecisionPointEntity point : points) {
            if (point.getTransition() == null || point.getTestCaseVersion() == null
                    || point.getTestCaseVersion().getMasterTestCase() == null) {
                continue;
            }
            UUID source = point.getTestCaseVersion().getMasterTestCase().getId();
            for (TransitionTargetEntity target : point.getTransition().getTargets()) {
                graph.computeIfAbsent(source, ignored -> new HashSet<>()).add(target.getTargetMasterTestCase().getId());
            }
        }
        return graph;
    }

    private void ensureAcyclicFrom(UUID root, Map<UUID, Set<UUID>> graph) {
        Set<UUID> visiting = new HashSet<>();
        Set<UUID> visited = new HashSet<>();
        if (hasCycle(root, graph, visiting, visited)) {
            throw new BusinessRuleException(ErrorCode.TEST_CASE_DAG_CYCLE_DETECTED,
                    "The Master Test Case logic graph contains a cycle reachable from " + root + ".");
        }
    }

    private boolean hasCycle(UUID node, Map<UUID, Set<UUID>> graph, Set<UUID> visiting, Set<UUID> visited) {
        if (visiting.contains(node)) return true;
        if (visited.contains(node)) return false;
        visiting.add(node);
        for (UUID target : graph.getOrDefault(node, Set.of())) {
            if (hasCycle(target, graph, visiting, visited)) return true;
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    public record GraphEdge(UUID source, UUID target) {
    }
}
