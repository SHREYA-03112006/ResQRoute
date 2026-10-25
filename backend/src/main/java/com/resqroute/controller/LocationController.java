package com.resqroute.controller;

import com.resqroute.model.Location;
import com.resqroute.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing depot and relief-camp locations.
 *
 * <p>Base path: {@code /api/locations}
 */
@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /** GET /api/locations — list all locations, optionally filtered by type. */
    @GetMapping
    public ResponseEntity<List<Location>> getLocations(
            @RequestParam(required = false) String type) {
        List<Location> result = (type != null)
                ? locationService.getLocationsByType(type)
                : locationService.getAllLocations();
        return ResponseEntity.ok(result);
    }

    /** GET /api/locations/{id} — get a single location by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocation(@PathVariable Long id) {
        return locationService.getLocationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/locations — create a new location. */
    @PostMapping
    public ResponseEntity<Location> createLocation(@Valid @RequestBody Location location) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createLocation(location));
    }

    /** PUT /api/locations/{id} — update an existing location. */
    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(
            @PathVariable Long id, @Valid @RequestBody Location location) {
        return locationService.updateLocation(id, location)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/locations/{id} — delete a location. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteLocation(@PathVariable Long id) {
        boolean deleted = locationService.deleteLocation(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Location deleted."));
        }
        return ResponseEntity.notFound().build();
    }
}
