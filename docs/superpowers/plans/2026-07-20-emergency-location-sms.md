# Emergency Location SMS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every manual or stick-triggered emergency action call the preconfigured contact and directly send a location-bearing SMS from the Android phone.

**Architecture:** `EmergencyActivity` remains the Android orchestrator, while a small pure-Java `EmergencyMessageBuilder` owns deterministic SMS text generation. The Django emergency status endpoint becomes consume-on-read so one stick signal cannot trigger repeated calls.

**Tech Stack:** Android Java/XML, `LocationManager`, `SmsManager`, Django REST Framework, JUnit 4, Django test client.

## Global Constraints

- Phone GPS/network location is the only location source.
- The phone number comes from `UserPrefs/emergency_phone` and must be configured before use.
- Calling must not wait for a fresh GPS fix.
- Location failure must fall back to an SMS that clearly says location is unavailable.
- Keep `minSdk 24`, `targetSdk 35`, and avoid new runtime dependencies.

---

### Task 1: Deterministic emergency SMS content

**Files:**
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/EmergencyMessageBuilder.java`
- Create: `03_android_client/GuideSticks_android/app/src/test/java/com/example/guidesticks_android/EmergencyMessageBuilderTest.java`

**Interfaces:**
- Produces: `EmergencyMessageBuilder.withLocation(double latitude, double longitude, float accuracyMeters)` and `EmergencyMessageBuilder.withoutLocation()`.

- [ ] Write failing JUnit tests asserting the brand, coordinates, accuracy, map URL, and no-location fallback.
- [ ] Run `gradlew.bat testDebugUnitTest --tests com.example.guidesticks_android.EmergencyMessageBuilderTest` and confirm compilation fails because the class is absent.
- [ ] Implement the two pure string-building methods using `Locale.US` for stable decimal formatting.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Consume emergency server signals once

**Files:**
- Modify: `02_backend/GuideSticks/GuideSticks/Android/views.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/tests.py`

**Interfaces:**
- Consumes: `GET /android/emergency/` to set the signal.
- Produces: `GET /android/iscall/` returns `{"isCall": 1}` once and then `{"isCall": 0}`.

- [ ] Add a failing Django test that triggers once and reads status twice.
- [ ] Run the focused Django test and confirm the second read incorrectly remains 1.
- [ ] Reset `isCall` inside `IsCallView.get()` immediately after a positive read.
- [ ] Re-run the focused backend test and confirm it passes.

### Task 3: Unified Android emergency dispatcher

**Files:**
- Modify: `03_android_client/GuideSticks_android/app/src/main/AndroidManifest.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/EmergencyActivity.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/layout/activity_emergency.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `EmergencyMessageBuilder`, `UserPrefs/emergency_phone`, Android location/telephony services, and the existing `/android/iscall/` poll.
- Produces: one emergency action that queues an SMS and launches `ACTION_CALL` without waiting for location.

- [ ] Add `SEND_SMS` to the manifest and replace the editable emergency number with a saved-number display plus manage-contact action.
- [ ] Route manual taps and positive server polls to one `beginEmergency(String phone)` method.
- [ ] Request missing call, SMS, and location permissions together, then continue with every granted capability.
- [ ] Start the call immediately; independently use the best cached GPS/network location for SMS, falling back to `withoutLocation()` when unavailable.
- [ ] Split long messages with `SmsManager.divideMessage` and send all parts.
- [ ] Cancel location callbacks and polling during destruction and prevent duplicate dispatch while one emergency action is active.

### Task 4: Verification and deliverable

**Files:**
- Modify: `docs/android-client-manual-checklist.md`

**Interfaces:**
- Produces: verified debug APK and an explicit physical-phone test checklist.

- [ ] Run all Django tests with the project test settings.
- [ ] Run Android `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
- [ ] Record manual checks for runtime permissions, SMS delivery, map URL, direct call, location-disabled fallback, and one-shot stick triggering.
- [ ] Report the APK absolute path, size, and SHA-256 hash.
