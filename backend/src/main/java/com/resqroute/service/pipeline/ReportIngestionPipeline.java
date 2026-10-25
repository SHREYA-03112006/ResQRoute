package com.resqroute.service.pipeline;

import com.resqroute.dto.ConfidenceScoreBreakdown;
import com.resqroute.dto.RawDisasterReportDto;
import com.resqroute.dto.RoadMatchResult;
import com.resqroute.dto.TinyFishDto;
import com.resqroute.model.RoadReport;
import com.resqroute.repository.RoadReportRepository;
import com.resqroute.service.SystemStatusService;
import com.resqroute.service.confidence.ConfidenceScoringService;
import com.resqroute.service.graph.DecoupledRoadGraphService;
import com.resqroute.service.matching.RoadMatchingService;
import com.resqroute.service.normalization.ReportNormalizationService;
import com.resqroute.service.tinyfish.TinyFishClient;
import com.resqroute.service.validation.ReportValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the Phase 2 disaster intelligence & TinyFish ingestion pipeline:
 * Validation -> Normalization -> Duplicate Detection -> Road Matching -> Confidence Scoring -> Graph Update -> Persistence.
 */
@Service
public class ReportIngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReportIngestionPipeline.class);

    private final ReportValidationService validationService;
    private final ReportNormalizationService normalizationService;
    private final RoadMatchingService matchingService;
    private final ConfidenceScoringService confidenceService;
    private final DecoupledRoadGraphService graphService;
    private final RoadReportRepository reportRepository;
    private final TinyFishClient tinyFishClient;
    private final SystemStatusService systemStatusService;

    public ReportIngestionPipeline(ReportValidationService validationService,
                                   ReportNormalizationService normalizationService,
                                   RoadMatchingService matchingService,
                                   ConfidenceScoringService confidenceService,
                                   DecoupledRoadGraphService graphService,
                                   RoadReportRepository reportRepository,
                                   TinyFishClient tinyFishClient,
                                   SystemStatusService systemStatusService) {
        this.validationService = validationService;
        this.normalizationService = normalizationService;
        this.matchingService = matchingService;
        this.confidenceService = confidenceService;
        this.graphService = graphService;
        this.reportRepository = reportRepository;
        this.tinyFishClient = tinyFishClient;
        this.systemStatusService = systemStatusService;
    }

    /**
     * Ingest a single raw disaster report through the complete pipeline.
     */
    @Transactional
    @CacheEvict(value = {"road-reports", "routes"}, allEntries = true)
    public RoadReport processReport(RawDisasterReportDto rawDto) {
        // Step 1: Validation (schema & URL protocol security)
        ReportValidationService.ValidationResult validation = validationService.validate(rawDto);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Report validation failed: " + String.join(", ", validation.getErrors()));
        }

        // Step 2: Normalization (category, city, road name, coordinates via gazetteer)
        RoadReport report = normalizationService.normalize(rawDto);
        report.setDataMode(tinyFishClient.isLiveMode() ? "LIVE" : "SAMPLE");

        // Step 3: Road Matching against graph edges
        List<RoadReport> existingActive = reportRepository.findAll();
        RoadMatchResult matchResult = matchingService.match(report, existingActive);

        report.setMatchStatus(matchResult.getMatchStatus());
        report.setMatchedEdgeId(matchResult.getMatchedEdgeId());

        // Step 4: Confidence Scoring
        ConfidenceScoreBreakdown confidence = confidenceService.computeScore(report, matchResult, existingActive);
        report.setConfidenceScore(confidence.getFinalScore());
        report.setFlaggedForReview(confidence.isFlaggedForReview());
        if (confidence.getFlagReason() != null && !confidence.getFlagReason().isEmpty()) {
            report.setReviewNotes(confidence.getFlagReason());
        }

        // Step 5: Duplicate & Update Detection
        RoadReport existingMatch = detectDuplicateOrUpdate(report, existingActive);

        // Persist report to database
        RoadReport saved = reportRepository.save(report);

        // Step 6: Road Graph update if confidence exceeds accept threshold (0.70)
        if (matchResult.getMatchedEdgeId() != null && confidence.getFinalScore() >= confidenceService.getAcceptThreshold()) {
            boolean updated = graphService.updateEdgeCondition(
                    matchResult.getMatchedEdgeId(),
                    saved.getReportedStatus(),
                    saved.getId()
            );
            if (updated) {
                log.info("Graph edge {} condition updated to {} by report id={}",
                        matchResult.getMatchedEdgeId(), saved.getReportedStatus(), saved.getId());
            }
        }

        systemStatusService.recordRefresh();
        return saved;
    }

    /**
     * Detects whether the new report is NEW, UPDATED, DUPLICATE, or UNVERIFIED.
     */
    private RoadReport detectDuplicateOrUpdate(RoadReport newReport, List<RoadReport> existingReports) {
        if (existingReports == null || existingReports.isEmpty()) {
            newReport.setVerificationStatus("NEW");
            return null;
        }

        for (RoadReport existing : existingReports) {
            if (existing.getId() == null) continue;

            // 1. Same source URL match
            boolean sameUrl = newReport.getSourceUrl() != null && !newReport.getSourceUrl().isEmpty()
                    && newReport.getSourceUrl().equalsIgnoreCase(existing.getSourceUrl());

            // 2. Same location/city and category match
            boolean sameLocationAndCategory = false;
            if (newReport.getCity() != null && newReport.getCity().equalsIgnoreCase(existing.getCity())
                    && newReport.getDisasterCategory() == existing.getDisasterCategory()) {
                if (newReport.getNormalisedRoadName() != null && newReport.getNormalisedRoadName().equalsIgnoreCase(existing.getNormalisedRoadName())) {
                    sameLocationAndCategory = true;
                } else if (newReport.getLatitude() != null && existing.getLatitude() != null
                        && Math.abs(newReport.getLatitude() - existing.getLatitude()) < 0.005
                        && Math.abs(newReport.getLongitude() - existing.getLongitude()) < 0.005) {
                    sameLocationAndCategory = true;
                }
            }

            if (sameUrl || sameLocationAndCategory) {
                if (newReport.getReportedStatus() == existing.getReportedStatus()) {
                    newReport.setVerificationStatus("DUPLICATE");
                    log.info("Duplicate report detected for '{}' (matches existing id={})",
                            newReport.getReportedRoadName(), existing.getId());
                    return existing;
                } else {
                    newReport.setVerificationStatus("UPDATED");
                    log.info("Updated condition detected for '{}' (status changed from {} to {})",
                            newReport.getReportedRoadName(), existing.getReportedStatus(), newReport.getReportedStatus());
                    return existing;
                }
            }
        }

        // If location is unknown (no coords and no matched road) or confidence is low, mark UNVERIFIED
        if ((newReport.getLatitude() == null || newReport.getLongitude() == null) && "UNMATCHED".equals(newReport.getMatchStatus())) {
            newReport.setVerificationStatus("UNVERIFIED");
        } else if (newReport.getConfidenceScore() != null && newReport.getConfidenceScore() < 0.40) {
            newReport.setVerificationStatus("UNVERIFIED");
        } else {
            newReport.setVerificationStatus("NEW");
        }
        return null;
    }

    /**
     * Batch ingestion of raw reports.
     */
    @Transactional
    @CacheEvict(value = {"road-reports", "routes"}, allEntries = true)
    public List<RoadReport> processBatch(List<RawDisasterReportDto> rawList) {
        List<RoadReport> results = new ArrayList<>();
        for (RawDisasterReportDto dto : rawList) {
            try {
                results.add(processReport(dto));
            } catch (Exception e) {
                log.error("Failed to process report for '{}': {}", dto.getReportedRoadName(), e.getMessage());
            }
        }
        return results;
    }

    /**
     * Executes the end-to-end TinyFish intelligence cycle with city and category targeting.
     */
    @Transactional
    public List<RoadReport> triggerTinyFishSync(String query, String city, String category) {
        StringBuilder sb = new StringBuilder();
        if (query != null && !query.trim().isEmpty()) {
            sb.append(query.trim());
        } else {
            if (city != null && !city.trim().isEmpty()) {
                sb.append("Uttarakhand ").append(city).append(" ");
            } else {
                sb.append("Uttarakhand ");
            }
            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                sb.append(category.toLowerCase()).append(" ");
            } else {
                sb.append("disaster flood landslide road blockage emergency ");
            }
            sb.append("official advisory");
        }

        String searchQuery = sb.toString().trim();
        log.info("Triggering TinyFish discovery and extraction cycle for query: '{}'", searchQuery);

        // 1. Search
        TinyFishDto.SearchResponse searchResp = tinyFishClient.search(searchQuery);
        List<RawDisasterReportDto> allExtracted = new ArrayList<>();

        if (searchResp != null && searchResp.getResults() != null) {
            int count = Math.min(4, searchResp.getResults().size());
            for (int i = 0; i < count; i++) {
                TinyFishDto.SearchResultItem item = searchResp.getResults().get(i);
                // 2. Fetch page content
                tinyFishClient.fetch(item.getUrl());

                // 3. AI Agent extraction
                TinyFishDto.AgentExtractionResponse agentResp = tinyFishClient.agentExtract(item.getUrl());
                if (agentResp != null && agentResp.getExtractedReports() != null) {
                    for (RawDisasterReportDto raw : agentResp.getExtractedReports()) {
                        if (city != null && (raw.getCity() == null || raw.getCity().isEmpty())) {
                            raw.setCity(city.toLowerCase());
                        }
                        allExtracted.add(raw);
                    }
                }
            }
        }

        // 4. Ingest all extracted reports
        log.info("TinyFish agent extracted {} structured reports. Ingesting into pipeline...", allExtracted.size());
        return processBatch(allExtracted);
    }

    /** Overload for backward compatibility */
    @Transactional
    public List<RoadReport> triggerTinyFishSync(String query) {
        return triggerTinyFishSync(query, null, null);
    }
}
