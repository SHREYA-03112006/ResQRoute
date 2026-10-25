package com.resqroute.service.confidence;

import com.resqroute.dto.ConfidenceScoreBreakdown;
import com.resqroute.dto.RoadMatchResult;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Calculates transparent, configurable confidence scores for disaster reports based on:
 * source authority, temporal recency decay, multi-source agreement, and road-matching quality.
 */
@Service
public class ConfidenceScoringService {

    private static final Logger log = LoggerFactory.getLogger(ConfidenceScoringService.class);

    @Value("${resqroute.routing.confidence-threshold-accept:0.70}")
    private double acceptThreshold;

    @Value("${resqroute.routing.confidence-threshold-review:0.40}")
    private double reviewThreshold;

    @Value("${resqroute.routing.report-stale-hours:6}")
    private double staleHours;

    public ConfidenceScoreBreakdown computeScore(RoadReport report, RoadMatchResult matchResult, List<RoadReport> otherReports) {
        // 1. Source Reliability Score (0.0 to 1.0)
        double sourceScore = evaluateSourceReliability(report.getSourceName());

        // 2. Recency Decay Score (0.0 to 1.0)
        double recencyScore = computeRecencyScore(report.getObservedAt());

        // 3. Multi-source Agreement Score (0.0 to 1.0)
        double agreementScore = computeAgreementScore(report, matchResult.getMatchedEdgeId(), otherReports);

        // 4. Road Match Quality (0.0 to 1.0)
        double matchScore = matchResult.getMatchScore();

        // Weighted Composite Score
        // 35% Source + 25% Recency + 20% Agreement + 20% Match Quality
        double composite = (0.35 * sourceScore)
                         + (0.25 * recencyScore)
                         + (0.20 * agreementScore)
                         + (0.20 * matchScore);

        double finalScore = Math.max(0.0, Math.min(1.0, composite));

        // Determine if flagged for dispatcher review
        boolean flagged = false;
        StringBuilder reason = new StringBuilder();

        if ("CONFLICT".equals(matchResult.getMatchStatus())) {
            flagged = true;
            reason.append("Conflicting reports on same road segment. ");
        }

        if ("WEAK_MATCH".equals(matchResult.getMatchStatus())) {
            flagged = true;
            reason.append("Weak road match requires human verification. ");
        }

        if (report.getReportedStatus() == RoadStatus.UNCERTAIN) {
            flagged = true;
            reason.append("Report status is uncertain/unconfirmed. ");
        }

        if (finalScore >= reviewThreshold && finalScore < acceptThreshold) {
            flagged = true;
            reason.append(String.format(Locale.ROOT, "Confidence (%.2f) requires dispatcher review (threshold: %.2f). ", finalScore, acceptThreshold));
        }

        if (recencyScore < 0.25) {
            reason.append("Report is stale (observed > 12 hours ago). ");
        }

        log.info("Confidence computed for report '{}': final={:.2f} (source={:.2f}, recency={:.2f}, agree={:.2f}, match={:.2f}, flagged={})",
                report.getReportedRoadName(), finalScore, sourceScore, recencyScore, agreementScore, matchScore, flagged);

        return new ConfidenceScoreBreakdown(
                finalScore, sourceScore, recencyScore, agreementScore, matchScore,
                flagged, reason.toString().trim()
        );
    }

    public double evaluateSourceReliability(String sourceName) {
        if (sourceName == null) return 0.40;
        String s = sourceName.toLowerCase(Locale.ROOT);

        // State / National Disaster authorities & military engineering
        if (s.contains("disaster management") || s.contains("usdma") || s.contains("ndrf")
                || s.contains("sdrf") || s.contains("bro") || s.contains("border roads")
                || s.contains("police") || s.contains("district magistrate") || s.contains("pwd")) {
            return 1.00;
        }

        // Meteorological & Hydrological offices
        if (s.contains("imd") || s.contains("meteorological") || s.contains("cwc")
                || s.contains("water commission") || s.contains("weather")) {
            return 0.95;
        }

        // Established news publications
        if (s.contains("times") || s.contains("express") || s.contains("tribune")
                || s.contains("hindu") || s.contains("ani") || s.contains("pti") || s.contains("bbc")) {
            return 0.75;
        }

        // Social media or user crowdsourced
        if (s.contains("twitter") || s.contains("x.com") || s.contains("facebook")
                || s.contains("social") || s.contains("crowdsource") || s.contains("citizen")) {
            return 0.35;
        }

        // Default local news / blog
        return 0.55;
    }

    public double computeRecencyScore(Instant observedAt) {
        if (observedAt == null) return 0.50;

        Duration age = Duration.between(observedAt, Instant.now());
        double ageHours = Math.max(0.0, age.toMinutes() / 60.0);

        // Exponential half-life decay formula: score = 2^(-(age / staleHours))
        double decayFactor = Math.pow(2.0, -(ageHours / staleHours));
        return Math.max(0.05, Math.min(1.0, decayFactor));
    }

    private double computeAgreementScore(RoadReport report, Long matchedEdgeId, List<RoadReport> otherReports) {
        if (matchedEdgeId == null || otherReports == null || otherReports.isEmpty()) {
            return 0.50; // Neutral baseline when no other reports exist
        }

        int agreeingCount = 0;
        int conflictingCount = 0;

        for (RoadReport r : otherReports) {
            if (report.getId() != null && report.getId().equals(r.getId())) continue;

            if (matchedEdgeId.equals(r.getMatchedEdgeId())) {
                if (report.getReportedStatus() == r.getReportedStatus()) {
                    agreeingCount++;
                } else if ((report.getReportedStatus() == RoadStatus.OPEN && r.getReportedStatus() == RoadStatus.BLOCKED)
                        || (report.getReportedStatus() == RoadStatus.BLOCKED && r.getReportedStatus() == RoadStatus.OPEN)) {
                    conflictingCount++;
                }
            }
        }

        if (conflictingCount > 0) {
            return 0.15; // Low score due to contradiction
        }

        if (agreeingCount > 0) {
            return Math.min(1.0, 0.50 + (agreeingCount * 0.20));
        }

        return 0.50;
    }

    public double getAcceptThreshold() { return acceptThreshold; }
    public double getReviewThreshold() { return reviewThreshold; }
    public double getStaleHours() { return staleHours; }
}
