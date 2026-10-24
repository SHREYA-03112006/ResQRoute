package com.resqroute.routing;

import com.resqroute.model.RoadNode;
import com.resqroute.model.RoadStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A* shortest path routing algorithm implementation for ResQ-Route.
 *
 * <p>Uses the Haversine great-circle distance divided by maximum plausible mountain speed (60 km/h)
 * as an admissible heuristic function h(n) towards the target node.
 *
 * <p>Guarantees optimality while significantly reducing node expansions compared to Dijkstra.
 */
@Component
@Slf4j
public class AStarRouter {

    private record AStarEntry(Long nodeId, double fScore) implements Comparable<AStarEntry> {
        @Override
        public int compareTo(AStarEntry other) {
            return Double.compare(this.fScore, other.fScore);
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
                "A_STAR",
                "Start and destination are identical"
            ));
        }

        RoadNode targetNode = graph.getNode(targetNodeId);
        double targetLat = targetNode.getLatitude();
        double targetLon = targetNode.getLongitude();

        Map<Long, Double> gScore = new HashMap<>();
        Map<Long, RoadGraph.GraphEdge> edgeTo = new HashMap<>();
        PriorityQueue<AStarEntry> openSet = new PriorityQueue<>();

        gScore.put(startNodeId, 0.0);
        RoadNode startNode = graph.getNode(startNodeId);
        double initialH = HaversineUtil.heuristicMinutes(startNode.getLatitude(), startNode.getLongitude(), targetLat, targetLon);
        openSet.add(new AStarEntry(startNodeId, initialH));

        boolean reachedTarget = false;

        while (!openSet.isEmpty()) {
            AStarEntry current = openSet.poll();
            Long u = current.nodeId();

            if (u.equals(targetNodeId)) {
                reachedTarget = true;
                break;
            }

            double currentG = gScore.getOrDefault(u, Double.POSITIVE_INFINITY);

            for (RoadGraph.GraphEdge edge : graph.getOutgoingEdges(u)) {
                if (!edge.isTraversable()) {
                    continue; // Skip BLOCKED roads
                }

                Long v = edge.toNodeId();
                double tentativeG = currentG + edge.getEffectiveCost();

                if (tentativeG < gScore.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    edgeTo.put(v, edge);
                    gScore.put(v, tentativeG);

                    RoadNode neighborNode = graph.getNode(v);
                    double h = HaversineUtil.heuristicMinutes(
                        neighborNode.getLatitude(), neighborNode.getLongitude(),
                        targetLat, targetLon
                    );
                    double fScore = tentativeG + h;
                    openSet.add(new AStarEntry(v, fScore));
                }
            }
        }

        if (!reachedTarget) {
            log.info("A* search found no traversable path from node {} to {}", startNodeId, targetNodeId);
            return Optional.empty();
        }

        // Reconstruct path
        LinkedList<Long> nodePath = new LinkedList<>();
        LinkedList<Long> edgePath = new LinkedList<>();
        LinkedList<double[]> waypoints = new LinkedList<>();
        LinkedList<String> roadSegments = new LinkedList<>();

        Long curr = targetNodeId;
        nodePath.addFirst(curr);
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

        boolean hasBlockedDetour = graph.getEdgeEntities().values().stream()
            .anyMatch(e -> e.getStatus() == RoadStatus.BLOCKED);

        String costAssumptions = String.format(
            "A* heuristic cost model: Haversine distance (max 60 km/h admissible lower bound) + dynamic conditions (FLOODED 5x, UNCERTAIN 2.5x). Total edges: %d",
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
            "A_STAR",
            costAssumptions
        ));
    }
}
