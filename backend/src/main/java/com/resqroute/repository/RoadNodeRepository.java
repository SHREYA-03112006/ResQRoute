package com.resqroute.repository;

import com.resqroute.model.RoadNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadNodeRepository extends JpaRepository<RoadNode, Long> {

    Optional<RoadNode> findByOsmNodeId(Long osmNodeId);

    /** Find the nearest graph node to the given coordinates. */
    @Query("SELECT n FROM RoadNode n ORDER BY " +
           "((n.latitude - :lat) * (n.latitude - :lat) + " +
           "(n.longitude - :lon) * (n.longitude - :lon)) ASC " +
           "LIMIT 1")
    Optional<RoadNode> findNearestNode(Double lat, Double lon);

    @Query("SELECT n FROM RoadNode n WHERE n.name IS NOT NULL ORDER BY n.name")
    List<RoadNode> findNamedNodes();
}
