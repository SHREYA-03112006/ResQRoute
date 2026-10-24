package com.resqroute.controller;

import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import com.resqroute.service.RoadReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for road-condition reports.
 *
 * <p>Base path: {@code /api/reports}
 *
 * <p>Note: In Phase 1 all reports are SAMPLE data. Phase 3 will add TinyFish
 * integration and live data retrieval through this controller.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class RoadReportController {

    private final RoadReportService roadReportService;

    /** GET /api/reports — list all reports (optionally filter by status or flagged). */
    @GetMapping
    public ResponseEntity<List<RoadReport>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean flagged,
            @RequestParam(defaultValue = "24") int hoursBack) {

        List<RoadReport> reports;
        if (Boolean.TRUE.equals(flagged)) {
            reports = roadReportService.getFlaggedReports();
        } else if (status != null) {
            reports = roadReportService.getReportsByStatus(RoadStatus.valueOf(status.toUpperCase()));
        } else {
            reports = roadReportService.getRecentReports(hoursBack);
        }
        return ResponseEntity.ok(reports);
    }

    /** GET /api/reports/{id} — get a single report. */
    @GetMapping("/{id}")
    public ResponseEntity<RoadReport> getReport(@PathVariable Long id) {
        return roadReportService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/reports — submit a manual road-condition report. */
    @PostMapping
    public ResponseEntity<RoadReport> createReport(@Valid @RequestBody RoadReport report) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roadReportService.saveReport(report));
    }

    /**
     * PATCH /api/reports/{id}/review — update dispatcher review notes and flag status.
     * Body: { "notes": "...", "flagged": false }
     */
    @PatchMapping("/{id}/review")
    public ResponseEntity<RoadReport> updateReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String notes = (String) body.getOrDefault("notes", "");
        boolean flagged = Boolean.parseBoolean(String.valueOf(body.getOrDefault("flagged", "false")));
        return roadReportService.updateReviewNotes(id, notes, flagged)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
