package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DeviceStatusPresentationTest {
    @Test
    public void mapsOnlineOfflineAndUnavailableToDistinctUiStates() {
        assertEquals(R.string.device_online,
                DeviceStatusPresentation.statusRes(Boolean.TRUE));
        assertEquals(R.string.device_offline,
                DeviceStatusPresentation.statusRes(Boolean.FALSE));
        assertEquals(R.string.device_unavailable,
                DeviceStatusPresentation.statusRes(null));

        assertEquals(R.drawable.bg_status_success,
                DeviceStatusPresentation.backgroundRes(Boolean.TRUE));
        assertEquals(R.drawable.bg_status_warning,
                DeviceStatusPresentation.backgroundRes(Boolean.FALSE));
        assertEquals(R.drawable.bg_status_neutral,
                DeviceStatusPresentation.backgroundRes(null));
    }
}
