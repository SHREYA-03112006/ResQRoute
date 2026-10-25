package com.resqroute.service;

import com.resqroute.model.DisasterCategory;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import com.resqroute.repository.RoadReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Service layer for managing road-condition and disaster reports (Phase 2).
 */
@Service
public class RoadReportService {

    private static final Logger log = LoggerFactory.getLogger(RoadReportService.class);

    private final RoadReportRepository roadReportRepository;

    public RoadReportService(RoadReportRepository roadReportRepository) {
        this.roadReportRepository = roadReportRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "road-reports", key = "'all'")
    public List<RoadReport> getAllReports() {
        return roadReportRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RoadReport> getRecentReports(int hoursBack) {
        Instant since = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        return roadReportRepository.findRecentReports(since);
    }

    @Transactional(readOnly = true)
    public List<RoadReport> getFlaggedReports() {
        return roadReportRepository.findByFlaggedForReviewTrue();
    }

    @Transactional(readOnly = true)
    public List<RoadReport> getReportsByStatus(RoadStatus status) {
        return roadReportRepository.findByReportedStatus(status);
    }

    @Transactional(readOnly = true)
    public Optional<RoadReport> getReportById(Long id) {
        return roadReportRepository.findById(id);
    }

    /**
     * Retrieves reports filtered by city, disaster category, status, severity, flagged, dataMode, and hoursBack.
     */
    @Transactional(readOnly = true)
    public List<RoadReport> getFilteredReports(String city, String category, String status, String severity,
                                              Boolean flagged, String dataMode, int hoursBack) {
        List<RoadReport> reports = getRecentReports(hoursBack);

        return reports.stream()
                .filter(r -> {
                    if (city != null && !city.trim().isEmpty() && !"ALL".equalsIgnoreCase(city)) {
                        if (r.getCity() == null || !r.getCity().equalsIgnoreCase(city.trim())) {
                            return false;
                        }
                    }
                    if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                        if (r.getDisasterCategory() == null || !r.getDisasterCategory().name().equalsIgnoreCase(category.trim())) {
                            return false;
                        }
                    }
                    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                        if (r.getReportedStatus() == null || !r.getReportedStatus().name().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }
                    if (severity != null && !severity.trim().isEmpty() && !"ALL".equalsIgnoreCase(severity)) {
                        if (r.getSeverity() == null || !r.getSeverity().equalsIgnoreCase(severity.trim())) {
                            return false;
                        }
                    }
                    if (flagged != null) {
                        if (!flagged.equals(r.getFlaggedForReview())) {
                            return false;
                        }
                    }
                    if (dataMode != null && !dataMode.trim().isEmpty() && !"ALL".equalsIgnoreCase(dataMode)) {
                        if (r.getDataMode() == null || !r.getDataMode().equalsIgnoreCase(dataMode.trim())) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    @Transactional
    @CacheEvict(value = "road-reports", allEntries = true)
    public RoadReport saveReport(RoadReport report) {
        log.info("Saving disaster report: {} [category={}, city={}, status={}, mode={}]",
                report.getReportedRoadName(), report.getDisasterCategory(), report.getCity(),
                report.getReportedStatus(), report.getDataMode());
        return roadReportRepository.save(report);
    }

    @Transactional
    @CacheEvict(value = "road-reports", allEntries = true)
    public Optional<RoadReport> updateReviewNotes(Long id, String notes, boolean flagged) {
        return roadReportRepository.findById(id).map(report -> {
            report.setReviewNotes(notes);
            report.setFlaggedForReview(flagged);
            return roadReportRepository.save(report);
        });
    }

    /**
     * Loads Phase 2 sample reports into the database if none exist.
     */
    @Transactional
    public void loadSampleReportsIfEmpty() {
        if (roadReportRepository.count() > 0) {
            log.debug("Sample reports already exist, skipping seed.");
            return;
        }
        log.info("Loading initial multi-city disaster reports for development/demo (SAMPLE mode).");

        List<RoadReport> samples = List.of(
            // Dehradun
            RoadReport.builder()
                .incidentTitle("[DEMO] Rispana River Flash Flood Inundation")
                .disasterCategory(DisasterCategory.FLOOD)
                .city("dehradun")
                .severity("CRITICAL")
                .reportedRoadName("Haridwar Bypass - Rispana Bridge")
                .normalisedRoadName("HARIDWAR BYPASS - RISPANA BRIDGE")
                .reportedStatus(RoadStatus.BLOCKED)
                .locationDescription("Rispana River bridge approach submerged under 1.2m rushing floodwater.")
                .latitude(30.3120).longitude(78.0550)
                .sourceName("[SAMPLE] Uttarakhand State Disaster Management Authority")
                .sourceUrl("https://usdma.uk.gov.in/dehradun-rispana-alert")
                .observedAt(Instant.now().minus(45, ChronoUnit.MINUTES))
                .confidenceScore(0.92)
                .matchStatus("MATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(false)
                .verificationStatus("NEW")
                .build(),

            RoadReport.builder()
                .incidentTitle("[DEMO] Rajpur Road Hillside Landslide")
                .disasterCategory(DisasterCategory.LANDSLIDE)
                .city("dehradun")
                .severity("CRITICAL")
                .reportedRoadName("Rajpur Road (Old Toll Post)")
                .normalisedRoadName("RAJPUR ROAD")
                .reportedStatus(RoadStatus.BLOCKED)
                .locationDescription("Mudflow and large rock boulders completely obstructing both lanes.")
                .latitude(30.3850).longitude(78.0820)
                .sourceName("[SAMPLE] State Disaster Response Force (SDRF)")
                .sourceUrl("https://sdrf.uk.gov.in/rajpur-advisory")
                .observedAt(Instant.now().minus(90, ChronoUnit.MINUTES))
                .confidenceScore(0.88)
                .matchStatus("MATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(false)
                .verificationStatus("NEW")
                .build(),

            RoadReport.builder()
                .incidentTitle("[DEMO] Sahastradhara IT Park Flash Waterlogging")
                .disasterCategory(DisasterCategory.FLOOD)
                .city("dehradun")
                .severity("HIGH")
                .reportedRoadName("Sahastradhara Road (Near IT Park)")
                .normalisedRoadName("SAHASTRADHARA ROAD")
                .reportedStatus(RoadStatus.FLOODED)
                .locationDescription("Heavy waterlogging observed near IT Park; caution advised.")
                .latitude(30.3620).longitude(78.0780)
                .sourceName("[SAMPLE] Dehradun Municipal Corporation")
                .sourceUrl("https://dehradun.nic.in/alerts/sahastradhara")
                .observedAt(Instant.now().minus(3, ChronoUnit.HOURS))
                .confidenceScore(0.82)
                .matchStatus("MATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(false)
                .verificationStatus("NEW")
                .build(),

            // Rudraprayag / Chamoli
            RoadReport.builder()
                .incidentTitle("[DEMO] NH-107 Tilwara River Inundation & Boulders")
                .disasterCategory(DisasterCategory.FLOOD)
                .city("rudraprayag")
                .severity("CRITICAL")
                .reportedRoadName("NH 7 Rudraprayag-Gaurikund")
                .normalisedRoadName("NH-107")
                .reportedStatus(RoadStatus.BLOCKED)
                .locationDescription("Near Tilwara, Rudraprayag district")
                .latitude(30.285).longitude(78.981)
                .sourceName("[SAMPLE] Uttarakhand State Disaster Management Authority")
                .sourceUrl("https://usdma.uk.gov.in/tilwara-nh107")
                .observedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .confidenceScore(0.85)
                .matchStatus("MATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(false)
                .verificationStatus("NEW")
                .build(),

            RoadReport.builder()
                .incidentTitle("[DEMO] NH-7 Pipalkoti Badrinath Highway Flood")
                .disasterCategory(DisasterCategory.FLOOD)
                .city("chamoli")
                .severity("HIGH")
                .reportedRoadName("Badrinath Highway")
                .normalisedRoadName("NH-7 (BADRINATH HIGHWAY)")
                .reportedStatus(RoadStatus.FLOODED)
                .locationDescription("Chamoli district, near Pipalkoti")
                .latitude(30.472).longitude(79.479)
                .sourceName("[SAMPLE] India Meteorological Department Advisory")
                .sourceUrl("https://mausam.imd.gov.in/pipalkoti")
                .observedAt(Instant.now().minus(4, ChronoUnit.HOURS))
                .confidenceScore(0.72)
                .matchStatus("MATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(false)
                .verificationStatus("NEW")
                .build()
        );

        roadReportRepository.saveAll(samples);
        log.info("Loaded {} multi-city sample disaster reports.", samples.size());
    }
}
