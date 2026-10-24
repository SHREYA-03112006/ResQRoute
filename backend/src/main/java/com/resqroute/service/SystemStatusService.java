package com.resqroute.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides system-status information for the dashboard:
 * data-mode (LIVE / SAMPLE / MOCK), last refresh time, and backend health.
 *
 * <p>In Phase 1 the application always reports SAMPLE mode because
 * TinyFish integration is not yet implemented.
 * Phase 3 will update this service to reflect live-data availability.
 */
@Service
@Slf4j
public class SystemStatusService {

    @Value("${resqroute.tinyfish.mock-mode:true}")
    private boolean mockMode;

    @Value("${resqroute.pilot-district.name:Chamoli / Rudraprayag, Uttarakhand}")
    private String pilotDistrict;

    private Instant lastRefresh = Instant.now();

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("dataMode", mockMode ? "SAMPLE" : "LIVE");
        status.put("pilotDistrict", pilotDistrict);
        status.put("lastRefresh", lastRefresh.toString());
        status.put("tinyFishIntegrated", !mockMode);
        status.put("phase", "Phase 1 — Project Setup and Basic Dashboard");
        status.put("serverTime", Instant.now().toString());
        return status;
    }

    public void recordRefresh() {
        this.lastRefresh = Instant.now();
        log.debug("System refresh recorded at {}", lastRefresh);
    }
}
