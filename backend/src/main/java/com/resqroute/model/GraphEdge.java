package com.resqroute.model;

/**
 * Represents a directed or bi-directional road segment in the pilot district road graph.
 */
public class GraphEdge {

    private Long id;
    private String roadName;
    private String roadCode; // e.g. NH-7, NH-107, SH-64
    private String fromNode;
    private String toNode;
    private double fromLat;
    private double fromLon;
    private double toLat;
    private double toLon;
    private double distanceKm;
    private double baseSpeedKmh;
    private double normalCostMinutes;
    private double currentCostMinutes;
    private RoadStatus status;
    private Long activeReportId;

    public GraphEdge() {}

    public GraphEdge(Long id, String roadName, String roadCode, String fromNode, String toNode,
                     double fromLat, double fromLon, double toLat, double toLon,
                     double distanceKm, double baseSpeedKmh) {
        this.id = id;
        this.roadName = roadName;
        this.roadCode = roadCode;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.fromLat = fromLat;
        this.fromLon = fromLon;
        this.toLat = toLat;
        this.toLon = toLon;
        this.distanceKm = distanceKm;
        this.baseSpeedKmh = baseSpeedKmh;
        this.normalCostMinutes = (distanceKm / baseSpeedKmh) * 60.0;
        this.currentCostMinutes = this.normalCostMinutes;
        this.status = RoadStatus.OPEN;
    }

    public double getMidLat() {
        return (fromLat + toLat) / 2.0;
    }

    public double getMidLon() {
        return (fromLon + toLon) / 2.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }

    public String getRoadCode() { return roadCode; }
    public void setRoadCode(String roadCode) { this.roadCode = roadCode; }

    public String getFromNode() { return fromNode; }
    public void setFromNode(String fromNode) { this.fromNode = fromNode; }

    public String getToNode() { return toNode; }
    public void setToNode(String toNode) { this.toNode = toNode; }

    public double getFromLat() { return fromLat; }
    public void setFromLat(double fromLat) { this.fromLat = fromLat; }

    public double getFromLon() { return fromLon; }
    public void setFromLon(double fromLon) { this.fromLon = fromLon; }

    public double getToLat() { return toLat; }
    public void setToLat(double toLat) { this.toLat = toLat; }

    public double getToLon() { return toLon; }
    public void setToLon(double toLon) { this.toLon = toLon; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public double getBaseSpeedKmh() { return baseSpeedKmh; }
    public void setBaseSpeedKmh(double baseSpeedKmh) { this.baseSpeedKmh = baseSpeedKmh; }

    public double getNormalCostMinutes() { return normalCostMinutes; }
    public void setNormalCostMinutes(double normalCostMinutes) { this.normalCostMinutes = normalCostMinutes; }

    public double getCurrentCostMinutes() { return currentCostMinutes; }
    public void setCurrentCostMinutes(double currentCostMinutes) { this.currentCostMinutes = currentCostMinutes; }

    public RoadStatus getStatus() { return status; }
    public void setStatus(RoadStatus status) { this.status = status; }

    public Long getActiveReportId() { return activeReportId; }
    public void setActiveReportId(Long activeReportId) { this.activeReportId = activeReportId; }
}
