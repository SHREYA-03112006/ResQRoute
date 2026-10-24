package com.resqroute.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A node in the road network graph, representing a junction or waypoint.
 *
 * <p>Each node has WGS-84 coordinates and an optional name (intersection name,
 * town name, or OSM node reference). Nodes are connected by {@link RoadEdge}s.
 */
@Entity
@Table(name = "road_nodes", indexes = {
    @Index(name = "idx_road_nodes_coords", columnList = "latitude, longitude")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoadNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** WGS-84 latitude of this graph node. */
    @Column(nullable = false)
    private Double latitude;

    /** WGS-84 longitude of this graph node. */
    @Column(nullable = false)
    private Double longitude;

    /** Optional name (town, junction, or OSM reference). */
    private String name;

    /** Optional OSM node ID for traceability. */
    private Long osmNodeId;
}
