package com.example.guidesticks_android;

public final class AccessibilityTextFormatter {
    private AccessibilityTextFormatter() {
    }

    public static String location(String address, String accuracy, String updated) {
        return join("当前位置", address, accuracy, updated);
    }

    public static String recognition(String message, String command) {
        String result = join("识别结果", message);
        if (isBlank(command)) {
            return result;
        }
        return result + "安全指令，" + sentence(command);
    }

    public static String serverStatus(String status) {
        return join("服务器状态", status);
    }

    private static String join(String prefix, String... values) {
        StringBuilder result = new StringBuilder(prefix);
        boolean hasValue = false;
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            result.append(hasValue ? "" : "，").append(sentence(value));
            hasValue = true;
        }
        if (!hasValue) {
            result.append("。");
        }
        return result.toString();
    }

    private static String sentence(String value) {
        String normalized = value.trim();
        char last = normalized.charAt(normalized.length() - 1);
        if (last == '。' || last == '！' || last == '？'
                || last == '!' || last == '?') {
            return normalized;
        }
        return normalized + "。";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
