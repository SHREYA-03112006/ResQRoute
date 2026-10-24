package com.resqroute.routing;

/**
 * Utility for geographic distance calculations using the Haversine formula.
 */
public final class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtil() {}

    /**
     * Calculates the great-circle distance between two points on the Earth in kilometres.
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Admissible heuristic for A*: haversine distance divided by maximum assumed vehicle speed.
     * In mountain terrain, 60 km/h is 1 km/min.
     * This guarantees the heuristic is admissible (never overestimates travel time).
     */
    public static double heuristicMinutes(double lat1, double lon1, double lat2, double lon2) {
        double distKm = distanceKm(lat1, lon1, lat2, lon2);
        double maxSpeedKmPerHour = 60.0;
        return (distKm / maxSpeedKmPerHour) * 60.0;
    }
}
