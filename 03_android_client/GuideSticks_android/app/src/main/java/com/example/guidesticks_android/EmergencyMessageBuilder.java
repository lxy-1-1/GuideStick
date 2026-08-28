package com.example.guidesticks_android;

import java.util.Locale;

public final class EmergencyMessageBuilder {
    private EmergencyMessageBuilder() {
    }

    public static String withLocation(double latitude, double longitude,
                                      float accuracyMeters) {
        String coordinates = String.format(
                Locale.US, "%.6f,%.6f", latitude, longitude);
        return "【智途紧急求助】我可能需要帮助，请尽快联系我。"
                + "手机最近位置：" + coordinates
                + "（精度约" + Math.round(accuracyMeters) + "米）。"
                + "地图：https://maps.google.com/?q=" + coordinates;
    }

    public static String withoutLocation() {
        return "【智途紧急求助】我可能需要帮助，请尽快联系我。"
                + "手机当前无法获取定位。";
    }
}
