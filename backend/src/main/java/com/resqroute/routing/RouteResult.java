package com.resqroute.routing;

import java.util.List;

/**
 * Represents a calculated route through the road network graph.
 *
 * <p>Contains the ordered list of node IDs, edge IDs, coordinates for map display,
 * total distance, estimated travel time, and cost assumptions used.
 */
public record RouteResult(
    /** Ordered list of node IDs along the route. */
    List<Long> nodeIds,

    /** Ordered list of edge IDs traversed. */
    List<Long> edgeIds,

    /** Ordered list of [lat, lon] waypoints for map polyline rendering. */
    List<double[]> waypoints,

    /** Ordered list of road names along the route. */
    List<String> roadSegments,

    /** Total route distance in kilometres. */
    double totalDistanceKm,

    /** Estimated travel time in minutes. */
    double estimatedTimeMinutes,

    /** Number of flooded edges on this route. */
    int floodedSegments,

    /** Number of uncertain-status edges on this route. */
    int uncertainSegments,

    /** Whether any blocked edges prevented the ideal route. */
    boolean hasBlockedDetour,

    /** The routing algorithm used (DIJKSTRA or A_STAR). */
    String algorithm,

    /** Human-readable description of cost assumptions. */
    String costAssumptions
) {}
