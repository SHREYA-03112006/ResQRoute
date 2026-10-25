package com.resqroute;

import com.resqroute.dto.ConfidenceScoreBreakdown;
import com.resqroute.dto.RawDisasterReportDto;
import com.resqroute.dto.RoadMatchResult;
import com.resqroute.model.GraphEdge;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import com.resqroute.service.confidence.ConfidenceScoringService;
import com.resqroute.service.graph.DecoupledRoadGraphService;
import com.resqroute.service.matching.RoadMatchingService;
import com.resqroute.service.normalization.ReportNormalizationService;
import com.resqroute.service.pipeline.ReportIngestionPipeline;
import com.resqroute.service.validation.ReportValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:phase3db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=simple",
        "resqroute.tinyfish.mock-mode=true"
})
public class Phase3PipelineTests {

    @Autowired
    private ReportValidationService validationService;

    @Autowired
    private ReportNormalizationService normalizationService;

    @Autowired
    private RoadMatchingService matchingService;

    @Autowired
    private ConfidenceScoringService confidenceService;

    @Autowired
    private DecoupledRoadGraphService graphService;

    @Autowired
    private ReportIngestionPipeline ingestionPipeline;

    @BeforeEach
    void setUp() {
        graphService.resetAllEdges();
    }

    @Test
    @DisplayName("1. Schema validation accepts complete reports and rejects missing required fields")
    void testValidation() {
        RawDisasterReportDto valid = new RawDisasterReportDto(
                "NH-107 Tilwara", "BLOCKED", "Near bridge", 30.292, 78.962,
                "USDMA", "https://example.com", "GOVERNMENT_AGENCY", Instant.now(), "Debris on road"
        );
        ReportValidationService.ValidationResult res1 = validationService.validate(valid);
        assertTrue(res1.isValid(), "Valid report should pass validation");

        RawDisasterReportDto invalid = new RawDisasterReportDto(
                "", null, "Desc", 120.0, 78.9, null, null, null, null, null
        );
        ReportValidationService.ValidationResult res2 = validationService.validate(invalid);
        assertFalse(res2.isValid(), "Invalid report should fail validation");
        assertTrue(res2.getErrors().size() >= 3, "Should report missing road name, status, and invalid latitude");
    }

    @Test
    @DisplayName("2. Normalization standardizes highway identifiers, statuses, and gazetteer coordinates")
    void testNormalization() {
        assertEquals("NH-7 (BADRINATH HIGHWAY)", normalizationService.normalizeRoadName("Badrinath Highway near Pipalkoti"));
        assertEquals("NH-7", normalizationService.normalizeRoadName("National Highway 7"));
        assertEquals("NH-107", normalizationService.normalizeRoadName("NH 107 Kedarnath route"));

        assertEquals(RoadStatus.FLOODED, normalizationService.normalizeStatus("submerged by river water"));
        assertEquals(RoadStatus.BLOCKED, normalizationService.normalizeStatus("road closed due to landslide"));
        assertEquals(RoadStatus.OPEN, normalizationService.normalizeStatus("traffic restored and clear"));
        assertEquals(RoadStatus.UNCERTAIN, normalizationService.normalizeStatus("unconfirmed rumour"));

        // Gazetteer lookup when coordinates are missing
        RawDisasterReportDto missingCoords = new RawDisasterReportDto(
                "NH-7 Pipalkoti Road", "FLOODED", "Pipalkoti town area",
                null, null, "IMD", null, "MET_OFFICE", Instant.now(), null
        );
        RoadReport norm = normalizationService.normalize(missingCoords);
        assertNotNull(norm.getLatitude(), "Latitude should be filled from gazetteer");
        assertNotNull(norm.getLongitude(), "Longitude should be filled from gazetteer");
        assertEquals(30.473, norm.getLatitude(), 0.01);
    }

