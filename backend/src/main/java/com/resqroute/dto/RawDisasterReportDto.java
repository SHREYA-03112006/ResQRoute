package com.resqroute.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Consistent JSON schema for raw disaster reports ingested into ResQ-Route (Phase 2).
 * Used for TinyFish agent output, direct webhook ingestion, and sample datasets.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawDisasterReportDto {

    private String incidentTitle;

    private String disasterCategory; // FIRE, FLOOD, LANDSLIDE, ROAD_BLOCKAGE, BUILDING_COLLAPSE, OTHER

    private String city;

    private String severity; // CRITICAL, HIGH, MODERATE, LOW

    @NotBlank(message = "reportedRoadName is required")
    private String reportedRoadName;

    @NotNull(message = "reportedStatus is required (OPEN, FLOODED, BLOCKED, UNCERTAIN)")
    private String reportedStatus;

    private String locationDescription;

    private Double latitude;

    private Double longitude;

    @NotBlank(message = "sourceName is required")
    private String sourceName;

    private String sourceUrl;

    /**
     * Source authority classification:
     * GOVERNMENT_AGENCY, MET_OFFICE, EMERGENCY_SERVICE, NEWS_MEDIA, SOCIAL_MEDIA, CROWDSOURCED
     */
    private String sourceType;

    /** Observed or published timestamp in UTC (ISO-8601). */
    private Instant observedAt;

    private String rawTextSnippet;

    private String extractionStatus;

    public RawDisasterReportDto() {}

    public RawDisasterReportDto(String reportedRoadName, String reportedStatus, String locationDescription,
                                Double latitude, Double longitude, String sourceName, String sourceUrl,
                                String sourceType, Instant observedAt, String rawTextSnippet) {
        this.reportedRoadName = reportedRoadName;
        this.reportedStatus = reportedStatus;
        this.locationDescription = locationDescription;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.sourceType = sourceType;
        this.observedAt = observedAt;
        this.rawTextSnippet = rawTextSnippet;
    }

    public RawDisasterReportDto(String incidentTitle, String disasterCategory, String city, String severity,
                                String reportedRoadName, String reportedStatus, String locationDescription,
                                Double latitude, Double longitude, String sourceName, String sourceUrl,
                                String sourceType, Instant observedAt, String rawTextSnippet) {
        this(reportedRoadName, reportedStatus, locationDescription, latitude, longitude, sourceName, sourceUrl, sourceType, observedAt, rawTextSnippet);
        this.incidentTitle = incidentTitle;
        this.disasterCategory = disasterCategory;
        this.city = city;
        this.severity = severity;
    }

    public String getIncidentTitle() { return incidentTitle != null ? incidentTitle : reportedRoadName; }
    public void setIncidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; }

    public String getDisasterCategory() { return disasterCategory; }
    public void setDisasterCategory(String disasterCategory) { this.disasterCategory = disasterCategory; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getReportedRoadName() { return reportedRoadName; }
    public void setReportedRoadName(String reportedRoadName) { this.reportedRoadName = reportedRoadName; }

    public String getReportedStatus() { return reportedStatus; }
    public void setReportedStatus(String reportedStatus) { this.reportedStatus = reportedStatus; }

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

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }

    public String getRawTextSnippet() { return rawTextSnippet; }
    public void setRawTextSnippet(String rawTextSnippet) { this.rawTextSnippet = rawTextSnippet; }

    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }
}
