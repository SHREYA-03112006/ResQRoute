package com.resqroute.service;

import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import com.resqroute.repository.RoadReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing road-condition reports.
 *
 * <p>Reports flow through: retrieval → validation → normalisation → confidence scoring
 * → road matching → graph update. This service handles persistence and querying.
 * Validation and confidence scoring are handled by dedicated services (added in Phase 3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoadReportService {

    private final RoadReportRepository roadReportRepository;

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

    @Transactional
    @CacheEvict(value = "road-reports", allEntries = true)
    public RoadReport saveReport(RoadReport report) {
        log.info("Saving road report: {} status={} mode={}",
                report.getReportedRoadName(), report.getReportedStatus(), report.getDataMode());
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
     * Loads Phase 1 sample reports into the database if none exist.
     * These are clearly marked with dataMode=SAMPLE and must never be
     * presented to the user as live or verified data.
     */
    @Transactional
    public void loadSampleReportsIfEmpty() {
        if (roadReportRepository.count() > 0) {
            log.debug("Sample reports already loaded, skipping.");
            return;
        }
        log.info("Loading sample road reports for development/demo (SAMPLE mode).");

        List<RoadReport> samples = List.of(
            RoadReport.builder()
                .reportedRoadName("NH 7 Rudraprayag-Gaurikund")
                .normalisedRoadName("NH7 RUDRAPRAYAG GAURIKUND")
                .reportedStatus(RoadStatus.BLOCKED)
                .locationDescription("Near Tilwara, Rudraprayag district")
                .latitude(30.285).longitude(78.981)
                .sourceName("[SAMPLE] Uttarakhand State Disaster Management Authority")
                .sourceUrl("https://example.com/usdma-sample")
                .observedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .confidenceScore(0.85)
                .matchStatus("UNMATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(false)
                .build(),

            RoadReport.builder()
                .reportedRoadName("Badrinath Highway")
                .normalisedRoadName("BADRINATH HIGHWAY")
                .reportedStatus(RoadStatus.FLOODED)
                .locationDescription("Chamoli district, near Pipalkoti")
                .latitude(30.472).longitude(79.479)
                .sourceName("[SAMPLE] India Meteorological Department Advisory")
                .sourceUrl("https://example.com/imd-sample")
                .observedAt(Instant.now().minus(4, ChronoUnit.HOURS))
                .confidenceScore(0.72)
                .matchStatus("UNMATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(false)
                .build(),

            RoadReport.builder()
                .reportedRoadName("Rudraprayag-Agastmuni Road")
                .normalisedRoadName("RUDRAPRAYAG AGASTMUNI ROAD")
                .reportedStatus(RoadStatus.UNCERTAIN)
                .locationDescription("Rudraprayag district")
                .latitude(30.296).longitude(78.983)
                .sourceName("[SAMPLE] Social Media Report")
                .sourceUrl("https://example.com/social-sample")
                .observedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .confidenceScore(0.35)
                .matchStatus("UNMATCHED")
                .dataMode("SAMPLE")
                .flaggedForReview(true)
                .build()
        );

        roadReportRepository.saveAll(samples);
        log.info("Loaded {} sample road reports.", samples.size());
    }
}
