package com.example.guidesticks_android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmergencyMessageBuilderTest {
    @Test
    public void withLocationIncludesCoordinatesAccuracyAndMapLink() {
        String message = EmergencyMessageBuilder.withLocation(
                30.274085, 120.155070, 18.6f);

        assertTrue(message.contains("智途紧急求助"));
        assertTrue(message.contains("手机最近位置"));
        assertTrue(message.contains("30.274085,120.155070"));
        assertTrue(message.contains("精度约19米"));
        assertTrue(message.contains(
                "https://maps.google.com/?q=30.274085,120.155070"));
        assertFalse(message.contains("无法获取定位"));
    }

    @Test
    public void withoutLocationClearlyStatesLocationIsUnavailable() {
        String message = EmergencyMessageBuilder.withoutLocation();

        assertTrue(message.contains("智途紧急求助"));
        assertTrue(message.contains("无法获取定位"));
        assertTrue(message.contains("请尽快联系我"));
        assertFalse(message.contains("maps.google.com"));
    }
}
