package com.resqroute;

import com.resqroute.dto.RawDisasterReportDto;
import com.resqroute.dto.TinyFishDto;
import com.resqroute.model.DisasterCategory;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import com.resqroute.service.RoadReportService;
import com.resqroute.service.normalization.ReportNormalizationService;
import com.resqroute.service.pipeline.ReportIngestionPipeline;
import com.resqroute.service.tinyfish.TinyFishClient;
import com.resqroute.service.tinyfish.TinyFishMockProvider;
import com.resqroute.service.validation.ReportValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:phase2db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=simple",
        "resqroute.tinyfish.mock-mode=true"
})
public class Phase2DisasterIntelligenceTests {

    @Autowired
    private ReportValidationService validationService;

    @Autowired
    private ReportNormalizationService normalizationService;

    @Autowired
    private ReportIngestionPipeline ingestionPipeline;

    @Autowired
    private RoadReportService roadReportService;

    @Autowired
    private TinyFishClient tinyFishClient;

    @Autowired
    private TinyFishMockProvider mockProvider;

    @Test
    @DisplayName("1. Disaster category normalization standardizes Fire, Flood, Landslide, Blockage, Collapse, and Other")
    void testDisasterCategoryNormalization() {
        assertEquals(DisasterCategory.FIRE, normalizationService.normalizeDisasterCategory(null, "Commercial godown blaze", "Fire alert"));
        assertEquals(DisasterCategory.FLOOD, normalizationService.normalizeDisasterCategory(null, "Rispana river surge inundates road", "Flash Flood"));
        assertEquals(DisasterCategory.LANDSLIDE, normalizationService.normalizeDisasterCategory(null, "Mudslide and rockfall on highway", "Hill slip"));
        assertEquals(DisasterCategory.ROAD_BLOCKAGE, normalizationService.normalizeDisasterCategory(null, "Fallen trees and barricades", "Traffic halt"));
        assertEquals(DisasterCategory.BUILDING_COLLAPSE, normalizationService.normalizeDisasterCategory(null, "Structural cave-in near bridge pier", "Building damage"));
        assertEquals(DisasterCategory.OTHER, normalizationService.normalizeDisasterCategory(null, "Minor medical delay", "Routine dispatch"));

        // Explicit enum string
        assertEquals(DisasterCategory.FLOOD, normalizationService.normalizeDisasterCategory("FLOOD", null, null));
        assertEquals(DisasterCategory.FIRE, normalizationService.normalizeDisasterCategory("FIRE", null, null));
    }

    @Test
    @DisplayName("2. Multi-city detection and gazetteer resolve coordinates across Uttarakhand sectors")
    void testMultiCityResolution() {
        assertEquals("dehradun", normalizationService.detectCity(null, "Rajpur Road", "Old Mussoorie Road"));
        assertEquals("rishikesh", normalizationService.detectCity(null, "Triveni Ghat Access", "Muni Ki Reti"));
        assertEquals("haridwar", normalizationService.detectCity(null, "Laksar Road", "Har Ki Pauri"));
        assertEquals("nainital", normalizationService.detectCity(null, "Lower Mall Road", "Tallital"));

        // Gazetteer coordinates lookup for Dehradun
        RawDisasterReportDto ddnReport = new RawDisasterReportDto(
                "Haridwar Bypass - Rispana Bridge", "BLOCKED", "Rispana River corridor",
                null, null, "USDMA", "https://example.com/rispana", "GOVERNMENT_AGENCY", Instant.now(), "Flash flood"
        );
        RoadReport norm = normalizationService.normalize(ddnReport);
        assertEquals("dehradun", norm.getCity());
        assertEquals(DisasterCategory.FLOOD, norm.getDisasterCategory());
        assertNotNull(norm.getLatitude(), "Gazetteer should resolve Dehradun Rispana latitude");
        assertEquals(30.3120, norm.getLatitude(), 0.01);
    }

    @Test
    @DisplayName("3. URL and payload security validation rejects non-HTTP/HTTPS and malformed requests")
    void testSecurityValidation() {
        RawDisasterReportDto unsafeUrl = new RawDisasterReportDto(
                "Rajpur Road", "BLOCKED", "Near toll", 30.385, 78.082,
                "SDRF", "javascript:alert('xss')", "EMERGENCY_SERVICE", Instant.now(), null
        );
        ReportValidationService.ValidationResult res1 = validationService.validate(unsafeUrl);
        assertFalse(res1.isValid(), "javascript: protocol URL must be rejected");

        RawDisasterReportDto fileUrl = new RawDisasterReportDto(
                "Rajpur Road", "BLOCKED", "Near toll", 30.385, 78.082,
                "SDRF", "file:///etc/passwd", "EMERGENCY_SERVICE", Instant.now(), null
        );
        ReportValidationService.ValidationResult res2 = validationService.validate(fileUrl);
        assertFalse(res2.isValid(), "file: protocol URL must be rejected");

        RawDisasterReportDto validHttps = new RawDisasterReportDto(
                "Rajpur Road", "BLOCKED", "Near toll", 30.385, 78.082,
                "SDRF", "https://sdrf.uk.gov.in/alert", "EMERGENCY_SERVICE", Instant.now(), null
        );
        ReportValidationService.ValidationResult res3 = validationService.validate(validHttps);
        assertTrue(res3.isValid(), "Valid https URL must be accepted");
    }

