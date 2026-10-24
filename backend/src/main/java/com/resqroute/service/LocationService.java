package com.resqroute.service;

import com.resqroute.model.Location;
import com.resqroute.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing depot and relief-camp locations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Location> getLocationsByType(String type) {
        return locationRepository.findByType(type.toUpperCase());
    }

    @Transactional(readOnly = true)
    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }

    @Transactional
    public Location createLocation(Location location) {
        location.setType(location.getType().toUpperCase());
        log.info("Creating location: {} ({})", location.getName(), location.getType());
        return locationRepository.save(location);
    }

    @Transactional
    public Optional<Location> updateLocation(Long id, Location updated) {
        return locationRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setType(updated.getType().toUpperCase());
            existing.setLatitude(updated.getLatitude());
            existing.setLongitude(updated.getLongitude());
            existing.setAddress(updated.getAddress());
            existing.setNotes(updated.getNotes());
            log.info("Updated location id={}", id);
            return locationRepository.save(existing);
        });
    }

    @Transactional
    public boolean deleteLocation(Long id) {
        if (locationRepository.existsById(id)) {
            locationRepository.deleteById(id);
            log.info("Deleted location id={}", id);
            return true;
        }
        return false;
    }
}
