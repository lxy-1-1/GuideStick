package com.example.guidesticks_android;

final class LocationFreshness {
    private static final long MAX_AGE_MS = 5 * 60 * 1000L;
    private static final long MAX_FUTURE_MS = 30 * 1000L;

    private LocationFreshness() {
    }

    static boolean isFresh(long capturedAtMs, long nowMs) {
        long ageMs = nowMs - capturedAtMs;
        return ageMs >= -MAX_FUTURE_MS && ageMs <= MAX_AGE_MS;
    }
}
