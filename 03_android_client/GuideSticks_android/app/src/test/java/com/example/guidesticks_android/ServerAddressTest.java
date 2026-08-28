package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServerAddressTest {
    @Test
    public void normalizeAddsSchemeAndTrailingSlash() {
        assertEquals("http://192.168.1.8:8000/", ServerAddress.normalize("192.168.1.8:8000"));
    }

    @Test
    public void normalizePreservesHttpsAndTrimsWhitespace() {
        assertEquals("https://example.com/api/", ServerAddress.normalize("  https://example.com/api  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeRejectsUnsupportedScheme() {
        ServerAddress.normalize("ftp://example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeRejectsMissingHost() {
        ServerAddress.normalize("http://");
    }
}
