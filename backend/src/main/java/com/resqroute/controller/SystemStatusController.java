package com.resqroute.controller;

import com.resqroute.service.SystemStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing system status for the dashboard header.
 *
 * <p>Base path: {@code /api/status}
 */
@RestController
@RequestMapping("/status")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    /** GET /api/status — returns data mode, last refresh time and pilot district. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(systemStatusService.getStatus());
    }
}
