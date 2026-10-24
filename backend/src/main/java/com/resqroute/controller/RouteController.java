package com.resqroute.controller;

import com.resqroute.model.RoadEdge;
import com.resqroute.model.RoadStatus;
import com.resqroute.model.RouteRequest;
import com.resqroute.service.RoutingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for road network routing, path calculations,
 * and dispatcher approval workflow.
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Slf4j
public class RouteController {

    private final RoutingService routingService;

    @Data
    public static class CalculateRouteDto {
        private Long originLocationId;
        private Long destinationLocationId;
        private Double startLat;
        private Double startLon;
        private Double endLat;
        private Double endLon;
        private String algorithm = "DIJKSTRA";
        private int maxAlternatives = 2;
    }

    @Data
    public static class ReviewRouteDto {
        private String notes;
    }

    @Data
    public static class SaveRequestDto {
        private Long originLocationId;
        private Long destinationLocationId;
        private String routeSummaryJson;
        private int alternativeCount;
    }

    @Data
    public static class UpdateEdgeStatusDto {
        private RoadStatus status;
    }

    /**
     * Calculates the primary and alternative routes.
     */
    @PostMapping("/calculate")
    public ResponseEntity<RoutingService.RouteCalculationResponse> calculateRoute(@RequestBody CalculateRouteDto dto) {
        log.info("Calculating route: originId={}, destId={}, coords=[{},{}]->[{},{}], algo={}",
            dto.getOriginLocationId(), dto.getDestinationLocationId(),
            dto.getStartLat(), dto.getStartLon(), dto.getEndLat(), dto.getEndLon(),
            dto.getAlgorithm());

        if (dto.getOriginLocationId() != null && dto.getDestinationLocationId() != null) {
            RoutingService.RouteCalculationResponse response = routingService.calculateRouteBetweenLocations(
                dto.getOriginLocationId(),
                dto.getDestinationLocationId(),
                dto.getAlgorithm(),
                dto.getMaxAlternatives()
            );
            return ResponseEntity.ok(response);
        } else if (dto.getStartLat() != null && dto.getStartLon() != null &&
                   dto.getEndLat() != null && dto.getEndLon() != null) {
            RoutingService.RouteCalculationResponse response = routingService.calculateRouteCoordinates(
                dto.getStartLat(), dto.getStartLon(),
                dto.getEndLat(), dto.getEndLon(),
                dto.getAlgorithm(),
                dto.getMaxAlternatives()
            );
            return ResponseEntity.ok(response);
        } else {
            throw new IllegalArgumentException("Either originLocationId & destinationLocationId, or coordinates must be provided.");
        }
    }

    /**
     * Returns the full road network graph for Leaflet map overlay.
     */
    @GetMapping("/network")
    public ResponseEntity<Map<String, Object>> getRoadNetwork() {
        return ResponseEntity.ok(routingService.getRoadNetworkGraph());
    }

    /**
     * Dynamically updates the status of a road edge (OPEN, FLOODED, BLOCKED, UNCERTAIN).
     */
    @PutMapping("/edges/{id}/status")
    public ResponseEntity<RoadEdge> updateEdgeStatus(
        @PathVariable Long id,
        @RequestBody UpdateEdgeStatusDto dto
    ) {
        RoadEdge updated = routingService.updateEdgeStatus(id, dto.getStatus());
        return ResponseEntity.ok(updated);
    }

    /**
     * Persists a route calculation request for dispatcher review and audit.
     */
    @PostMapping("/requests")
    public ResponseEntity<RouteRequest> saveRouteRequest(@RequestBody SaveRequestDto dto) {
        RouteRequest req = routingService.saveRouteRequest(
            dto.getOriginLocationId(),
            dto.getDestinationLocationId(),
            dto.getRouteSummaryJson(),
            dto.getAlternativeCount()
        );
        return ResponseEntity.ok(req);
    }

    /**
     * Dispatcher approves a suggested route.
     */
    @PutMapping("/requests/{id}/approve")
    public ResponseEntity<RouteRequest> approveRouteRequest(
        @PathVariable Long id,
        @RequestBody(required = false) ReviewRouteDto dto
    ) {
        String notes = (dto != null) ? dto.getNotes() : "Approved by dispatcher";
        RouteRequest req = routingService.reviewRouteRequest(id, "APPROVED", notes);
        return ResponseEntity.ok(req);
    }

    /**
     * Dispatcher rejects a suggested route.
     */
    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<RouteRequest> rejectRouteRequest(
        @PathVariable Long id,
        @RequestBody(required = false) ReviewRouteDto dto
    ) {
        String notes = (dto != null) ? dto.getNotes() : "Rejected by dispatcher";
        RouteRequest req = routingService.reviewRouteRequest(id, "REJECTED", notes);
        return ResponseEntity.ok(req);
    }

    /**
     * Gets all route audit records.
     */
    @GetMapping("/requests")
    public ResponseEntity<List<RouteRequest>> getAllRequests() {
        return ResponseEntity.ok(routingService.getAllRouteRequests());
    }
}
