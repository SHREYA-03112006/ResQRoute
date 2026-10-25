package com.resqroute.dto;

/**
 * Encapsulates the transparent, auditable breakdown of a report's confidence score.
 */
public class ConfidenceScoreBreakdown {

    private double finalScore;
    private double sourceReliabilityScore;
    private double recencyScore;
    private double agreementScore;
    private double roadMatchScore;
    private boolean flaggedForReview;
    private String flagReason;

    public ConfidenceScoreBreakdown() {}

    public ConfidenceScoreBreakdown(double finalScore, double sourceReliabilityScore, double recencyScore,
                                    double agreementScore, double roadMatchScore, boolean flaggedForReview,
                                    String flagReason) {
        this.finalScore = finalScore;
        this.sourceReliabilityScore = sourceReliabilityScore;
        this.recencyScore = recencyScore;
        this.agreementScore = agreementScore;
        this.roadMatchScore = roadMatchScore;
        this.flaggedForReview = flaggedForReview;
        this.flagReason = flagReason;
    }

    public double getFinalScore() { return finalScore; }
    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

    public double getSourceReliabilityScore() { return sourceReliabilityScore; }
    public void setSourceReliabilityScore(double sourceReliabilityScore) { this.sourceReliabilityScore = sourceReliabilityScore; }

    public double getRecencyScore() { return recencyScore; }
    public void setRecencyScore(double recencyScore) { this.recencyScore = recencyScore; }

    public double getAgreementScore() { return agreementScore; }
    public void setAgreementScore(double agreementScore) { this.agreementScore = agreementScore; }

    public double getRoadMatchScore() { return roadMatchScore; }
    public void setRoadMatchScore(double roadMatchScore) { this.roadMatchScore = roadMatchScore; }

    public boolean isFlaggedForReview() { return flaggedForReview; }
    public void setFlaggedForReview(boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }
}
