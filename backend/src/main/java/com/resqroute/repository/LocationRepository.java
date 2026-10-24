package com.resqroute.repository;

import com.resqroute.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByType(String type);

    List<Location> findByNameContainingIgnoreCase(String name);
}
