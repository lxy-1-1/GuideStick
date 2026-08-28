package com.example.guidesticks_android.model;

import com.google.gson.annotations.SerializedName;

public class SensorContextRequest {
    private final double latitude;
    private final double longitude;
    private final float accuracy;
    private final String address;

    @SerializedName("captured_at_ms")
    private final long capturedAtMs;

    public SensorContextRequest(double latitude,
                                double longitude,
                                float accuracy,
                                String address,
                                long capturedAtMs) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.address = address == null ? "" : address;
        this.capturedAtMs = capturedAtMs;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getAddress() {
        return address;
    }

    public long getCapturedAtMs() {
        return capturedAtMs;
    }
}
