package com.example.guidesticks_android;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class ServerAddress {
    private ServerAddress() {
    }

    public static String normalize(String rawAddress) {
        String value = rawAddress == null ? "" : rawAddress.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("服务器地址不能为空");
        }
        if (!value.contains("://")) {
            value = "http://" + value;
        }

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null
                    ? ""
                    : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("服务器地址只支持 HTTP 或 HTTPS");
            }
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new IllegalArgumentException("服务器地址缺少主机名或 IP");
            }
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("服务器地址格式不正确");
            }
            String normalized = uri.toString();
            return normalized.endsWith("/") ? normalized : normalized + "/";
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("服务器地址格式不正确", error);
        }
    }
}
