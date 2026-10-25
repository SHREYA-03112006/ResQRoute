package com.resqroute.service.matching;

import com.resqroute.dto.RoadMatchResult;
import com.resqroute.model.GraphEdge;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import com.resqroute.service.graph.DecoupledRoadGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Matches normalized disaster reports against road graph edges using
 * highway code matching, fuzzy string similarity, and spatial proximity.
 */
@Service
public class RoadMatchingService {

    private static final Logger log = LoggerFactory.getLogger(RoadMatchingService.class);

    private final DecoupledRoadGraphService graphService;

    public RoadMatchingService(DecoupledRoadGraphService graphService) {
        this.graphService = graphService;
    }

    public RoadMatchResult match(RoadReport report, List<RoadReport> existingActiveReports) {
        List<GraphEdge> candidateEdges = graphService.getAllEdges();
        if (candidateEdges.isEmpty()) {
            return RoadMatchResult.unmatched("Road graph has no edges loaded.");
        }

        GraphEdge bestEdge = null;
        double highestScore = -1.0;
        String bestReason = "";

        for (GraphEdge edge : candidateEdges) {
            double score = computeMatchScore(report, edge);
            if (score > highestScore) {
                highestScore = score;
                bestEdge = edge;
                bestReason = String.format("Score: %.2f (code=%s, road=%s)", score, edge.getRoadCode(), edge.getRoadName());
            }
        }

        if (bestEdge == null || highestScore < 0.35) {
            log.info("No matching edge found for report on '{}' (best score: {:.2f})",
                    report.getReportedRoadName(), highestScore);
            return RoadMatchResult.unmatched("Low similarity to known road segments (score=" + Math.max(0, highestScore) + ")");
        }

        // Check for conflicts with existing reports on the same edge
        boolean hasConflict = checkConflict(bestEdge.getId(), report.getReportedStatus(), existingActiveReports);
        String matchStatus;
        if (hasConflict) {
            matchStatus = "CONFLICT";
            bestReason += " [CONFLICT: Contradicts recent report on same segment]";
            log.warn("Conflict detected on edge {} ({}) for report {}", bestEdge.getId(), bestEdge.getRoadName(), report.getReportedRoadName());
        } else if (highestScore >= 0.65) {
            matchStatus = "MATCHED";
        } else {
            matchStatus = "WEAK_MATCH";
            bestReason += " [Weak match: spatial or name confidence below threshold, requires review]";
        }

        log.info("Matched report '{}' -> Edge {} ({}) [status={}, score={:.2f}]",
                report.getReportedRoadName(), bestEdge.getId(), bestEdge.getRoadName(), matchStatus, highestScore);

        return new RoadMatchResult(bestEdge.getId(), bestEdge.getRoadName(), matchStatus, highestScore, bestReason);
    }

    private double computeMatchScore(RoadReport report, GraphEdge edge) {
        double score = 0.0;

        String normReport = report.getNormalisedRoadName() != null ? report.getNormalisedRoadName().toUpperCase(Locale.ROOT) : "";
        String edgeCode = edge.getRoadCode() != null ? edge.getRoadCode().toUpperCase(Locale.ROOT) : "";
        String edgeName = edge.getRoadName() != null ? edge.getRoadName().toUpperCase(Locale.ROOT) : "";

        // 1. Direct Highway Code / NH identifier Match (weight: up to 0.45)
        if (!edgeCode.isEmpty() && normReport.contains(edgeCode)) {
            score += 0.45;
        }

        // 2. Name / Node text matching (weight: up to 0.25)
        String fromNode = edge.getFromNode().toUpperCase(Locale.ROOT);
        String toNode = edge.getToNode().toUpperCase(Locale.ROOT);
        String locDesc = report.getLocationDescription() != null ? report.getLocationDescription().toUpperCase(Locale.ROOT) : "";

        if (normReport.contains(fromNode) || locDesc.contains(fromNode)) score += 0.12;
        if (normReport.contains(toNode) || locDesc.contains(toNode)) score += 0.13;

        // String similarity between names
        double sim = jaroWinklerSimilarity(normReport, edgeName);
        score += sim * 0.15;

        // 3. Spatial Proximity using Haversine (weight: up to 0.25)
        if (report.getLatitude() != null && report.getLongitude() != null) {
            double distanceKm = haversineDistanceKm(
                    report.getLatitude(), report.getLongitude(),
                    edge.getMidLat(), edge.getMidLon()
            );

            if (distanceKm < 5.0) {
                score += 0.25;
            } else if (distanceKm < 15.0) {
                score += 0.18;
            } else if (distanceKm < 30.0) {
                score += 0.10;
            } else if (distanceKm > 75.0) {
                score -= 0.30; // Heavy penalty if geographically distant
            }
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    private boolean checkConflict(Long edgeId, RoadStatus newStatus, List<RoadReport> existingReports) {
        if (existingReports == null || existingReports.isEmpty()) return false;

        for (RoadReport r : existingReports) {
            if (edgeId.equals(r.getMatchedEdgeId())) {
                RoadStatus existingStatus = r.getReportedStatus();
                // A conflict exists if one says OPEN and another says BLOCKED / FLOODED
                if (existingStatus == RoadStatus.OPEN && (newStatus == RoadStatus.BLOCKED || newStatus == RoadStatus.FLOODED)) {
                    return true;
                }
                if ((existingStatus == RoadStatus.BLOCKED || existingStatus == RoadStatus.FLOODED) && newStatus == RoadStatus.OPEN) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Haversine formula for distance between two points in km */
    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** Simple Jaro string similarity */
    private double jaroWinklerSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length(), len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }
        if (matches == 0) return 0.0;

        double t = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) t += 0.5;
            k++;
        }

        return (((double) matches / len1) + ((double) matches / len2) + (((double) matches - t) / matches)) / 3.0;
    }
}
