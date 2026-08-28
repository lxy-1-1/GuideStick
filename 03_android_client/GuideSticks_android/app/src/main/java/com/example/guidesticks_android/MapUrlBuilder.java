package com.example.guidesticks_android;

import java.util.Locale;

public final class MapUrlBuilder {
    private static final double MAP_SPAN = 0.003;
    private static final double EARTH_SEMI_MAJOR_AXIS = 6378245.0;
    private static final double ECCENTRICITY_SQUARED = 0.00669342162296594323;
    private static final String CURRENT_LOCATION_NAME =
            "%E5%BD%93%E5%89%8D%E4%BD%8D%E7%BD%AE";
    private static final double[][] MAINLAND_CHINA_BORDER = {
            {73.5, 39.0}, {78.5, 35.0}, {80.0, 31.0}, {84.5, 28.5},
            {89.0, 27.5}, {95.0, 28.0}, {98.5, 24.0}, {101.0, 21.5},
            {106.7, 21.7}, {108.0, 20.2}, {112.0, 18.0}, {113.5, 22.0},
            {117.0, 23.0}, {121.0, 27.0}, {122.0, 31.0}, {124.5, 39.0},
            {131.5, 43.0}, {134.8, 48.5}, {130.0, 53.5}, {120.0, 53.5},
            {115.0, 48.0}, {109.0, 42.5}, {100.0, 42.0}, {96.0, 45.0},
            {91.0, 46.5}, {87.0, 49.0}, {82.0, 47.0}, {79.0, 42.0}
    };

    private MapUrlBuilder() {
    }

    public static String embed(double latitude, double longitude) {
        if (isInMainlandChina(latitude, longitude)) {
            return amapMarker(latitude, longitude);
        }
        return osmEmbed(latitude, longitude);
    }

    public static String browser(double latitude, double longitude) {
        if (isInMainlandChina(latitude, longitude)) {
            return amapMarker(latitude, longitude);
        }
        return osmBrowser(latitude, longitude);
    }

    public static boolean isAllowedMapUri(String scheme, String host) {
        if (!"https".equalsIgnoreCase(scheme) || host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.US);
        return normalizedHost.equals("amap.com")
                || normalizedHost.endsWith(".amap.com")
                || normalizedHost.equals("openstreetmap.org")
                || normalizedHost.endsWith(".openstreetmap.org");
    }

    private static String amapMarker(double latitude, double longitude) {
        Coordinate converted = wgs84ToGcj02(latitude, longitude);
        return String.format(
                Locale.US,
                "https://uri.amap.com/marker?position=%.6f,%.6f"
                        + "&name=%s&src=GuideSticks&callnative=0",
                converted.longitude, converted.latitude, CURRENT_LOCATION_NAME);
    }

    private static String osmEmbed(double latitude, double longitude) {
        double west = longitude - MAP_SPAN;
        double south = latitude - MAP_SPAN;
        double east = longitude + MAP_SPAN;
        double north = latitude + MAP_SPAN;
        return String.format(
                Locale.US,
                "https://www.openstreetmap.org/export/embed.html?"
                        + "bbox=%.6f%%2C%.6f%%2C%.6f%%2C%.6f"
                        + "&layer=mapnik&marker=%.6f%%2C%.6f",
                west, south, east, north, latitude, longitude);
    }

    private static String osmBrowser(double latitude, double longitude) {
        return String.format(
                Locale.US,
                "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f"
                        + "#map=17/%.6f/%.6f",
                latitude, longitude, latitude, longitude);
    }

    private static boolean isInMainlandChina(double latitude, double longitude) {
        if (longitude < 73.5 || longitude > 134.8
                || latitude < 18.0 || latitude > 53.5) {
            return false;
        }
        boolean inside = false;
        for (int current = 0, previous = MAINLAND_CHINA_BORDER.length - 1;
             current < MAINLAND_CHINA_BORDER.length;
             previous = current++) {
            double currentLongitude = MAINLAND_CHINA_BORDER[current][0];
            double currentLatitude = MAINLAND_CHINA_BORDER[current][1];
            double previousLongitude = MAINLAND_CHINA_BORDER[previous][0];
            double previousLatitude = MAINLAND_CHINA_BORDER[previous][1];
            boolean crossesLatitude = (currentLatitude > latitude)
                    != (previousLatitude > latitude);
            if (crossesLatitude) {
                double borderLongitude = (previousLongitude - currentLongitude)
                        * (latitude - currentLatitude)
                        / (previousLatitude - currentLatitude)
                        + currentLongitude;
                if (longitude < borderLongitude) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static Coordinate wgs84ToGcj02(double latitude, double longitude) {
        double latitudeOffset = transformLatitude(longitude - 105.0, latitude - 35.0);
        double longitudeOffset = transformLongitude(longitude - 105.0, latitude - 35.0);
        double latitudeRadians = Math.toRadians(latitude);
        double sinLatitude = Math.sin(latitudeRadians);
        double magic = 1.0 - ECCENTRICITY_SQUARED * sinLatitude * sinLatitude;
        double squareRootMagic = Math.sqrt(magic);
        latitudeOffset = latitudeOffset * 180.0
                / ((EARTH_SEMI_MAJOR_AXIS * (1.0 - ECCENTRICITY_SQUARED))
                / (magic * squareRootMagic) * Math.PI);
        longitudeOffset = longitudeOffset * 180.0
                / (EARTH_SEMI_MAJOR_AXIS / squareRootMagic
                * Math.cos(latitudeRadians) * Math.PI);
        return new Coordinate(latitude + latitudeOffset, longitude + longitudeOffset);
    }

    private static double transformLatitude(double x, double y) {
        double result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y
                + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        result += (20.0 * Math.sin(6.0 * x * Math.PI)
                + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        result += (20.0 * Math.sin(y * Math.PI)
                + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        result += (160.0 * Math.sin(y / 12.0 * Math.PI)
                + 320.0 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return result;
    }

    private static double transformLongitude(double x, double y) {
        double result = 300.0 + x + 2.0 * y + 0.1 * x * x
                + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        result += (20.0 * Math.sin(6.0 * x * Math.PI)
                + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        result += (20.0 * Math.sin(x * Math.PI)
                + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        result += (150.0 * Math.sin(x / 12.0 * Math.PI)
                + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return result;
    }

    private static final class Coordinate {
        private final double latitude;
        private final double longitude;

        private Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
