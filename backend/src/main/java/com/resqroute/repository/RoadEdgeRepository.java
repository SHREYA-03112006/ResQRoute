package com.resqroute.repository;

import com.resqroute.model.RoadEdge;
import com.resqroute.model.RoadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadEdgeRepository extends JpaRepository<RoadEdge, Long> {

    /** Find all edges originating from a given node. */
    @Query("SELECT e FROM RoadEdge e JOIN FETCH e.fromNode JOIN FETCH e.toNode WHERE e.fromNode.id = :nodeId")
    List<RoadEdge> findByFromNodeId(Long nodeId);

    /** Find all edges ending at a given node (for bidirectional lookup). */
    @Query("SELECT e FROM RoadEdge e JOIN FETCH e.fromNode JOIN FETCH e.toNode WHERE e.toNode.id = :nodeId")
    List<RoadEdge> findByToNodeId(Long nodeId);

    /** Get all edges with their nodes loaded (for building the in-memory graph). */
    @Query("SELECT e FROM RoadEdge e JOIN FETCH e.fromNode JOIN FETCH e.toNode")
    List<RoadEdge> findAllWithNodes();

    /** Find edges by road status. */
    List<RoadEdge> findByStatus(RoadStatus status);

    /** Find edges by road name (case-insensitive partial match). */
    @Query("SELECT e FROM RoadEdge e WHERE LOWER(e.roadName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<RoadEdge> findByRoadNameContaining(String name);
}
