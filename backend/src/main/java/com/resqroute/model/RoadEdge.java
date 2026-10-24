package com.resqroute.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A directed edge in the road network graph, representing a road segment
 * between two {@link RoadNode}s.
 *
 * <p>Each edge stores:
 * <ul>
 *   <li>Road name and optional highway identifier (NH/SH)</li>
 *   <li>Distance in kilometres (from OSM or calculated from coordinates)</li>
 *   <li>Base travel time in minutes (derived from road type speed assumptions)</li>
 *   <li>Current road status (OPEN, FLOODED, BLOCKED, UNCERTAIN)</li>
 * </ul>
 *
 * <p>Cost model:
 * <ul>
 *   <li>OPEN — base travel time used as cost</li>
 *   <li>FLOODED — cost multiplied by configurable penalty (default 5x)</li>
 *   <li>BLOCKED — edge excluded from routing (infinite cost)</li>
 *   <li>UNCERTAIN — cost multiplied by a conservative penalty (default 3x)</li>
 * </ul>
 */
@Entity
@Table(name = "road_edges", indexes = {
    @Index(name = "idx_road_edges_from", columnList = "from_node_id"),
    @Index(name = "idx_road_edges_to", columnList = "to_node_id"),
    @Index(name = "idx_road_edges_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoadEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Source node of this directed edge. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id", nullable = false)
    private RoadNode fromNode;

    /** Target node of this directed edge. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id", nullable = false)
    private RoadNode toNode;

    /** Human-readable road name (e.g., "NH-7", "Badrinath Highway"). */
    private String roadName;

    /** Highway classification (NH, SH, MDR, LOCAL). */
    @Builder.Default
    private String roadType = "LOCAL";

    /** Distance in kilometres. */
    @Column(nullable = false)
    private Double distanceKm;

    /**
     * Base travel time in minutes under normal (OPEN) conditions.
     * Derived from distance and assumed speed for the road type.
     */
    @Column(nullable = false)
    private Double baseTravelTimeMinutes;

    /** Current status of this edge. Defaults to OPEN. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoadStatus status = RoadStatus.OPEN;

    /** Whether this edge represents a bidirectional road (most mountain roads are). */
    @Builder.Default
    private Boolean bidirectional = true;

    /** Optional OSM way ID for traceability. */
    private Long osmWayId;
}
