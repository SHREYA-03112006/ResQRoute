package com.resqroute.routing;

import com.resqroute.model.RoadNode;
import com.resqroute.model.RoadStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Dijkstra shortest path routing algorithm implementation for ResQ-Route.
 *
 * <p>Finds the path minimizing effective travel time while avoiding BLOCKED segments
 * and heavily penalizing FLOODED and UNCERTAIN segments.
 */
@Component
@Slf4j
public class DijkstraRouter {

    private record NodeEntry(Long nodeId, double cost) implements Comparable<NodeEntry> {
        @Override
        public int compareTo(NodeEntry other) {
            return Double.compare(this.cost, other.cost);
        }
    }

    public Optional<RouteResult> findRoute(RoadGraph graph, Long startNodeId, Long targetNodeId) {
        if (!graph.getNodes().containsKey(startNodeId) || !graph.getNodes().containsKey(targetNodeId)) {
            log.warn("Start node {} or target node {} not found in graph", startNodeId, targetNodeId);
            return Optional.empty();
        }

        if (startNodeId.equals(targetNodeId)) {
            RoadNode node = graph.getNode(startNodeId);
            return Optional.of(new RouteResult(
                List.of(startNodeId),
                List.of(),
                List.of(new double[]{node.getLatitude(), node.getLongitude()}),
                List.of("Origin/Destination same"),
                0.0,
                0.0,
                0,
                0,
                false,
                "DIJKSTRA",
                "Start and destination are identical"
            ));
        }

        Map<Long, Double> distTo = new HashMap<>();
        Map<Long, RoadGraph.GraphEdge> edgeTo = new HashMap<>();
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>();

        distTo.put(startNodeId, 0.0);
        pq.add(new NodeEntry(startNodeId, 0.0));

        boolean reachedTarget = false;

        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();
            Long u = current.nodeId();

            if (current.cost() > distTo.getOrDefault(u, Double.POSITIVE_INFINITY)) {
                continue;
            }

            if (u.equals(targetNodeId)) {
                reachedTarget = true;
                break;
            }

            for (RoadGraph.GraphEdge edge : graph.getOutgoingEdges(u)) {
                if (!edge.isTraversable()) {
                    continue; // Skip BLOCKED edges
                }

                Long v = edge.toNodeId();
                double edgeCost = edge.getEffectiveCost();
                double newDist = distTo.get(u) + edgeCost;

                if (newDist < distTo.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    distTo.put(v, newDist);
                    edgeTo.put(v, edge);
                    pq.add(new NodeEntry(v, newDist));
                }
            }
        }

        if (!reachedTarget) {
            log.info("No traversable route found between node {} and node {}", startNodeId, targetNodeId);
            return Optional.empty();
        }

        // Reconstruct path
        LinkedList<Long> nodePath = new LinkedList<>();
        LinkedList<Long> edgePath = new LinkedList<>();
        LinkedList<double[]> waypoints = new LinkedList<>();
        LinkedList<String> roadSegments = new LinkedList<>();

        Long curr = targetNodeId;
        nodePath.addFirst(curr);
        RoadNode targetNode = graph.getNode(curr);
        waypoints.addFirst(new double[]{targetNode.getLatitude(), targetNode.getLongitude()});

        double totalDistanceKm = 0.0;
        double estimatedTimeMinutes = 0.0;
        int floodedSegments = 0;
        int uncertainSegments = 0;

        while (!curr.equals(startNodeId)) {
            RoadGraph.GraphEdge edge = edgeTo.get(curr);
            if (edge == null) {
                log.error("Broken predecessor pointer at node {}", curr);
                return Optional.empty();
            }

            edgePath.addFirst(edge.edgeId());
            roadSegments.addFirst(edge.roadName() != null ? edge.roadName() : "Unnamed Road");
            totalDistanceKm += edge.distanceKm();
            estimatedTimeMinutes += edge.getEffectiveCost();

            if (edge.status() == RoadStatus.FLOODED) {
                floodedSegments++;
            } else if (edge.status() == RoadStatus.UNCERTAIN) {
                uncertainSegments++;
            }

            curr = edge.fromNodeId();
            nodePath.addFirst(curr);
            RoadNode node = graph.getNode(curr);
            waypoints.addFirst(new double[]{node.getLatitude(), node.getLongitude()});
        }

        // Check if any edges in graph are BLOCKED
        boolean hasBlockedDetour = graph.getEdgeEntities().values().stream()
            .anyMatch(e -> e.getStatus() == RoadStatus.BLOCKED);

        String costAssumptions = String.format(
            "Dijkstra cost model: OPEN (1.0x), FLOODED (5.0x penalty), UNCERTAIN (2.5x penalty), BLOCKED (excluded). Total edges: %d",
            edgePath.size()
        );

        return Optional.of(new RouteResult(
            nodePath,
            edgePath,
            waypoints,
            roadSegments,
            Math.round(totalDistanceKm * 100.0) / 100.0,
            Math.round(estimatedTimeMinutes * 10.0) / 10.0,
            floodedSegments,
            uncertainSegments,
            hasBlockedDetour,
            "DIJKSTRA",
            costAssumptions
        ));
    }
}
