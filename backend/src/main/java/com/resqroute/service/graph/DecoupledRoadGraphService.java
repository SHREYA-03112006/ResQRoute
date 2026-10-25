package com.resqroute.service.graph;

import com.resqroute.model.GraphEdge;
import com.resqroute.model.RoadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decoupled road graph service for Chamoli / Rudraprayag pilot district.
 *
 * <p>Maintains edge costs, travel times, and status updates driven by disaster reports.
 * Designed to seamlessly interface with Dijkstra/A* routing in Phase 2 and OSM ingestion.
 */
@Service
public class DecoupledRoadGraphService {

    private static final Logger log = LoggerFactory.getLogger(DecoupledRoadGraphService.class);

    private final Map<Long, GraphEdge> edges = new ConcurrentHashMap<>();

    @Value("${resqroute.routing.flood-penalty-multiplier:5.0}")
    private double floodPenaltyMultiplier;

    @PostConstruct
    public void initPilotDistrictNetwork() {
        log.info("Initializing Chamoli/Rudraprayag pilot district road graph (decoupled)...");

        // Kedarnath Corridor (NH-107)
        addEdge(1L, "NH 107 Rudraprayag-Tilwara", "NH-107", "Rudraprayag", "Tilwara", 30.285, 78.981, 30.292, 78.962, 9.5, 35.0);
        addEdge(2L, "NH 107 Tilwara-Agastmuni", "NH-107", "Tilwara", "Agastmuni", 30.292, 78.962, 30.395, 79.031, 16.2, 35.0);
        addEdge(3L, "NH 107 Agastmuni-Kund", "NH-107", "Agastmuni", "Kund", 30.395, 79.031, 30.501, 79.112, 18.0, 30.0);
        addEdge(4L, "NH 107 Kund-Guptkashi", "NH-107", "Kund", "Guptkashi", 30.501, 79.112, 30.523, 79.082, 14.5, 30.0);
        addEdge(5L, "NH 107 Guptkashi-Gaurikund", "NH-107", "Guptkashi", "Gaurikund", 30.523, 79.082, 30.645, 79.266, 28.0, 25.0);

        // Badrinath Corridor (NH-7 / Old NH-58)
        addEdge(6L, "NH 7 Rudraprayag-Karnaprayag", "NH-7", "Rudraprayag", "Karnaprayag", 30.285, 78.981, 30.258, 79.219, 32.0, 40.0);
        addEdge(7L, "NH 7 Karnaprayag-Nandprayag", "NH-7", "Karnaprayag", "Nandprayag", 30.258, 79.219, 30.331, 79.324, 21.0, 40.0);
        addEdge(8L, "NH 7 Nandprayag-Chamoli", "NH-7", "Nandprayag", "Chamoli", 30.331, 79.324, 30.396, 79.321, 11.5, 40.0);
        addEdge(9L, "NH 7 Chamoli-Pipalkoti", "NH-7", "Chamoli", "Pipalkoti", 30.396, 79.321, 30.473, 79.479, 17.0, 35.0);
        addEdge(10L, "NH 7 Pipalkoti-Joshimath", "NH-7", "Pipalkoti", "Joshimath", 30.473, 79.479, 30.556, 79.567, 34.0, 30.0);

        // High Altitude Bypass / Alternative Corridor (Kund - Chopta - Mandal - Gopeshwar)
        addEdge(11L, "SH 64 Kund-Chopta-Mandal", "SH-64", "Kund", "Mandal", 30.501, 79.112, 30.461, 79.278, 38.0, 25.0);
        addEdge(12L, "SH 64 Mandal-Gopeshwar Chamoli", "SH-64", "Mandal", "Chamoli", 30.461, 79.278, 30.396, 79.321, 14.0, 30.0);

        log.info("Initialized {} road graph edges for pilot district.", edges.size());
    }

    private void addEdge(Long id, String name, String code, String from, String to,
                         double fromLat, double fromLon, double toLat, double toLon,
                         double distKm, double speedKmh) {
        GraphEdge edge = new GraphEdge(id, name, code, from, to, fromLat, fromLon, toLat, toLon, distKm, speedKmh);
        edges.put(id, edge);
    }

    public List<GraphEdge> getAllEdges() {
        return new ArrayList<>(edges.values());
    }

    public Optional<GraphEdge> getEdgeById(Long id) {
        return Optional.ofNullable(edges.get(id));
    }

    /**
     * Updates an edge's operational condition based on an accepted disaster report.
     */
    public synchronized boolean updateEdgeCondition(Long edgeId, RoadStatus status, Long reportId) {
        GraphEdge edge = edges.get(edgeId);
        if (edge == null) {
            log.warn("Attempted to update non-existent edge id={}", edgeId);
            return false;
        }

        edge.setStatus(status);
        edge.setActiveReportId(reportId);

        switch (status) {
            case BLOCKED -> {
                // Impassable: infinite travel cost excludes it from normal routing
                edge.setCurrentCostMinutes(Double.POSITIVE_INFINITY);
                log.info("Edge {} ({}) marked BLOCKED (cost: INFINITY).", edgeId, edge.getRoadName());
            }
            case FLOODED -> {
                // Flooded: configured penalty multiplier applied
                double penaltyCost = edge.getNormalCostMinutes() * floodPenaltyMultiplier;
                edge.setCurrentCostMinutes(penaltyCost);
                log.info("Edge {} ({}) marked FLOODED (cost: {:.1f} min, penalty: {}x).",
                        edgeId, edge.getRoadName(), penaltyCost, floodPenaltyMultiplier);
            }
            case UNCERTAIN -> {
                // Uncertain: conservative 1.5x buffer
                double conservativeCost = edge.getNormalCostMinutes() * 1.5;
                edge.setCurrentCostMinutes(conservativeCost);
                log.info("Edge {} ({}) marked UNCERTAIN (cost: {:.1f} min).",
                        edgeId, edge.getRoadName(), conservativeCost);
            }
            case OPEN -> {
                // Open: reset to baseline
                edge.setCurrentCostMinutes(edge.getNormalCostMinutes());
                log.info("Edge {} ({}) restored to OPEN (cost: {:.1f} min).",
                        edgeId, edge.getRoadName(), edge.getNormalCostMinutes());
            }
        }
        return true;
    }

    public void resetAllEdges() {
        for (GraphEdge edge : edges.values()) {
            edge.setStatus(RoadStatus.OPEN);
            edge.setCurrentCostMinutes(edge.getNormalCostMinutes());
            edge.setActiveReportId(null);
        }
        log.info("Reset all graph edges to OPEN status.");
    }
}
