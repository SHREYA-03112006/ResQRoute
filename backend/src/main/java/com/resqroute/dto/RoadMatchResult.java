package com.resqroute.dto;

/**
 * Result of attempting to match a disaster report to a road segment / graph edge.
 */
public class RoadMatchResult {

    private Long matchedEdgeId;
    private String matchedRoadName;
    private String matchStatus; // MATCHED, WEAK_MATCH, UNMATCHED, CONFLICT
    private double matchScore;  // 0.0 to 1.0
    private String matchDetails;

    public RoadMatchResult() {}

    public RoadMatchResult(Long matchedEdgeId, String matchedRoadName, String matchStatus, double matchScore, String matchDetails) {
        this.matchedEdgeId = matchedEdgeId;
        this.matchedRoadName = matchedRoadName;
        this.matchStatus = matchStatus;
        this.matchScore = matchScore;
        this.matchDetails = matchDetails;
    }

    public static RoadMatchResult unmatched(String reason) {
        return new RoadMatchResult(null, null, "UNMATCHED", 0.0, reason);
    }

    public Long getMatchedEdgeId() { return matchedEdgeId; }
    public void setMatchedEdgeId(Long matchedEdgeId) { this.matchedEdgeId = matchedEdgeId; }

    public String getMatchedRoadName() { return matchedRoadName; }
    public void setMatchedRoadName(String matchedRoadName) { this.matchedRoadName = matchedRoadName; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }

    public String getMatchDetails() { return matchDetails; }
    public void setMatchDetails(String matchDetails) { this.matchDetails = matchDetails; }
}