    @Test
    @DisplayName("4. Duplicate report detection correctly flags DUPLICATE and UPDATED incident records")
    void testDuplicateAndUpdatedDetection() {
        RawDisasterReportDto r1 = new RawDisasterReportDto(
                "[DEMO] Dehradun Chakrata Bypass Flood", "FLOOD", "dehradun", "HIGH",
                "Chakrata Road Evacuation Bypass", "FLOODED", "Near Ballupur Chowk",
                30.334, 77.995, "SDRF", "https://sdrf.uk.gov.in/chakrata-alert",
                "EMERGENCY_SERVICE", Instant.now(), "Waterlogging across bypass road"
        );
        RoadReport saved1 = ingestionPipeline.processReport(r1);
        assertEquals("NEW", saved1.getVerificationStatus(), "Fresh corridor should be NEW");

        // Ingest identical report with same URL and status -> DUPLICATE
        RawDisasterReportDto r2 = new RawDisasterReportDto(
                "[DEMO] Dehradun Chakrata Bypass Flood Repeated", "FLOOD", "dehradun", "HIGH",
                "Chakrata Road Evacuation Bypass", "FLOODED", "Near Ballupur Chowk",
                30.334, 77.995, "SDRF", "https://sdrf.uk.gov.in/chakrata-alert",
                "EMERGENCY_SERVICE", Instant.now(), "Waterlogging across bypass road"
        );
        RoadReport saved2 = ingestionPipeline.processReport(r2);
        assertEquals("DUPLICATE", saved2.getVerificationStatus(), "Second identical report must be marked DUPLICATE");

        // Ingest update where status changes to OPEN -> UPDATED
        RawDisasterReportDto r3 = new RawDisasterReportDto(
                "[DEMO] Dehradun Chakrata Bypass Restored", "FLOOD", "dehradun", "LOW",
                "Chakrata Road Evacuation Bypass", "OPEN", "Near Ballupur Chowk",
                30.334, 77.995, "SDRF", "https://sdrf.uk.gov.in/chakrata-alert",
                "EMERGENCY_SERVICE", Instant.now(), "Bypass pumped dry and open"
        );
        RoadReport saved3 = ingestionPipeline.processReport(r3);
        assertEquals("UPDATED", saved3.getVerificationStatus(), "Status change on same corridor must be marked UPDATED");
    }

    @Test
    @DisplayName("5. TinyFish client operates in mock mode when key is absent and extracts multi-city reports")
    void testTinyFishMultiCityMockIngestion() {
        assertFalse(tinyFishClient.isLiveMode(), "Should be in mock mode when no live key is set");

        // Targeted search for Dehradun
        List<RoadReport> ddnReports = ingestionPipeline.triggerTinyFishSync(null, "dehradun", "ALL");
        assertFalse(ddnReports.isEmpty(), "Should extract reports for Dehradun");
        assertTrue(ddnReports.stream().anyMatch(r -> "dehradun".equalsIgnoreCase(r.getCity())), "Reports must contain Dehradun incidents");
        assertTrue(ddnReports.stream().anyMatch(r -> r.getDisasterCategory() == DisasterCategory.FLOOD || r.getDisasterCategory() == DisasterCategory.LANDSLIDE));

        // Verify data mode is explicitly marked as SAMPLE
        for (RoadReport r : ddnReports) {
            assertEquals("SAMPLE", r.getDataMode(), "Mock reports must be marked as SAMPLE");
            assertNotNull(r.getConfidenceScore());
        }
    }

    @Test
    @DisplayName("6. Service filtering supports multi-dimensional queries (city, category, status, severity)")
    void testServiceFiltering() {
        List<RoadReport> dehradunReports = roadReportService.getFilteredReports("dehradun", null, null, null, null, null, 48);
        assertNotNull(dehradunReports);

        List<RoadReport> floodReports = roadReportService.getFilteredReports(null, "FLOOD", null, null, null, null, 48);
        assertNotNull(floodReports);
        for (RoadReport r : floodReports) {
            assertEquals(DisasterCategory.FLOOD, r.getDisasterCategory());
        }

        List<RoadReport> blockedReports = roadReportService.getFilteredReports(null, null, "BLOCKED", null, null, null, 48);
        for (RoadReport r : blockedReports) {
            assertEquals(RoadStatus.BLOCKED, r.getReportedStatus());
        }
    }
}
