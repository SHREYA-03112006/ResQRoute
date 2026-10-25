package com.resqroute.repository;

import com.resqroute.model.DisasterCategory;
import com.resqroute.model.RoadReport;
import com.resqroute.model.RoadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RoadReportRepository extends JpaRepository<RoadReport, Long> {

    List<RoadReport> findByFlaggedForReviewTrue();

    List<RoadReport> findByReportedStatus(RoadStatus status);

    List<RoadReport> findByMatchedEdgeId(Long edgeId);

    List<RoadReport> findByCityIgnoreCase(String city);

    List<RoadReport> findByDisasterCategory(DisasterCategory category);

    List<RoadReport> findByCityIgnoreCaseAndDisasterCategory(String city, DisasterCategory category);

    List<RoadReport> findBySourceUrl(String sourceUrl);

    @Query("SELECT r FROM RoadReport r WHERE r.retrievedAt > :since ORDER BY r.retrievedAt DESC")
    List<RoadReport> findRecentReports(Instant since);

    @Query("SELECT r FROM RoadReport r WHERE r.confidenceScore >= :threshold AND r.reportedStatus != 'OPEN'")
    List<RoadReport> findHighConfidenceNonOpenReports(Double threshold);
}
