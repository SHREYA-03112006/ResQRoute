package com.resqroute.routing;

import com.resqroute.model.RoadEdge;
import com.resqroute.model.RoadNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generates distinct alternative routes between two points.
 *
 * <p>Uses an edge-penalty variation of K-shortest paths:
 * <ol>
 *   <li>Calculates the primary optimal route using the selected router.</li>
 *   <li>Applies a penalty multiplier to the edges used by the primary route to encourage
 *       the routing engine to explore viable detours (e.g., bypasses, secondary mountain passes).</li>
 *   <li>Calculates alternative routes sequentially, ensuring each proposed alternative is distinct.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlternativeRoutesGenerator {

    private final DijkstraRouter dijkstraRouter;
    private final AStarRouter aStarRouter;

    public List<RouteResult> generateRoutes(
        List<RoadNode> allNodes,
        List<RoadEdge> allEdges,
        Long startNodeId,
        Long targetNodeId,
        String algorithm,
        int maxAlternatives
    ) {
        List<RouteResult> results = new ArrayList<>();

        // 1. Compute Primary Route
        RoadGraph baseGraph = buildGraph(allNodes, allEdges, Collections.emptyMap());
        Optional<RouteResult> primary = route(baseGraph, startNodeId, targetNodeId, algorithm);

        if (primary.isEmpty()) {
            return results;
        }

        results.add(primary.get());

        if (maxAlternatives <= 0) {
            return results;
        }

        // Set of seen edge lists to prevent duplicate routes
        Set<List<Long>> seenEdgePaths = new HashSet<>();
        seenEdgePaths.add(primary.get().edgeIds());

        Map<Long, Double> accumulatedPenalties = new HashMap<>();

        // Penalize edges from primary route (1.8x)
        for (Long edgeId : primary.get().edgeIds()) {
            accumulatedPenalties.put(edgeId, 1.8);
        }

        for (int altIndex = 1; altIndex <= maxAlternatives; altIndex++) {
            RoadGraph penalizedGraph = buildGraph(allNodes, allEdges, accumulatedPenalties);
            Optional<RouteResult> alt = route(penalizedGraph, startNodeId, targetNodeId, algorithm);

            if (alt.isEmpty()) {
                break;
            }

            RouteResult altRoute = alt.get();
            if (seenEdgePaths.contains(altRoute.edgeIds())) {
                // Try higher penalty on common bottleneck edges
                for (Long edgeId : altRoute.edgeIds()) {
                    accumulatedPenalties.merge(edgeId, 2.5, (oldV, newV) -> oldV * 1.5);
                }
                RoadGraph stronglyPenalizedGraph = buildGraph(allNodes, allEdges, accumulatedPenalties);
                alt = route(stronglyPenalizedGraph, startNodeId, targetNodeId, algorithm);
                if (alt.isEmpty() || seenEdgePaths.contains(alt.get().edgeIds())) {
                    break;
                }
                altRoute = alt.get();
            }

            seenEdgePaths.add(altRoute.edgeIds());
            results.add(altRoute);

            // Add penalties for this alternative's edges as well
            for (Long edgeId : altRoute.edgeIds()) {
                accumulatedPenalties.merge(edgeId, 1.8, (oldV, newV) -> oldV * 1.5);
            }
        }

        return results;
    }

    private Optional<RouteResult> route(RoadGraph graph, Long startNodeId, Long targetNodeId, String algorithm) {
        if ("A_STAR".equalsIgnoreCase(algorithm)) {
            return aStarRouter.findRoute(graph, startNodeId, targetNodeId);
        }
        return dijkstraRouter.findRoute(graph, startNodeId, targetNodeId);
    }

    private RoadGraph buildGraph(List<RoadNode> nodes, List<RoadEdge> edges, Map<Long, Double> penalties) {
        RoadGraph graph = new RoadGraph();
        for (RoadNode node : nodes) {
            graph.addNode(node);
        }
        for (RoadEdge edge : edges) {
            double penalty = penalties.getOrDefault(edge.getId(), 1.0);
            graph.addEdge(edge, penalty);
        }
        return graph;
    }
}
