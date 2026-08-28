# Zhitu Launcher Icon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace every Android launcher and in-app brand image with one coherent Zhitu navigation icon.

**Architecture:** Keep one vector geometry across legacy, round, adaptive, and monochrome resources. Android version qualifiers select the appropriate wrapper while the manifest and About page reference the same launcher resource names.

**Tech Stack:** Android VectorDrawable, AdaptiveIconDrawable, XML resources, Gradle Android plugin.

## Global Constraints

- Background color is `#006C67`.
- Route is white and the destination point is amber `#F2B544`.
- No people, text, gradients, or details that disappear at launcher size.
- Keep all foreground geometry within the adaptive icon safe zone.

---

### Task 1: Replace icon vector resources

**Files:**
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/drawable/ic_launcher_background.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `03_android_client/GuideSticks_android/app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- Create: `03_android_client/GuideSticks_android/app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`

**Interfaces:**
- Produces: `@mipmap/ic_launcher`, `@mipmap/ic_launcher_round`, `@drawable/ic_launcher_foreground`.

- [ ] Replace the default Android background with a solid brand background.
- [ ] Draw the white Z-shaped route and amber destination point inside the 108x108 safe area.
- [ ] Add rounded-square and round legacy wrappers using the same geometry.

### Task 2: Use the new icon everywhere

**Files:**
- Modify: `03_android_client/GuideSticks_android/app/src/main/AndroidManifest.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/layout/activity_about.xml`

**Interfaces:**
- Consumes: launcher resources from Task 1.

- [ ] Point `android:icon` and `android:roundIcon` to the new launcher resources.
- [ ] Point the About page image to `@mipmap/ic_launcher`.
- [ ] Remove the obsolete silhouette JPG resource.

### Task 3: Verify the Android package

**Files:**
- Verify: `03_android_client/GuideSticks_android/app/build/outputs/apk/debug/app-debug.apk`

- [ ] Run `gradlew lintDebug --no-daemon` and require exit code 0.
- [ ] Run `gradlew assembleDebug --rerun-tasks --no-daemon` and require exit code 0.
- [ ] Confirm the APK timestamp, size, and SHA-256 checksum.
