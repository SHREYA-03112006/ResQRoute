package com.resqroute.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Records a dispatcher's route-planning request along with its result and approval status.
 */
@Entity
@Table(name = "route_requests")
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

    @Column(columnDefinition = "TEXT")
    private String routeSummaryJson;

    private Integer alternativeCount;

    private String approvalStatus = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String dispatcherNotes;

    private Instant reviewedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public RouteRequest() {}

    public RouteRequest(Long id, Location origin, Location destination, String routeSummaryJson,
                        Integer alternativeCount, String approvalStatus, String dispatcherNotes,
                        Instant reviewedAt, Instant createdAt) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.routeSummaryJson = routeSummaryJson;
        this.alternativeCount = alternativeCount;
        this.approvalStatus = approvalStatus != null ? approvalStatus : "PENDING";
        this.dispatcherNotes = dispatcherNotes;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Location origin;
        private Location destination;
        private String routeSummaryJson;
        private Integer alternativeCount;
        private String approvalStatus = "PENDING";
        private String dispatcherNotes;
        private Instant reviewedAt;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder origin(Location origin) { this.origin = origin; return this; }
        public Builder destination(Location destination) { this.destination = destination; return this; }
        public Builder routeSummaryJson(String routeSummaryJson) { this.routeSummaryJson = routeSummaryJson; return this; }
        public Builder alternativeCount(Integer alternativeCount) { this.alternativeCount = alternativeCount; return this; }
        public Builder approvalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; return this; }
        public Builder dispatcherNotes(String dispatcherNotes) { this.dispatcherNotes = dispatcherNotes; return this; }
        public Builder reviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public RouteRequest build() {
            return new RouteRequest(id, origin, destination, routeSummaryJson, alternativeCount, approvalStatus, dispatcherNotes, reviewedAt, createdAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Location getOrigin() { return origin; }
    public void setOrigin(Location origin) { this.origin = origin; }

    public Location getDestination() { return destination; }
    public void setDestination(Location destination) { this.destination = destination; }

    public String getRouteSummaryJson() { return routeSummaryJson; }
    public void setRouteSummaryJson(String routeSummaryJson) { this.routeSummaryJson = routeSummaryJson; }

    public Integer getAlternativeCount() { return alternativeCount; }
    public void setAlternativeCount(Integer alternativeCount) { this.alternativeCount = alternativeCount; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getDispatcherNotes() { return dispatcherNotes; }
    public void setDispatcherNotes(String dispatcherNotes) { this.dispatcherNotes = dispatcherNotes; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteRequest that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
