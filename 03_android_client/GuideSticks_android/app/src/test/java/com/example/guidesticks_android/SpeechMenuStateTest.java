package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SpeechMenuStateTest {
    @Test
    public void enabledStateUsesSpeakerAndDisableAction() {
        assertEquals(R.drawable.ic_speaker_on, SpeechMenuState.iconRes(true));
        assertEquals(R.string.assistant_disable_auto_speak,
                SpeechMenuState.titleRes(true));
        assertEquals(R.string.auto_speak_enabled,
                SpeechMenuState.statusRes(true));
    }

    @Test
    public void disabledStateUsesMutedSpeakerAndEnableAction() {
        assertEquals(R.drawable.ic_stop_speaking, SpeechMenuState.iconRes(false));
        assertEquals(R.string.assistant_enable_auto_speak,
                SpeechMenuState.titleRes(false));
        assertEquals(R.string.auto_speak_disabled,
                SpeechMenuState.statusRes(false));
    }
}
