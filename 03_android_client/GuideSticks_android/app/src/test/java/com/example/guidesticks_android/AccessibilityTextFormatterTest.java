package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AccessibilityTextFormatterTest {
    @Test
    public void locationIncludesAddressAccuracyAndTime() {
        assertEquals(
                "当前位置，人民路 1 号。精度约 5 米。更新于 10:30。",
                AccessibilityTextFormatter.location(
                        "人民路 1 号", "精度约 5 米", "更新于 10:30")
        );
    }

    @Test
    public void locationSkipsBlankOptionalDetails() {
        assertEquals(
                "当前位置，暂时无法获取详细地址。",
                AccessibilityTextFormatter.location(
                        "暂时无法获取详细地址", "", null)
        );
    }

    @Test
    public void recognitionIncludesOptionalCommand() {
        assertEquals(
                "识别结果，前方为红灯。安全指令，停止前进。",
                AccessibilityTextFormatter.recognition("前方为红灯", "停止前进")
        );
    }

    @Test
    public void recognitionSkipsBlankCommand() {
        assertEquals(
                "识别结果，暂时无法判断。",
                AccessibilityTextFormatter.recognition("暂时无法判断", " ")
        );
    }

    @Test
    public void serverStatusProducesCompleteSentence() {
        assertEquals(
                "服务器状态，服务在线。",
                AccessibilityTextFormatter.serverStatus("服务在线")
        );
    }
}
