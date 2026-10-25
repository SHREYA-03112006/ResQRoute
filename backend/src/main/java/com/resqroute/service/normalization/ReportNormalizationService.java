package com.resqroute.service.normalization;

import com.resqroute.dto.RawDisasterReportDto;
import com.resqroute.model.DisasterCategory;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes road names, statuses, disaster categories, cities, timestamps,
 * and resolves missing coordinates using a multi-city gazetteer (Phase 2).
 */
@Service
public class ReportNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(ReportNormalizationService.class);

    // Multi-city gazetteer for Uttarakhand sectors (Town/Area name -> [Lat, Lon])
    private static final Map<String, double[]> GAZETTEER = new LinkedHashMap<>();

    static {
        // Dehradun Sector
        GAZETTEER.put("dehradun",       new double[]{30.3165, 78.0322});
        GAZETTEER.put("rispana",        new double[]{30.3120, 78.0550});
        GAZETTEER.put("rajpur",         new double[]{30.3850, 78.0820});
        GAZETTEER.put("sahastradhara",  new double[]{30.3620, 78.0780});
        GAZETTEER.put("bindal",         new double[]{30.3270, 78.0280});
        GAZETTEER.put("isbt",           new double[]{30.2875, 78.0035});
        GAZETTEER.put("chakrata",       new double[]{30.3340, 77.9950});
        GAZETTEER.put("clock tower",    new double[]{30.3245, 78.0415});
        GAZETTEER.put("transport nagar",new double[]{30.2910, 78.0050});
        GAZETTEER.put("paltan bazaar",  new double[]{30.3220, 78.0380});

        // Rishikesh Sector
        GAZETTEER.put("rishikesh",      new double[]{30.0869, 78.2676});
        GAZETTEER.put("triveni",        new double[]{30.1040, 78.2980});
        GAZETTEER.put("muni ki reti",   new double[]{30.1180, 78.3120});
        GAZETTEER.put("tapovan",        new double[]{30.1320, 78.3260});
        GAZETTEER.put("barrage",        new double[]{30.0650, 78.2880});
        GAZETTEER.put("pashulok",       new double[]{30.0650, 78.2880});

        // Haridwar Sector
        GAZETTEER.put("haridwar",       new double[]{29.9457, 78.1642});
        GAZETTEER.put("har ki pauri",   new double[]{29.9560, 78.1700});
        GAZETTEER.put("chandi ghat",    new double[]{29.9410, 78.1750});
        GAZETTEER.put("laksar",         new double[]{29.8950, 78.1420});
        GAZETTEER.put("roorkee",        new double[]{29.8540, 77.8880});
        GAZETTEER.put("bhel",           new double[]{29.9320, 78.1180});

        // Nainital Sector
        GAZETTEER.put("nainital",       new double[]{29.3919, 79.4542});
        GAZETTEER.put("mall road",      new double[]{29.3880, 79.4600});
        GAZETTEER.put("tallital",       new double[]{29.3820, 79.4640});
        GAZETTEER.put("mallital",       new double[]{29.3940, 79.4580});
        GAZETTEER.put("bhowali",        new double[]{29.3950, 79.4920});

        // Rudraprayag & Chamoli Sectors
        GAZETTEER.put("rudraprayag",    new double[]{30.285, 78.981});
        GAZETTEER.put("tilwara",        new double[]{30.292, 78.962});
        GAZETTEER.put("agastmuni",      new double[]{30.395, 79.031});
        GAZETTEER.put("kund",           new double[]{30.501, 79.112});
        GAZETTEER.put("guptkashi",      new double[]{30.523, 79.082});
        GAZETTEER.put("gaurikund",      new double[]{30.645, 79.266});
        GAZETTEER.put("karnaprayag",    new double[]{30.258, 79.219});
        GAZETTEER.put("nandprayag",     new double[]{30.331, 79.324});
        GAZETTEER.put("chamoli",        new double[]{30.396, 79.321});
        GAZETTEER.put("gopeshwar",      new double[]{30.396, 79.321});
        GAZETTEER.put("pipalkoti",      new double[]{30.473, 79.479});
        GAZETTEER.put("joshimath",      new double[]{30.556, 79.567});
        GAZETTEER.put("chopta",         new double[]{30.485, 79.182});
        GAZETTEER.put("mandal",         new double[]{30.461, 79.278});
    }

    private static final Pattern NH_PATTERN = Pattern.compile("(?i)(?:NH|NATIONAL\\s*HIGHWAY)[\\s-]*(\\d+)");
    private static final Pattern SH_PATTERN = Pattern.compile("(?i)(?:SH|STATE\\s*HIGHWAY)[\\s-]*(\\d+)");

    public RoadReport normalize(RawDisasterReportDto raw) {
        String cleanName = normalizeRoadName(raw.getReportedRoadName());
        RoadStatus status = normalizeStatus(raw.getReportedStatus());
        DisasterCategory category = normalizeDisasterCategory(raw.getDisasterCategory(), raw.getRawTextSnippet(), raw.getIncidentTitle());
        String city = detectCity(raw.getCity(), raw.getReportedRoadName(), raw.getLocationDescription());
        String severity = raw.getSeverity() != null ? raw.getSeverity().toUpperCase(Locale.ROOT)
                : estimateSeverity(status, category, raw.getRawTextSnippet());

        Double lat = raw.getLatitude();
        Double lon = raw.getLongitude();

        // Gazetteer fallback if coordinates are missing
        if (lat == null || lon == null) {
            double[] resolvedCoords = lookupGazetteer(raw.getReportedRoadName(), raw.getLocationDescription());
            if (resolvedCoords != null) {
                lat = resolvedCoords[0];
                lon = resolvedCoords[1];
                log.info("Resolved coordinates from gazetteer for '{}': lat={}, lon={}",
                        raw.getReportedRoadName(), lat, lon);
            }
        }

        Instant observedAt = raw.getObservedAt() != null ? raw.getObservedAt() : Instant.now();
        String title = raw.getIncidentTitle() != null ? raw.getIncidentTitle()
                : (category + " - " + cleanName);

        return RoadReport.builder()
                .incidentTitle(title)
                .disasterCategory(category)
                .city(city)
                .severity(severity)
                .reportedRoadName(raw.getReportedRoadName())
                .normalisedRoadName(cleanName)
                .reportedStatus(status)
                .locationDescription(raw.getLocationDescription())
                .latitude(lat)
                .longitude(lon)
                .sourceName(raw.getSourceName())
                .sourceUrl(raw.getSourceUrl())
                .rawSnippet(raw.getRawTextSnippet())
                .observedAt(observedAt)
                .dataMode("SAMPLE") // default mode, overridden by pipeline
                .matchStatus("UNMATCHED")
                .confidenceScore(0.0)
                .flaggedForReview(status == RoadStatus.UNCERTAIN)
                .verificationStatus("NEW")
                .extractionStatus(raw.getExtractionStatus() != null ? raw.getExtractionStatus() : "SUCCESS")
                .build();
    }

    public DisasterCategory normalizeDisasterCategory(String rawCategory, String text, String title) {
        if (rawCategory != null && !rawCategory.trim().isEmpty()) {
            try {
                return DisasterCategory.valueOf(rawCategory.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        String combined = ((text != null ? text : "") + " " + (title != null ? title : "")).toLowerCase(Locale.ROOT);

        if (combined.contains("fire") || combined.contains("blaze") || combined.contains("flame")
                || combined.contains("burn") || combined.contains("combustion")) {
            return DisasterCategory.FIRE;
        }

        if (combined.contains("flood") || combined.contains("inundat") || combined.contains("submerg")
                || combined.contains("waterlog") || combined.contains("overflow") || combined.contains("river surge")
                || combined.contains("deluge") || combined.contains("water level")) {
            return DisasterCategory.FLOOD;
        }

        if (combined.contains("landslide") || combined.contains("mudslide") || combined.contains("rockfall")
                || combined.contains("debris") || combined.contains("hill cut") || combined.contains("slope failure")
                || combined.contains("subsidence") || combined.contains("moraine") || combined.contains("scree")) {
            return DisasterCategory.LANDSLIDE;
        }

        if (combined.contains("collapse") || combined.contains("cave-in") || combined.contains("structural")
                || combined.contains("bridge pier") || combined.contains("building damage") || combined.contains("cracks")) {
            return DisasterCategory.BUILDING_COLLAPSE;
        }

        if (combined.contains("block") || combined.contains("clos") || combined.contains("tree fall")
                || combined.contains("barricad") || combined.contains("jam") || combined.contains("divert")
                || combined.contains("impassable") || combined.contains("cut off")) {
            return DisasterCategory.ROAD_BLOCKAGE;
        }

        return DisasterCategory.OTHER;
    }

    public String detectCity(String rawCity, String roadName, String locationDesc) {
        if (rawCity != null && !rawCity.trim().isEmpty()) {
            return rawCity.trim().toLowerCase(Locale.ROOT);
        }

        String combined = ((roadName != null ? roadName : "") + " "
                + (locationDesc != null ? locationDesc : "")).toLowerCase(Locale.ROOT);

        if (combined.contains("dehradun") || combined.contains("rispana") || combined.contains("rajpur")
                || combined.contains("sahastradhara") || combined.contains("bindal") || combined.contains("isbt")
                || combined.contains("chakrata")) {
            return "dehradun";
        }
        if (combined.contains("rishikesh") || combined.contains("triveni") || combined.contains("muni ki reti")
                || combined.contains("tapovan") || combined.contains("pashulok")) {
            return "rishikesh";
        }
        if (combined.contains("haridwar") || combined.contains("laksar") || combined.contains("chandi ghat")
                || combined.contains("roorkee") || combined.contains("bhel")) {
            return "haridwar";
        }
        if (combined.contains("nainital") || combined.contains("tallital") || combined.contains("mallital")
                || combined.contains("bhowali")) {
            return "nainital";
        }
        if (combined.contains("chamoli") || combined.contains("joshimath") || combined.contains("pipalkoti")
                || combined.contains("gopeshwar") || combined.contains("helang")) {
            return "chamoli";
        }
        if (combined.contains("rudraprayag") || combined.contains("tilwara") || combined.contains("agastmuni")
                || combined.contains("guptkashi") || combined.contains("sirobagarh")) {
            return "rudraprayag";
        }

        return "dehradun"; // default sector
    }

    public String estimateSeverity(RoadStatus status, DisasterCategory category, String text) {
        String t = (text != null ? text : "").toLowerCase(Locale.ROOT);

        if (status == RoadStatus.BLOCKED || category == DisasterCategory.BUILDING_COLLAPSE
                || t.contains("fatal") || t.contains("cloudburst") || t.contains("danger mark")
                || t.contains("completely impassable") || t.contains("submerged")) {
            return "CRITICAL";
        }
        if (status == RoadStatus.FLOODED || category == DisasterCategory.FIRE || category == DisasterCategory.LANDSLIDE
                || t.contains("waterlogged") || t.contains("divert") || t.contains("single lane")) {
            return "HIGH";
        }
        if (status == RoadStatus.UNCERTAIN || t.contains("unconfirmed") || t.contains("rumour") || t.contains("pending")) {
            return "MODERATE";
        }
        if (status == RoadStatus.OPEN || t.contains("restored") || t.contains("clear")) {
            return "LOW";
        }
        return "MODERATE";
    }

    public String normalizeRoadName(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "UNKNOWN ROAD";

        String text = raw.trim();

        // Check for National Highway identifier
        Matcher nhMatcher = NH_PATTERN.matcher(text);
        if (nhMatcher.find()) {
            String nhNum = nhMatcher.group(1);
            if ("58".equals(nhNum)) {
                return "NH-7 (OLD NH-58)";
            }
            return "NH-" + nhNum;
        }

        // Check for State Highway identifier
        Matcher shMatcher = SH_PATTERN.matcher(text);
        if (shMatcher.find()) {
            return "SH-" + shMatcher.group(1);
        }

        // Named corridor normalization
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("badrinath")) {
            return "NH-7 (BADRINATH HIGHWAY)";
        }
        if (lower.contains("kedarnath")) {
            return "NH-107 (KEDARNATH HIGHWAY)";
        }
        if (lower.contains("chopta") || lower.contains("mandal")) {
            return "SH-64 (CHOPTA-MANDAL ROAD)";
        }
        if (lower.contains("rispana")) {
            return "HARIDWAR BYPASS - RISPANA BRIDGE";
        }
        if (lower.contains("rajpur")) {
            return "RAJPUR ROAD";
        }
        if (lower.contains("sahastradhara")) {
            return "SAHASTRADHARA ROAD";
        }

        // Clean punctuation and uppercase
        return text.replaceAll("[^a-zA-Z0-9\\s-]", " ")
                   .replaceAll("\\s+", " ")
                   .toUpperCase(Locale.ROOT)
                   .trim();
    }

    public RoadStatus normalizeStatus(String raw) {
        if (raw == null) return RoadStatus.UNCERTAIN;
        String s = raw.toLowerCase(Locale.ROOT).trim();

        if (s.contains("flood") || s.contains("submerg") || s.contains("waterlog")
                || s.contains("inundat") || s.contains("overflow") || s.contains("underwater")) {
            return RoadStatus.FLOODED;
        }

        if (s.contains("block") || s.contains("clos") || s.contains("landslide")
                || s.contains("debris") || s.contains("collaps") || s.contains("impass")
                || s.contains("cut off") || s.contains("damage")) {
            return RoadStatus.BLOCKED;
        }

        if (s.contains("open") || s.contains("clear") || s.contains("passable")
                || s.contains("normal") || s.contains("restor")) {
            return RoadStatus.OPEN;
        }

        return RoadStatus.UNCERTAIN;
    }

    private double[] lookupGazetteer(String roadName, String locationDesc) {
        String combined = ((roadName != null ? roadName : "") + " "
                         + (locationDesc != null ? locationDesc : "")).toLowerCase(Locale.ROOT);

        for (Map.Entry<String, double[]> entry : GAZETTEER.entrySet()) {
            if (combined.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
