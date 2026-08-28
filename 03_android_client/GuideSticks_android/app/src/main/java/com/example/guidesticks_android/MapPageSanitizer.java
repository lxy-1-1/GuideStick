package com.example.guidesticks_android;

import java.util.Locale;

final class MapPageSanitizer {
    private static final String SCRIPT = "(function(){"
            + "var id='guidesticks-map-style';"
            + "if(!document.getElementById(id)){"
            + "var style=document.createElement('style');"
            + "style.id=id;"
            + "style.textContent='"
            + "html,body{width:100%!important;height:100%!important;margin:0!important;"
            + "overflow:hidden!important;background:#e9f1ef!important;}"
            + ".common_top,.common_bottom,.call_native_button_wrapper,"
            + "#wx_open_app_button_goSearch,.map_download,.rodeinfo_and_satellite,"
            + ".map_locate{display:none!important;}"
            + ".common_center,.map_all,.common_content_box,#map{top:0!important;"
            + "bottom:0!important;width:100%!important;height:100vh!important;}"
            + ".map_zoom{right:12px!important;top:50%!important;"
            + "transform:translateY(-50%)!important;}';"
            + "(document.head||document.documentElement).appendChild(style);"
            + "}"
            + "var mapElement=document.getElementById('map');"
            + "if(mapElement){mapElement.setAttribute('aria-label','当前位置地图');}"
            + "window.dispatchEvent(new Event('resize'));"
            + "setTimeout(function(){window.dispatchEvent(new Event('resize'));},200);"
            + "return 'ready';"
            + "})()";

    private MapPageSanitizer() {
    }

    static boolean supports(String scheme, String host) {
        if (!"https".equalsIgnoreCase(scheme) || host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.US);
        return normalizedHost.equals("amap.com") || normalizedHost.endsWith(".amap.com");
    }

    static String script() {
        return SCRIPT;
    }
}
