# Location Map Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the text-only location screen with an embedded, keyless OpenStreetMap view and a current-location marker.

**Architecture:** A pure Java `MapUrlBuilder` creates deterministic embed and browser URLs. `ListActivity` owns the WebView lifecycle and updates the map from its existing Android `LocationManager` callback; XML owns the stable map dimensions and loading/error overlays.

**Tech Stack:** Android Java/XML, `WebView`, OpenStreetMap embed page, JUnit 4.

## Global Constraints

- Keep phone GPS/network positioning as the only location source.
- Do not add a map SDK, API key, account, or paid service.
- Use HTTPS OpenStreetMap URLs only.
- Keep text location information functional when the map cannot load.
- Keep `minSdk 24` and `targetSdk 35`.

---

### Task 1: Deterministic map URLs

**Files:**
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/MapUrlBuilder.java`
- Create: `03_android_client/GuideSticks_android/app/src/test/java/com/example/guidesticks_android/MapUrlBuilderTest.java`

**Interfaces:**
- Produces: `embed(double latitude, double longitude)` and `browser(double latitude, double longitude)`.

- [ ] Write failing tests asserting an HTTPS OSM host, marker order `latitude,longitude`, bounding box order `west,south,east,north`, and browser query coordinates.
- [ ] Run the focused JUnit class and confirm failure because `MapUrlBuilder` is absent.
- [ ] Implement stable six-decimal formatting with `Locale.US` and a `0.003` degree map span.
- [ ] Re-run the focused tests and confirm they pass.

### Task 2: Embedded map location screen

**Files:**
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/layout/activity_list.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/values/strings.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/ListActivity.java`

**Interfaces:**
- Consumes: `MapUrlBuilder`, existing `LocationManager` results, and `geo:` intents.
- Produces: embedded map marker, map loading/error feedback, refresh action, and external map action.

- [ ] Add a fixed-height `WebView` map band with progress and failure overlays.
- [ ] Move address and coordinate information into a compact scrollable detail area below the map.
- [ ] Add an outlined “打开地图” button that stays disabled until a real location exists.
- [ ] Configure a restricted `WebViewClient`; load the embed URL in `showLocation` and update overlay states from main-frame callbacks.
- [ ] Open `geo:latitude,longitude?q=latitude,longitude(当前位置)` and fall back to the OSM browser URL when no map app is installed.
- [ ] Stop and destroy the WebView in `onDestroy`.

### Task 3: Verification and APK

**Files:**
- Modify: `docs/android-client-manual-checklist.md`

**Interfaces:**
- Produces: verified APK and physical-device map checks.

- [ ] Run all Android unit tests.
- [ ] Run Android Lint and fix every error.
- [ ] Assemble the debug APK.
- [ ] Record checks for marker position, refresh, external map, no-network fallback, and bottom navigation.
- [ ] Report the APK path, byte size, and SHA-256.
