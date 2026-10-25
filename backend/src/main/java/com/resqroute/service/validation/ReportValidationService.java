package com.resqroute.service.validation;

import com.resqroute.dto.RawDisasterReportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates incoming raw disaster report JSON payloads against schema and security rules (Phase 2).
 * Treats scraped web content as untrusted input.
 */
@Service
public class ReportValidationService {

    private static final Logger log = LoggerFactory.getLogger(ReportValidationService.class);

    @Value("${resqroute.pilot-district.bbox-min-lat:28.0}")
    private double bboxMinLat;

    @Value("${resqroute.pilot-district.bbox-max-lat:32.0}")
    private double bboxMaxLat;

    @Value("${resqroute.pilot-district.bbox-min-lon:77.0}")
    private double bboxMinLon;

    @Value("${resqroute.pilot-district.bbox-max-lon:81.5}")
    private double bboxMaxLon;

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }

    public ValidationResult validate(RawDisasterReportDto dto) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (dto == null) {
            errors.add("Report payload cannot be null.");
            return new ValidationResult(false, errors, warnings);
        }

        // Required road name
        if (dto.getReportedRoadName() == null || dto.getReportedRoadName().trim().isEmpty()) {
            errors.add("Field 'reportedRoadName' is required.");
        }

        // Required status
        if (dto.getReportedStatus() == null || dto.getReportedStatus().trim().isEmpty()) {
            errors.add("Field 'reportedStatus' is required.");
        }

        // Required source name
        if (dto.getSourceName() == null || dto.getSourceName().trim().isEmpty()) {
            errors.add("Field 'sourceName' is required.");
        }

        // URL security & format validation
        if (dto.getSourceUrl() != null && !dto.getSourceUrl().trim().isEmpty()) {
            String url = dto.getSourceUrl().trim();
            try {
                URI uri = URI.create(url);
                String scheme = uri.getScheme();
                if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                    errors.add("Source URL must use http or https protocol: " + url);
                }
            } catch (Exception e) {
                errors.add("Invalid source URL format: " + url);
            }
        }

        // Coordinate bounds
        if (dto.getLatitude() != null) {
            if (dto.getLatitude() < -90.0 || dto.getLatitude() > 90.0) {
                errors.add("Latitude " + dto.getLatitude() + " is outside valid range [-90, 90].");
            } else if (dto.getLatitude() < bboxMinLat || dto.getLatitude() > bboxMaxLat) {
                warnings.add("Latitude " + dto.getLatitude() + " falls outside Uttarakhand bounding box ("
                        + bboxMinLat + " to " + bboxMaxLat + ").");
            }
        }

        if (dto.getLongitude() != null) {
            if (dto.getLongitude() < -180.0 || dto.getLongitude() > 180.0) {
                errors.add("Longitude " + dto.getLongitude() + " is outside valid range [-180, 180].");
            } else if (dto.getLongitude() < bboxMinLon || dto.getLongitude() > bboxMaxLon) {
                warnings.add("Longitude " + dto.getLongitude() + " falls outside Uttarakhand bounding box ("
                        + bboxMinLon + " to " + bboxMaxLon + ").");
            }
        }

        // Timestamp check: observedAt cannot be in future by more than 1 hour
        if (dto.getObservedAt() != null) {
            Instant maxAllowedFuture = Instant.now().plus(1, ChronoUnit.HOURS);
            if (dto.getObservedAt().isAfter(maxAllowedFuture)) {
                warnings.add("ObservedAt timestamp " + dto.getObservedAt() + " is in the future.");
            }
        }

        boolean isValid = errors.isEmpty();
        if (!isValid) {
            log.warn("Validation failed for report on '{}': {}", dto.getReportedRoadName(), errors);
        }
        return new ValidationResult(isValid, errors, warnings);
    }
}
