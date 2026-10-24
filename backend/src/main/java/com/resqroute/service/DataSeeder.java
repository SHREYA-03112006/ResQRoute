package com.resqroute.service;

import com.resqroute.model.Location;
import com.resqroute.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the database with initial Chamoli/Rudraprayag depot and relief-camp locations
 * when the application starts and the locations table is empty.
 *
 * <p>All coordinates are approximate and based on publicly available geographic information
 * for the Uttarakhand pilot district. They serve as starting-point examples for Phase 1.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final LocationRepository locationRepository;
    private final RoadReportService roadReportService;
    private final RoadNetworkSeeder roadNetworkSeeder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedLocations();
        roadReportService.loadSampleReportsIfEmpty();
        roadNetworkSeeder.seedRoadNetworkIfEmpty();
    }

    private void seedLocations() {
        if (locationRepository.count() > 0) {
            log.debug("Locations already seeded, skipping.");
            return;
        }

        log.info("Seeding initial locations for Chamoli/Rudraprayag district.");

        List<Location> locations = List.of(
            // Depots
            Location.builder()
                .name("Rudraprayag Relief Depot")
                .type("DEPOT")
                .latitude(30.285).longitude(78.981)
                .address("Rudraprayag, Uttarakhand")
                .notes("Primary SDRF staging point")
                .build(),
            Location.builder()
                .name("Chamoli District Depot")
                .type("DEPOT")
                .latitude(30.396).longitude(79.321)
                .address("Gopeshwar, Chamoli, Uttarakhand")
                .notes("Secondary staging point near Badrinath Highway")
                .build(),
            Location.builder()
                .name("Srinagar (Garhwal) Collection Point")
                .type("DEPOT")
                .latitude(30.224).longitude(78.791)
                .address("Srinagar, Pauri Garhwal, Uttarakhand")
                .notes("NH58 road access; helicopter landing feasible")
                .build(),

            // Relief camps
            Location.builder()
                .name("Gaurikund Relief Camp")
                .type("RELIEF_CAMP")
                .latitude(30.645).longitude(79.266)
                .address("Gaurikund, Rudraprayag, Uttarakhand")
                .notes("Kedarnath route base; capacity ~500")
                .build(),
            Location.builder()
                .name("Joshimath Relief Camp")
                .type("RELIEF_CAMP")
                .latitude(30.556).longitude(79.567)
                .address("Joshimath, Chamoli, Uttarakhand")
                .notes("Badrinath valley gateway; capacity ~800")
                .build(),
            Location.builder()
                .name("Pipalkoti Community Shelter")
                .type("RELIEF_CAMP")
                .latitude(30.473).longitude(79.479)
                .address("Pipalkoti, Chamoli, Uttarakhand")
                .notes("Capacity ~300; medical post present")
                .build(),
            Location.builder()
                .name("Tilwara Flood Relief Camp")
                .type("RELIEF_CAMP")
                .latitude(30.292).longitude(78.962)
                .address("Tilwara, Rudraprayag, Uttarakhand")
                .notes("Riverside location; monitor water levels")
                .build(),

            // Hospitals
            Location.builder()
                .name("Rudraprayag District Hospital")
                .type("HOSPITAL")
                .latitude(30.284).longitude(78.980)
                .address("Rudraprayag town, Uttarakhand")
                .notes("24 h emergency; 150-bed capacity")
                .build(),
            Location.builder()
                .name("Gopeshwar Community Health Centre")
                .type("HOSPITAL")
                .latitude(30.393).longitude(79.317)
                .address("Gopeshwar, Chamoli, Uttarakhand")
                .notes("Referral centre for Chamoli district")
                .build()
        );

        locationRepository.saveAll(locations);
        log.info("Seeded {} locations.", locations.size());
    }
}
