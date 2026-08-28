# Location Map UI Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the map fill unused vertical space while keeping location details and commands compact and stable.

**Architecture:** This is an XML-only layout refinement. The existing `ListActivity`, WebView lifecycle, map URLs, location callbacks, and bottom navigation contracts remain unchanged.

**Tech Stack:** Android XML, Material Components, existing Java location screen.

## Global Constraints

- Do not change any view IDs consumed by `ListActivity`.
- Do not modify map, location, emergency, server, or firmware behavior.
- Keep the map unobstructed and the detail section full-width without floating cards.
- Keep controls usable in large-text mode and on narrow portrait screens.

---

### Task 1: Responsive map-first layout

**Files:**
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/layout/activity_list.xml`

**Interfaces:**
- Consumes: existing IDs `mapWebView`, `mapStatusOverlay`, `tvList`, `tvCoordinates`, `tvAccuracy`, `tvLocationTime`, `btnRefreshLocation`, and `btnOpenMap`.
- Produces: elastic map height and compact detail band without Java changes.

- [ ] Change the map container to `0dp` height with `layout_weight="1"` and a portrait-safe minimum height.
- [ ] Change the details scroll container from weighted fill to content-height layout.
- [ ] Use a full-width surface background and reduce vertical spacing without reducing touch targets.
- [ ] Limit the address to two lines with end ellipsis.
- [ ] Place accuracy and update time in one stable horizontal metadata row.
- [ ] Preserve two equal-width command buttons and bottom navigation clearance.

### Task 2: Verification and APK

**Files:**
- Modify: `docs/android-client-manual-checklist.md`

**Interfaces:**
- Produces: verified resources and installable debug APK.

- [ ] Run all Android unit tests.
- [ ] Run Android Lint to validate resource references and accessibility.
- [ ] Assemble the debug APK.
- [ ] Record map-size, long-address, large-text, map-control, and bottom-navigation checks.
- [ ] Report the APK path, byte size, and SHA-256.
