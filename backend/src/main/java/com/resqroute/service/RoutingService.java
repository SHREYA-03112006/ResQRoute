package com.resqroute.service;

import com.resqroute.model.*;
import com.resqroute.repository.*;
import com.resqroute.routing.AlternativeRoutesGenerator;
import com.resqroute.routing.HaversineUtil;
import com.resqroute.routing.RouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * High-level service orchestrating graph-based disaster route calculations.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Snap locations or coordinates to the nearest OSM road graph node.</li>
 *   <li>Calculate primary route and multiple distinct alternatives using Dijkstra or A*.</li>
 *   <li>Dynamically apply road conditions (FLOODED, BLOCKED, UNCERTAIN) to graph edges.</li>
 *   <li>Persist and manage dispatcher route-planning approval requests for human-in-the-loop oversight.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final LocationRepository locationRepository;
    private final RouteRequestRepository routeRequestRepository;
    private final AlternativeRoutesGenerator alternativeRoutesGenerator;

    public record RouteCalculationResponse(
        RouteResult primaryRoute,
        List<RouteResult> alternativeRoutes,
        Location originLocation,
        Location destinationLocation,
        RoadNode snappedOriginNode,
        RoadNode snappedDestinationNode,
        String calculationSummary
    ) {}

    /**
     * Calculates routes between two registered locations (depot to relief camp/hospital).
     */
    @Transactional(readOnly = true)
    public RouteCalculationResponse calculateRouteBetweenLocations(
        Long originLocationId,
        Long destinationLocationId,
        String algorithm,
        int maxAlternatives
    ) {
        Location origin = locationRepository.findById(originLocationId)
            .orElseThrow(() -> new IllegalArgumentException("Origin location not found: " + originLocationId));
        Location destination = locationRepository.findById(destinationLocationId)
            .orElseThrow(() -> new IllegalArgumentException("Destination location not found: " + destinationLocationId));

        RoadNode startNode = snapToNearestNode(origin.getLatitude(), origin.getLongitude());
        RoadNode targetNode = snapToNearestNode(destination.getLatitude(), destination.getLongitude());

        return executeRouting(origin, destination, startNode, targetNode, algorithm, maxAlternatives);
    }

    /**
     * Calculates routes between arbitrary GPS coordinates.
     */
    @Transactional(readOnly = true)
    public RouteCalculationResponse calculateRouteCoordinates(
        double startLat, double startLon,
        double endLat, double endLon,
        String algorithm,
        int maxAlternatives
    ) {
        RoadNode startNode = snapToNearestNode(startLat, startLon);
        RoadNode targetNode = snapToNearestNode(endLat, endLon);

        Location simulatedOrigin = Location.builder()
            .name("Selected Point A")
            .type("WAYPOINT")
            .latitude(startLat).longitude(startLon)
            .build();

        Location simulatedDestination = Location.builder()
            .name("Selected Point B")
            .type("WAYPOINT")
            .latitude(endLat).longitude(endLon)
            .build();

        return executeRouting(simulatedOrigin, simulatedDestination, startNode, targetNode, algorithm, maxAlternatives);
    }

    private RouteCalculationResponse executeRouting(
        Location origin,
        Location destination,
        RoadNode startNode,
        RoadNode targetNode,
        String algorithm,
        int maxAlternatives
    ) {
        List<RoadNode> allNodes = roadNodeRepository.findAll();
        List<RoadEdge> allEdges = roadEdgeRepository.findAllWithNodes();

        String algo = (algorithm != null && algorithm.equalsIgnoreCase("A_STAR")) ? "A_STAR" : "DIJKSTRA";

        List<RouteResult> allRoutes = alternativeRoutesGenerator.generateRoutes(
            allNodes,
            allEdges,
            startNode.getId(),
            targetNode.getId(),
            algo,
            maxAlternatives
        );

        if (allRoutes.isEmpty()) {
            throw new IllegalStateException(String.format(
                "No viable route found between %s (node %d) and %s (node %d). Road blockages may have isolated destination.",
                origin.getName(), startNode.getId(), destination.getName(), targetNode.getId()
            ));
        }

        RouteResult primary = allRoutes.get(0);
        List<RouteResult> alternatives = allRoutes.size() > 1
            ? allRoutes.subList(1, allRoutes.size())
            : Collections.emptyList();

        String summary = String.format(
            "Calculated %s route (%s to %s): %.2f km, est. %.1f mins. %d alternative(s) generated.",
            algo, origin.getName(), destination.getName(),
            primary.totalDistanceKm(), primary.estimatedTimeMinutes(),
            alternatives.size()
        );

        return new RouteCalculationResponse(
            primary,
            alternatives,
            origin,
            destination,
            startNode,
            targetNode,
            summary
        );
    }

    /**
     * Snaps a geographic coordinate to the nearest road network node using Haversine distance.
     */
    public RoadNode snapToNearestNode(double lat, double lon) {
        List<RoadNode> nodes = roadNodeRepository.findAll();
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Road network is empty. Please seed road graph first.");
        }

        RoadNode nearest = null;
        double minDistance = Double.POSITIVE_INFINITY;

        for (RoadNode node : nodes) {
            double dist = HaversineUtil.distanceKm(lat, lon, node.getLatitude(), node.getLongitude());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = node;
            }
        }
        return nearest;
    }

    public record RoadEdgeDto(
        Long id,
        Long fromNodeId,
        String fromNodeName,
        double fromLat,
        double fromLon,
        Long toNodeId,
        String toNodeName,
        double toLat,
        double toLon,
        String roadName,
        String roadType,
        double distanceKm,
        double baseTravelTimeMinutes,
        RoadStatus status,
        boolean bidirectional,
        Long osmWayId
    ) {}

    /**
     * Returns the full road network nodes and edges for client-side rendering.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoadNetworkGraph() {
        List<RoadNode> nodes = roadNodeRepository.findAll();
        List<RoadEdge> edges = roadEdgeRepository.findAllWithNodes();

        List<RoadEdgeDto> edgeDtos = edges.stream().map(e -> new RoadEdgeDto(
            e.getId(),
            e.getFromNode().getId(),
            e.getFromNode().getName(),
            e.getFromNode().getLatitude(),
            e.getFromNode().getLongitude(),
            e.getToNode().getId(),
            e.getToNode().getName(),
            e.getToNode().getLatitude(),
            e.getToNode().getLongitude(),
            e.getRoadName(),
            e.getRoadType(),
            e.getDistanceKm(),
            e.getBaseTravelTimeMinutes(),
            e.getStatus(),
            Boolean.TRUE.equals(e.getBidirectional()),
            e.getOsmWayId()
        )).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edgeDtos);
        result.put("totalNodes", nodes.size());
        result.put("totalEdges", edgeDtos.size());
        return result;
    }

    /**
     * Updates an edge's status dynamically (e.g. when road reports confirm flooding or blockage).
     */
    @Transactional
    public RoadEdge updateEdgeStatus(Long edgeId, RoadStatus status) {
        RoadEdge edge = roadEdgeRepository.findById(edgeId)
            .orElseThrow(() -> new IllegalArgumentException("Road edge not found: " + edgeId));
        edge.setStatus(status);
        log.info("Updated road edge #{} ({}) status to {}", edgeId, edge.getRoadName(), status);
        return roadEdgeRepository.save(edge);
    }

    /**
     * Saves a route planning request for dispatcher review and audit.
     */
    @Transactional
    public RouteRequest saveRouteRequest(Long originId, Long destinationId, String summaryJson, int altCount) {
        Location origin = locationRepository.findById(originId)
            .orElseThrow(() -> new IllegalArgumentException("Origin not found: " + originId));
        Location destination = locationRepository.findById(destinationId)
            .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + destinationId));

        RouteRequest req = RouteRequest.builder()
            .origin(origin)
            .destination(destination)
            .routeSummaryJson(summaryJson)
            .alternativeCount(altCount)
            .approvalStatus("PENDING")
            .build();

        return routeRequestRepository.save(req);
    }

    /**
     * Records a dispatcher's review decision (APPROVED or REJECTED) with audit notes.
     */
    @Transactional
    public RouteRequest reviewRouteRequest(Long requestId, String status, String notes) {
        RouteRequest req = routeRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Route request not found: " + requestId));

        req.setApprovalStatus(status.toUpperCase());
        req.setDispatcherNotes(notes);
        req.setReviewedAt(Instant.now());

        log.info("Route request #{} reviewed by dispatcher: status={}, notes={}", requestId, status, notes);
        return routeRequestRepository.save(req);
    }

    @Transactional(readOnly = true)
    public List<RouteRequest> getAllRouteRequests() {
        return routeRequestRepository.findAll();
    }
}
