package com.resqroute.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Represents a named location (depot or relief camp) in the ResQ-Route system.
 *
 * <p>Locations are stored with their WGS-84 coordinates and a human-readable name.
 * They are the source and destination inputs for route planning requests.
 */
@Entity
@Table(name = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable name (e.g., "Gopeshwar Relief Depot"). */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /** One of: DEPOT, RELIEF_CAMP, HOSPITAL, CHECKPOINT, OTHER. */
    @NotBlank
    @Column(nullable = false)
    private String type;

    /** WGS-84 latitude. */
    @NotNull
    @Column(nullable = false)
    private Double latitude;

    /** WGS-84 longitude. */
    @NotNull
    @Column(nullable = false)
    private Double longitude;

    /** Optional human-readable address or district. */
    private String address;

    /** Optional notes (capacity, contact, etc.). */
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
