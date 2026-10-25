package com.resqroute.controller;

import com.resqroute.dto.RawDisasterReportDto;
import com.resqroute.dto.ReviewReportRequest;
import com.resqroute.model.DisasterCategory;
import com.resqroute.model.GraphEdge;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import com.resqroute.service.RoadReportService;
import com.resqroute.service.confidence.ConfidenceScoringService;
import com.resqroute.service.graph.DecoupledRoadGraphService;
import com.resqroute.service.pipeline.ReportIngestionPipeline;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * REST controller for disaster reports, TinyFish web intelligence pipeline,
 * and graph condition state (Phase 2).
 *
 * <p>Base path: {@code /api/reports}
 */
@RestController
@RequestMapping("/reports")
public class RoadReportController {

    private final RoadReportService roadReportService;
    private final ReportIngestionPipeline ingestionPipeline;
    private final DecoupledRoadGraphService graphService;
    private final ConfidenceScoringService confidenceService;

    public RoadReportController(RoadReportService roadReportService,
                                ReportIngestionPipeline ingestionPipeline,
                                DecoupledRoadGraphService graphService,
                                ConfidenceScoringService confidenceService) {
        this.roadReportService = roadReportService;
        this.ingestionPipeline = ingestionPipeline;
        this.graphService = graphService;
        this.confidenceService = confidenceService;
    }

    /**
     * GET /api/reports — list disaster reports with rich multi-dimensional filtering.
     * Supports: city, category, status, severity, flagged, dataMode, hoursBack.
     */
    @GetMapping
    public ResponseEntity<List<RoadReport>> getReports(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean flagged,
            @RequestParam(required = false) String dataMode,
            @RequestParam(defaultValue = "48") int hoursBack) {

        List<RoadReport> filtered = roadReportService.getFilteredReports(
                city, category, status, severity, flagged, dataMode, hoursBack
        );
        return ResponseEntity.ok(filtered);
    }

    /** GET /api/reports/{id} — get a report by ID with full details. */
    @GetMapping("/{id}")
    public ResponseEntity<RoadReport> getReportById(@PathVariable Long id) {
        return roadReportService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/reports/categories — returns supported disaster categories and incident statistics. */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        List<RoadReport> all = roadReportService.getAllReports();
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (DisasterCategory cat : DisasterCategory.values()) {
            counts.put(cat.name(), 0);
        }
        for (RoadReport r : all) {
            if (r.getDisasterCategory() != null) {
                String name = r.getDisasterCategory().name();
                counts.put(name, counts.getOrDefault(name, 0) + 1);
            }
        }

        return ResponseEntity.ok(Map.of(
                "categories", Arrays.stream(DisasterCategory.values()).map(Enum::name).toList(),
                "counts", counts,
                "totalReports", all.size()
        ));
    }

    /** POST /api/reports/ingest — Ingest raw disaster report through Phase 2 pipeline. */
    @PostMapping("/ingest")
    public ResponseEntity<RoadReport> ingestReport(@Valid @RequestBody RawDisasterReportDto rawDto) {
        RoadReport processed = ingestionPipeline.processReport(rawDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(processed);
    }

    /** POST /api/reports/batch — Ingest batch of reports. */
    @PostMapping("/batch")
    public ResponseEntity<List<RoadReport>> ingestBatch(@RequestBody List<RawDisasterReportDto> rawList) {
        List<RoadReport> processed = ingestionPipeline.processBatch(rawList);
        return ResponseEntity.status(HttpStatus.CREATED).body(processed);
    }

    /**
     * POST /api/reports/tinyfish/trigger-fetch — Run TinyFish discovery & extraction
     * targeted at a specific city or disaster category.
     */
    @PostMapping("/tinyfish/trigger-fetch")
    public ResponseEntity<Map<String, Object>> triggerTinyFish(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category) {

        List<RoadReport> ingested = ingestionPipeline.triggerTinyFishSync(query, city, category);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "city", city != null ? city : "all",
                "category", category != null ? category : "all",
                "extractedCount", ingested.size(),
                "reports", ingested,
                "message", "TinyFish disaster intelligence cycle completed successfully."
        ));
    }

    /** GET /api/reports/graph-state — Inspect current road graph edge conditions. */
    @GetMapping("/graph-state")
    public ResponseEntity<List<GraphEdge>> getGraphState() {
        return ResponseEntity.ok(graphService.getAllEdges());
    }

    /** GET /api/reports/stale — Retrieve reports older than configured stale threshold. */
    @GetMapping("/stale")
    public ResponseEntity<List<RoadReport>> getStaleReports() {
        int staleHours = (int) confidenceService.getStaleHours();
        Instant cutoff = Instant.now().minus(staleHours, ChronoUnit.HOURS);
        List<RoadReport> all = roadReportService.getAllReports();
        List<RoadReport> stale = all.stream()
                .filter(r -> r.getObservedAt() != null && r.getObservedAt().isBefore(cutoff))
                .toList();
        return ResponseEntity.ok(stale);
    }

    /** POST /api/reports/{id}/review — Dispatcher reviews, adds notes, or resolves a report. */
    @PostMapping("/{id}/review")
    public ResponseEntity<RoadReport> reviewReport(
            @PathVariable Long id,
            @RequestBody ReviewReportRequest request) {

        return roadReportService.getReportById(id).map(report -> {
            if (request.getFlaggedForReview() != null) {
                report.setFlaggedForReview(request.getFlaggedForReview());
            }
            if (request.getReviewNotes() != null) {
                report.setReviewNotes(request.getReviewNotes());
            }
            if (request.getStatusOverride() != null && !request.getStatusOverride().trim().isEmpty()) {
                try {
                    RoadStatus newStatus = RoadStatus.valueOf(request.getStatusOverride().toUpperCase());
                    report.setReportedStatus(newStatus);
                    if (report.getMatchedEdgeId() != null) {
                        graphService.updateEdgeCondition(report.getMatchedEdgeId(), newStatus, report.getId());
                    }
                } catch (IllegalArgumentException ignored) {}
            }
            if (request.getManualEdgeId() != null) {
                report.setMatchedEdgeId(request.getManualEdgeId());
                report.setMatchStatus("MATCHED");
                graphService.updateEdgeCondition(request.getManualEdgeId(), report.getReportedStatus(), report.getId());
            }

            RoadReport saved = roadReportService.saveReport(report);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }
}
