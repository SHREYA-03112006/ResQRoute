package com.resqroute.dto;

/**
 * Payload for a dispatcher reviewing, confirming, or overriding a report.
 */
public class ReviewReportRequest {

    private Boolean flaggedForReview;
    private String reviewNotes;
    private String statusOverride; // Optional manual override of road status
    private Long manualEdgeId;     // Optional manual edge assignment

    public ReviewReportRequest() {}

    public ReviewReportRequest(Boolean flaggedForReview, String reviewNotes, String statusOverride, Long manualEdgeId) {
        this.flaggedForReview = flaggedForReview;
        this.reviewNotes = reviewNotes;
        this.statusOverride = statusOverride;
        this.manualEdgeId = manualEdgeId;
    }

    public Boolean getFlaggedForReview() { return flaggedForReview; }
    public void setFlaggedForReview(Boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public String getStatusOverride() { return statusOverride; }
    public void setStatusOverride(String statusOverride) { this.statusOverride = statusOverride; }

    public Long getManualEdgeId() { return manualEdgeId; }
    public void setManualEdgeId(Long manualEdgeId) { this.manualEdgeId = manualEdgeId; }
}