    @Test
    @DisplayName("3. Road matching links reports to correct graph edges and detects conflicts")
    void testRoadMatching() {
        RoadReport report = RoadReport.builder()
                .reportedRoadName("NH 107 Rudraprayag-Tilwara")
                .normalisedRoadName("NH-107")
                .reportedStatus(RoadStatus.BLOCKED)
                .latitude(30.290)
                .longitude(78.970)
                .locationDescription("Tilwara bridge")
                .build();

        RoadMatchResult match = matchingService.match(report, Collections.emptyList());
        assertEquals("MATCHED", match.getMatchStatus());
        assertEquals(1L, match.getMatchedEdgeId());
        assertTrue(match.getMatchScore() >= 0.65);

        // Conflict check: existing report says OPEN, new report says BLOCKED
        RoadReport existing = RoadReport.builder()
                .matchedEdgeId(1L)
                .reportedStatus(RoadStatus.OPEN)
                .build();

        RoadMatchResult conflictMatch = matchingService.match(report, List.of(existing));
        assertEquals("CONFLICT", conflictMatch.getMatchStatus(), "Should flag conflict when statuses oppose");
    }

    @Test
    @DisplayName("4. Confidence scoring applies source weighting, recency decay, and flags uncertain reports")
    void testConfidenceScoring() {
        // High confidence official report
        RoadReport officialReport = RoadReport.builder()
                .reportedRoadName("NH-107 Tilwara")
                .sourceName("Uttarakhand State Disaster Management Authority (USDMA)")
                .observedAt(Instant.now())
                .reportedStatus(RoadStatus.BLOCKED)
                .build();
        RoadMatchResult strongMatch = new RoadMatchResult(1L, "NH 107", "MATCHED", 0.90, "High match");

        ConfidenceScoreBreakdown score1 = confidenceService.computeScore(officialReport, strongMatch, Collections.emptyList());
        assertTrue(score1.getFinalScore() >= 0.70, "Official fresh report should exceed accept threshold");
        assertFalse(score1.isFlaggedForReview(), "High confidence report should not be flagged");

        // Stale report (24 hours old)
        RoadReport staleReport = RoadReport.builder()
                .reportedRoadName("NH-107 Tilwara")
                .sourceName("USDMA")
                .observedAt(Instant.now().minus(24, ChronoUnit.HOURS))
                .reportedStatus(RoadStatus.BLOCKED)
                .build();

        ConfidenceScoreBreakdown score2 = confidenceService.computeScore(staleReport, strongMatch, Collections.emptyList());
        assertTrue(score2.getFinalScore() < score1.getFinalScore(), "Stale report score must decay");
        assertTrue(score2.getRecencyScore() < 0.20, "Recency score should be heavily decayed");
    }

    @Test
    @DisplayName("5. Ingestion pipeline accepts report, updates road graph edge cost, and triggers TinyFish sync")
    void testEndToEndPipeline() {
        RawDisasterReportDto raw = new RawDisasterReportDto(
                "NH 107 Rudraprayag to Tilwara",
                "BLOCKED",
                "Tilwara bridge area",
                30.292, 78.962,
                "Uttarakhand State Disaster Management Authority",
                "https://usdma.uk.gov.in/alert",
                "GOVERNMENT_AGENCY",
                Instant.now(),
                "Road blocked due to heavy landslide"
        );

        RoadReport saved = ingestionPipeline.processReport(raw);
        assertNotNull(saved.getId());
        assertEquals("NH-107", saved.getNormalisedRoadName());
        assertEquals(1L, saved.getMatchedEdgeId());
        assertTrue(saved.getConfidenceScore() >= 0.70);

        // Verify graph edge was updated to BLOCKED with infinite cost
        GraphEdge edge = graphService.getEdgeById(1L).orElseThrow();
        assertEquals(RoadStatus.BLOCKED, edge.getStatus());
        assertEquals(Double.POSITIVE_INFINITY, edge.getCurrentCostMinutes());

        // Test TinyFish discovery & extraction cycle in mock mode
        List<RoadReport> tinyFishReports = ingestionPipeline.triggerTinyFishSync(null);
        assertFalse(tinyFishReports.isEmpty(), "TinyFish sync should produce extracted reports");
        assertTrue(tinyFishReports.size() >= 3);
    }
}
