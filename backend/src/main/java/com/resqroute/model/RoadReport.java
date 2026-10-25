package com.resqroute.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * A structured disaster and road-condition report derived from online sources or the TinyFish API (Phase 2).
 */
@Entity
@Table(name = "road_reports")
public class RoadReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String incidentTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisasterCategory disasterCategory = DisasterCategory.OTHER;

    private String city;

    private String severity = "MODERATE";

    @NotBlank
    @Column(nullable = false)
    private String reportedRoadName;

    private String normalisedRoadName;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RoadStatus reportedStatus;

    private String locationDescription;

    private Double latitude;

    private Double longitude;

    private String sourceName;

    @Column(length = 2048)
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String rawSnippet;

    private Instant observedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant retrievedAt;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double confidenceScore = 0.0;

    private String matchStatus = "UNMATCHED";

    private Long matchedEdgeId;

    private String dataMode = "SAMPLE";

    private Boolean flaggedForReview = false;

    private String verificationStatus = "NEW";

    private String extractionStatus = "SUCCESS";

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    public RoadReport() {}

    public RoadReport(Long id, String reportedRoadName, String normalisedRoadName, RoadStatus reportedStatus,
                      String locationDescription, Double latitude, Double longitude, String sourceName,
                      String sourceUrl, Instant observedAt, Instant retrievedAt, Double confidenceScore,
                      String matchStatus, Long matchedEdgeId, String dataMode, Boolean flaggedForReview,
                      String reviewNotes) {
        this.id = id;
        this.reportedRoadName = reportedRoadName;
        this.normalisedRoadName = normalisedRoadName;
        this.reportedStatus = reportedStatus;
        this.locationDescription = locationDescription;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.observedAt = observedAt;
        this.retrievedAt = retrievedAt;
        this.confidenceScore = confidenceScore != null ? confidenceScore : 0.0;
        this.matchStatus = matchStatus != null ? matchStatus : "UNMATCHED";
        this.matchedEdgeId = matchedEdgeId;
        this.dataMode = dataMode != null ? dataMode : "SAMPLE";
        this.flaggedForReview = flaggedForReview != null ? flaggedForReview : false;
        this.reviewNotes = reviewNotes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String incidentTitle;
        private DisasterCategory disasterCategory = DisasterCategory.OTHER;
        private String city;
        private String severity = "MODERATE";
        private String reportedRoadName;
        private String normalisedRoadName;
        private RoadStatus reportedStatus;
        private String locationDescription;
        private Double latitude;
        private Double longitude;
        private String sourceName;
        private String sourceUrl;
        private String rawSnippet;
        private Instant observedAt;
        private Instant retrievedAt;
        private Double confidenceScore = 0.0;
        private String matchStatus = "UNMATCHED";
        private Long matchedEdgeId;
        private String dataMode = "SAMPLE";
        private Boolean flaggedForReview = false;
        private String verificationStatus = "NEW";
        private String extractionStatus = "SUCCESS";
        private String reviewNotes;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder incidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; return this; }
        public Builder disasterCategory(DisasterCategory disasterCategory) { this.disasterCategory = disasterCategory; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder reportedRoadName(String reportedRoadName) { this.reportedRoadName = reportedRoadName; return this; }
        public Builder normalisedRoadName(String normalisedRoadName) { this.normalisedRoadName = normalisedRoadName; return this; }
        public Builder reportedStatus(RoadStatus reportedStatus) { this.reportedStatus = reportedStatus; return this; }
        public Builder locationDescription(String locationDescription) { this.locationDescription = locationDescription; return this; }
        public Builder latitude(Double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { this.longitude = longitude; return this; }
        public Builder sourceName(String sourceName) { this.sourceName = sourceName; return this; }
        public Builder sourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; return this; }
        public Builder rawSnippet(String rawSnippet) { this.rawSnippet = rawSnippet; return this; }
        public Builder observedAt(Instant observedAt) { this.observedAt = observedAt; return this; }
        public Builder retrievedAt(Instant retrievedAt) { this.retrievedAt = retrievedAt; return this; }
        public Builder confidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public Builder matchStatus(String matchStatus) { this.matchStatus = matchStatus; return this; }
        public Builder matchedEdgeId(Long matchedEdgeId) { this.matchedEdgeId = matchedEdgeId; return this; }
        public Builder dataMode(String dataMode) { this.dataMode = dataMode; return this; }
        public Builder flaggedForReview(Boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; return this; }
        public Builder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public Builder extractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; return this; }
        public Builder reviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; return this; }

        public RoadReport build() {
            RoadReport r = new RoadReport(id, reportedRoadName, normalisedRoadName, reportedStatus,
                    locationDescription, latitude, longitude, sourceName, sourceUrl, observedAt,
                    retrievedAt, confidenceScore, matchStatus, matchedEdgeId, dataMode,
                    flaggedForReview, reviewNotes);
            r.setIncidentTitle(incidentTitle != null ? incidentTitle : reportedRoadName);
            r.setDisasterCategory(disasterCategory != null ? disasterCategory : DisasterCategory.OTHER);
            r.setCity(city);
            r.setSeverity(severity != null ? severity : "MODERATE");
            r.setRawSnippet(rawSnippet);
            r.setVerificationStatus(verificationStatus != null ? verificationStatus : "NEW");
            r.setExtractionStatus(extractionStatus != null ? extractionStatus : "SUCCESS");
            return r;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIncidentTitle() { return incidentTitle != null ? incidentTitle : reportedRoadName; }
    public void setIncidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; }

    public DisasterCategory getDisasterCategory() { return disasterCategory != null ? disasterCategory : DisasterCategory.OTHER; }
    public void setDisasterCategory(DisasterCategory disasterCategory) { this.disasterCategory = disasterCategory; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getSeverity() { return severity != null ? severity : "MODERATE"; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getReportedRoadName() { return reportedRoadName; }
    public void setReportedRoadName(String reportedRoadName) { this.reportedRoadName = reportedRoadName; }

    public String getNormalisedRoadName() { return normalisedRoadName; }
    public void setNormalisedRoadName(String normalisedRoadName) { this.normalisedRoadName = normalisedRoadName; }

    public RoadStatus getReportedStatus() { return reportedStatus; }
    public void setReportedStatus(RoadStatus reportedStatus) { this.reportedStatus = reportedStatus; }

    public String getLocationDescription() { return locationDescription; }
    public void setLocationDescription(String locationDescription) { this.locationDescription = locationDescription; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getRawSnippet() { return rawSnippet; }
    public void setRawSnippet(String rawSnippet) { this.rawSnippet = rawSnippet; }

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }

    public Instant getRetrievedAt() { return retrievedAt; }
    public void setRetrievedAt(Instant retrievedAt) { this.retrievedAt = retrievedAt; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public Long getMatchedEdgeId() { return matchedEdgeId; }
    public void setMatchedEdgeId(Long matchedEdgeId) { this.matchedEdgeId = matchedEdgeId; }

    public String getDataMode() { return dataMode; }
    public void setDataMode(String dataMode) { this.dataMode = dataMode; }

    public Boolean getFlaggedForReview() { return flaggedForReview; }
    public void setFlaggedForReview(Boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; }

    public String getVerificationStatus() { return verificationStatus != null ? verificationStatus : "NEW"; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getExtractionStatus() { return extractionStatus != null ? extractionStatus : "SUCCESS"; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoadReport that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
