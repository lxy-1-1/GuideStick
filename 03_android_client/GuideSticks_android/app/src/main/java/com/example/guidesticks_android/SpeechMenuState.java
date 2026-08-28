package com.example.guidesticks_android;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public final class SpeechMenuState {
    private SpeechMenuState() {
    }

    @DrawableRes
    public static int iconRes(boolean enabled) {
        return enabled ? R.drawable.ic_speaker_on : R.drawable.ic_stop_speaking;
    }

    @StringRes
    public static int titleRes(boolean enabled) {
        return enabled
                ? R.string.assistant_disable_auto_speak
                : R.string.assistant_enable_auto_speak;
    }

    @StringRes
    public static int statusRes(boolean enabled) {
        return enabled ? R.string.auto_speak_enabled : R.string.auto_speak_disabled;
    }
}
