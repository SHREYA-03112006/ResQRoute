package com.resqroute.model;

/**
 * Possible operational statuses of a road segment in the ResQ-Route system.
 *
 * <p>These values drive both routing cost calculations and dashboard display.
 */
public enum RoadStatus {

    /** Road is passable under normal conditions. */
    OPEN,

    /** Road is flooded; passable but with a high time-cost penalty. */
    FLOODED,

    /** Road is confirmed blocked; excluded from standard route calculations. */
    BLOCKED,

    /**
     * Report exists but cannot be independently verified.
     * The road is treated conservatively (high penalty) and flagged for dispatcher review.
     */
    UNCERTAIN,

    /** Status unknown — no reports available. Treated as open. */
    UNKNOWN
}
