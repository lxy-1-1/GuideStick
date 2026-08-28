package com.example.guidesticks_android.model;

import com.google.gson.annotations.SerializedName;

public class DeviceStatusResponse {
    private boolean online;

    @SerializedName("last_seen")
    private String lastSeen;

    @SerializedName("last_seen_epoch_ms")
    private Long lastSeenEpochMs;

    @SerializedName("age_seconds")
    private Long ageSeconds;

    @SerializedName("timeout_seconds")
    private int timeoutSeconds;

    public boolean isOnline() {
        return online;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public Long getLastSeenEpochMs() {
        return lastSeenEpochMs;
    }

    public Long getAgeSeconds() {
        return ageSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
