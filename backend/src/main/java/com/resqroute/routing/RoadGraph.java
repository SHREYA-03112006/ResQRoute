package com.resqroute.routing;

import com.resqroute.model.RoadEdge;
import com.resqroute.model.RoadNode;
import com.resqroute.model.RoadStatus;
import lombok.Getter;

import java.util.*;

/**
 * In-memory graph structure for fast routing calculations.
 *
 * <p>Constructed from the database {@link RoadNode} and {@link RoadEdge} entities.
 * Supports cost computation based on distance, travel time, and disaster conditions:
 * <ul>
 *   <li>OPEN: multiplier = 1.0</li>
 *   <li>FLOODED: multiplier = 5.0 (hazardous, slow transit)</li>
 *   <li>UNCERTAIN: multiplier = 2.5 (cautious transit)</li>
 *   <li>BLOCKED: excluded from traversal (infinite cost)</li>
 * </ul>
 *
 * <p>Supports temporary edge penalties for generating diverse alternative routes.
 */
public class RoadGraph {

    @Getter
    private final Map<Long, RoadNode> nodes = new HashMap<>();

    @Getter
    private final Map<Long, List<GraphEdge>> adjacencyList = new HashMap<>();

    @Getter
    private final Map<Long, RoadEdge> edgeEntities = new HashMap<>();

    /**
     * Internal representation of a directed graph edge for traversal.
     */
    public record GraphEdge(
        Long edgeId,
        Long fromNodeId,
        Long toNodeId,
        String roadName,
        String roadType,
        double distanceKm,
        double baseTravelTimeMinutes,
        RoadStatus status,
        double penaltyMultiplier
    ) {
        /**
         * Calculates the dynamic traversal cost in minutes.
         * Returns Double.POSITIVE_INFINITY if the road is BLOCKED.
         */
        public double getEffectiveCost() {
            if (status == RoadStatus.BLOCKED) {
                return Double.POSITIVE_INFINITY;
            }
            double conditionMultiplier = switch (status) {
                case OPEN, UNKNOWN -> 1.0;
                case FLOODED -> 5.0;     // Significant flood penalty
                case UNCERTAIN -> 2.5;   // Cautious condition penalty
                case BLOCKED -> Double.POSITIVE_INFINITY;
                default -> 1.0;
            };
            return baseTravelTimeMinutes * conditionMultiplier * penaltyMultiplier;
        }

        public boolean isTraversable() {
            return status != RoadStatus.BLOCKED && !Double.isInfinite(getEffectiveCost());
        }
    }

    public void addNode(RoadNode node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(RoadEdge edge, double penaltyMultiplier) {
        edgeEntities.put(edge.getId(), edge);
        Long fromId = edge.getFromNode().getId();
        Long toId = edge.getToNode().getId();

        adjacencyList.putIfAbsent(fromId, new ArrayList<>());
        adjacencyList.get(fromId).add(new GraphEdge(
            edge.getId(),
            fromId,
            toId,
            edge.getRoadName(),
            edge.getRoadType(),
            edge.getDistanceKm(),
            edge.getBaseTravelTimeMinutes(),
            edge.getStatus(),
            penaltyMultiplier
        ));

        // If bidirectional, add reverse edge as well
        if (Boolean.TRUE.equals(edge.getBidirectional())) {
            adjacencyList.putIfAbsent(toId, new ArrayList<>());
            adjacencyList.get(toId).add(new GraphEdge(
                edge.getId(),
                toId,
                fromId,
                edge.getRoadName(),
                edge.getRoadType(),
                edge.getDistanceKm(),
                edge.getBaseTravelTimeMinutes(),
                edge.getStatus(),
                penaltyMultiplier
            ));
        }
    }

    public List<GraphEdge> getOutgoingEdges(Long nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public RoadNode getNode(Long nodeId) {
        return nodes.get(nodeId);
    }

    public RoadEdge getEdgeEntity(Long edgeId) {
        return edgeEntities.get(edgeId);
    }
}
