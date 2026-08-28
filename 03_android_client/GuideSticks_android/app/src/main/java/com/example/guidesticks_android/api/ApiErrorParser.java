package com.example.guidesticks_android.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Map;

import okhttp3.ResponseBody;

public final class ApiErrorParser {
    private ApiErrorParser() {
    }

    public static String parse(ResponseBody body, String fallback) {
        if (body == null) {
            return fallback;
        }
        try {
            return parse(body.string(), fallback);
        } catch (IOException ignored) {
            return fallback;
        }
    }

    public static String parse(String body, String fallback) {
        if (body == null || body.trim().isEmpty()) {
            return fallback;
        }
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject() && !root.isJsonArray()) {
                return fallback;
            }
            String message = firstMessage(root);
            return message == null || message.trim().isEmpty() ? fallback : message.trim();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String firstMessage(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return array.isEmpty() ? null : firstMessage(array.get(0));
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("detail")) {
            return firstMessage(object.get("detail"));
        }
        if (object.has("non_field_errors")) {
            return firstMessage(object.get("non_field_errors"));
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String value = firstMessage(entry.getValue());
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
