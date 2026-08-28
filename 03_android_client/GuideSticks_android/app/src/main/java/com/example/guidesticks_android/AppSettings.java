package com.example.guidesticks_android;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    public static final String DEFAULT_SERVER_URL = "http://172.20.10.3:8000/";

    private static final String PREFS_NAME = "GuideSticksSettings";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_LARGE_TEXT = "large_text";
    private static final String KEY_AUTO_SPEAK = "auto_speak";
    private static final String KEY_HAPTICS = "haptics";
    private static final String KEY_LAST_LIGHT = "last_light";
    private static final String KEY_LAST_COMMAND = "last_command";
    private static final String KEY_LAST_MESSAGE = "last_message";
    private static final String KEY_LAST_RECOGNITION_TIME = "last_recognition_time";

    private final SharedPreferences preferences;

    public AppSettings(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getServerUrl() {
        String stored = preferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        try {
            return ServerAddress.normalize(stored);
        } catch (IllegalArgumentException ignored) {
            return DEFAULT_SERVER_URL;
        }
    }

    public void setServerUrl(String serverUrl) {
        preferences.edit().putString(KEY_SERVER_URL, ServerAddress.normalize(serverUrl)).apply();
    }

    public boolean isLargeTextEnabled() {
        return preferences.getBoolean(KEY_LARGE_TEXT, false);
    }

    public void setLargeTextEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply();
    }

    public boolean isAutoSpeakEnabled() {
        return preferences.getBoolean(KEY_AUTO_SPEAK, true);
    }

    public void setAutoSpeakEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply();
    }

    public boolean isHapticsEnabled() {
        return preferences.getBoolean(KEY_HAPTICS, true);
    }

    public void setHapticsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_HAPTICS, enabled).apply();
    }

    public void saveLastRecognition(String light, String command, String message) {
        preferences.edit()
                .putString(KEY_LAST_LIGHT, valueOrEmpty(light))
                .putString(KEY_LAST_COMMAND, valueOrEmpty(command))
                .putString(KEY_LAST_MESSAGE, valueOrEmpty(message))
                .putLong(KEY_LAST_RECOGNITION_TIME, System.currentTimeMillis())
                .apply();
    }

    public String getLastLight() {
        return preferences.getString(KEY_LAST_LIGHT, "");
    }

    public String getLastCommand() {
        return preferences.getString(KEY_LAST_COMMAND, "");
    }

    public String getLastMessage() {
        return preferences.getString(KEY_LAST_MESSAGE, "");
    }

    public long getLastRecognitionTime() {
        return preferences.getLong(KEY_LAST_RECOGNITION_TIME, 0L);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
