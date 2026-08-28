package com.example.guidesticks_android;

final class DeviceStatusPresentation {
    private DeviceStatusPresentation() {
    }

    static int statusRes(Boolean online) {
        if (online == null) {
            return R.string.device_unavailable;
        }
        return online ? R.string.device_online : R.string.device_offline;
    }

    static int backgroundRes(Boolean online) {
        if (online == null) {
            return R.drawable.bg_status_neutral;
        }
        return online ? R.drawable.bg_status_success : R.drawable.bg_status_warning;
    }
}
