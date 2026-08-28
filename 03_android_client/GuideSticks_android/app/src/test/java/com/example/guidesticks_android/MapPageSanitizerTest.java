package com.example.guidesticks_android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MapPageSanitizerTest {
    @Test
    public void amapPresentationScriptRemovesPageChromeAndKeepsMapControls() {
        String script = MapPageSanitizer.script();

        assertTrue(script.contains(".common_top"));
        assertTrue(script.contains(".common_bottom"));
        assertTrue(script.contains(".call_native_button_wrapper"));
        assertTrue(script.contains("#wx_open_app_button_goSearch"));
        assertTrue(script.contains(".map_locate"));
        assertTrue(script.contains("display:none!important"));
        assertTrue(script.contains("#map"));
        assertTrue(script.contains("height:100vh!important"));
        assertFalse(script.contains(".map_zoom{display:none"));
    }

    @Test
    public void sanitizerRunsOnlyOnSecureAmapPages() {
        assertTrue(MapPageSanitizer.supports("https", "uri.amap.com"));
        assertTrue(MapPageSanitizer.supports("HTTPS", "m.amap.com"));
        assertFalse(MapPageSanitizer.supports("http", "uri.amap.com"));
        assertFalse(MapPageSanitizer.supports("https", "amap.com.example.com"));
        assertFalse(MapPageSanitizer.supports("https", "openstreetmap.org"));
    }
}
