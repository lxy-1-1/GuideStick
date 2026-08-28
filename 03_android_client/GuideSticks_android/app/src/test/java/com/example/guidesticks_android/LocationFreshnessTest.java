package com.example.guidesticks_android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocationFreshnessTest {
    @Test
    public void acceptsRecentLocationAndRejectsStaleOrFutureLocation() {
        long now = 1_000_000L;

        assertTrue(LocationFreshness.isFresh(now - 30_000L, now));
        assertFalse(LocationFreshness.isFresh(now - 600_000L, now));
        assertFalse(LocationFreshness.isFresh(now + 60_000L, now));
    }
}
