package com.resqroute.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Records a dispatcher's route-planning request along with its result and approval status.
 *
 * <p>A RouteRequest captures the origin, destination, the recommended route reference,
 * and whether the dispatcher has approved it. It is persisted for audit purposes.
 *
 * <p>Approval here means the dispatcher has reviewed and accepted the suggested route
 * in the dashboard. It does NOT trigger any real-world vehicle dispatch.
 */
@Entity
@Table(name = "route_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "origin_id", nullable = false)
    @NotNull
    private Location origin;

    @ManyToOne
    @JoinColumn(name = "destination_id", nullable = false)
    @NotNull
    private Location destination;

    /** Serialised route summary (JSON). Full route stored in Redis. */
    @Column(columnDefinition = "TEXT")
    private String routeSummaryJson;

    /** Number of alternative routes found. */
    private Integer alternativeCount;

    /**
     * Approval status.
     * PENDING — awaiting dispatcher review.
     * APPROVED — dispatcher has reviewed and accepted the route.
     * REJECTED — dispatcher has rejected the suggestion.
     */
    @Builder.Default
    private String approvalStatus = "PENDING";

    /** Dispatcher's optional notes at approval time. */
    @Column(columnDefinition = "TEXT")
    private String dispatcherNotes;

    /** Timestamp when the dispatcher acted on the request. */
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
