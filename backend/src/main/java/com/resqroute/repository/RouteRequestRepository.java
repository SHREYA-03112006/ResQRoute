package com.resqroute.repository;

import com.resqroute.model.RouteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRequestRepository extends JpaRepository<RouteRequest, Long> {

    List<RouteRequest> findByApprovalStatusOrderByCreatedAtDesc(String approvalStatus);

    List<RouteRequest> findTop10ByOrderByCreatedAtDesc();
}
