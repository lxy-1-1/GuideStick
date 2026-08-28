package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MapUrlBuilderTest {
    @Test
    public void embedUsesAmapForMainlandChinaAndConvertsGpsCoordinates() {
        String url = MapUrlBuilder.embed(34.811895, 113.499129);

        assertTrue(url.startsWith(
                "https://uri.amap.com/marker?"));
        assertTrue(url.contains("position=113.505438,34.810949"));
        assertTrue(url.contains("name=%E5%BD%93%E5%89%8D%E4%BD%8D%E7%BD%AE"));
        assertTrue(url.contains("callnative=0"));
    }

    @Test
    public void browserUsesAmapForMainlandChina() {
        String url = MapUrlBuilder.browser(34.811895, 113.499129);

        assertTrue(url.startsWith("https://uri.amap.com/marker?"));
        assertTrue(url.contains("position=113.505438,34.810949"));
    }

    @Test
    public void embedKeepsOsmForLocationsOutsideMainlandChina() {
        String url = MapUrlBuilder.embed(40.782613, -73.965280);

        assertTrue(url.startsWith(
                "https://www.openstreetmap.org/export/embed.html?"));
        assertTrue(url.contains("marker=40.782613%2C-73.965280"));
    }

    @Test
    public void browserKeepsOsmForLocationsOutsideMainlandChina() {
        assertEquals(
                "https://www.openstreetmap.org/?mlat=40.782613&mlon=-73.965280"
                        + "#map=17/40.782613/-73.965280",
                MapUrlBuilder.browser(40.782613, -73.965280));
    }

    @Test
    public void nearbyAsianLocationsOutsideChinaKeepOsm() {
        assertTrue(MapUrlBuilder.embed(37.566500, 126.978000)
                .startsWith("https://www.openstreetmap.org/"));
        assertTrue(MapUrlBuilder.embed(21.027800, 105.834200)
                .startsWith("https://www.openstreetmap.org/"));
        assertTrue(MapUrlBuilder.embed(27.717200, 85.324000)
                .startsWith("https://www.openstreetmap.org/"));
        assertTrue(MapUrlBuilder.embed(1.352100, 103.819800)
                .startsWith("https://www.openstreetmap.org/"));
    }

    @Test
    public void representativeMainlandChinaLocationsUseAmap() {
        assertTrue(MapUrlBuilder.embed(39.904200, 116.407400)
                .startsWith("https://uri.amap.com/"));
        assertTrue(MapUrlBuilder.embed(43.825600, 87.616800)
                .startsWith("https://uri.amap.com/"));
        assertTrue(MapUrlBuilder.embed(25.038900, 102.718300)
                .startsWith("https://uri.amap.com/"));
        assertTrue(MapUrlBuilder.embed(45.803800, 126.534900)
                .startsWith("https://uri.amap.com/"));
    }

    @Test
    public void mapHostPolicyAllowsOnlyHttpsAmapAndOsmHosts() {
        assertTrue(MapUrlBuilder.isAllowedMapUri("https", "uri.amap.com"));
        assertTrue(MapUrlBuilder.isAllowedMapUri("https", "m.amap.com"));
        assertTrue(MapUrlBuilder.isAllowedMapUri("https", "www.openstreetmap.org"));

        assertTrue(!MapUrlBuilder.isAllowedMapUri("http", "uri.amap.com"));
        assertTrue(!MapUrlBuilder.isAllowedMapUri("https", "amap.com.example.com"));
        assertTrue(!MapUrlBuilder.isAllowedMapUri("https", null));
    }
}
