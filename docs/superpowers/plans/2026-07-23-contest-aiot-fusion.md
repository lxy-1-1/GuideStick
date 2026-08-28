# GuideStick Contest AIoT Fusion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tested camera-location-device fusion path whose structured context is processed by DeepSeek and whose validated command reaches the existing ESP32 parser.

**Architecture:** Android uploads phone location through a dedicated endpoint. Django caches that snapshot, fuses it with YOLO output and heartbeat status, and calls a focused DeepSeek guidance function. The existing response schema and deterministic fallback preserve firmware compatibility.

**Tech Stack:** Java Android, Retrofit/Gson, Django REST Framework, Python requests, Django cache, YOLOv5.

## Global Constraints

- Do not change ESP32 endpoint paths or response field names.
- Do not expose the DeepSeek API key to Android or firmware.
- Only `STOP`, `PROCEED`, and `CAUTION` may reach the device.
- Unsafe or unavailable model output must fall back locally.
- Do not claim the physical ESP32-S3 requirement is solved by this software change.

---

### Task 1: Backend Context And Fusion

**Files:**
- Create: `02_backend/GuideSticks/GuideSticks/Android/sensor_context.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/assistant_serializers.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/views.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/url.py`
- Test: `02_backend/GuideSticks/GuideSticks/Android/tests.py`

**Interfaces:**
- Consumes: validated phone location and `device_state.get_status()`
- Produces: `record_phone_context(...)`, `get_phone_context()`, and `build_fused_scene(...)`

- [ ] Write tests for valid, invalid, fresh, and stale location snapshots.
- [ ] Run the focused Django tests and confirm missing behavior fails.
- [ ] Implement cache-backed context storage and the sensor-context endpoint.
- [ ] Run focused tests and confirm they pass.

### Task 2: DeepSeek Restricted Downlink

**Files:**
- Modify: `02_backend/GuideSticks/GuideSticks/Android/deepseek_client.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/views.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/travel_assistant.py`
- Test: `02_backend/GuideSticks/GuideSticks/Android/tests.py`

**Interfaces:**
- Consumes: `build_fused_scene(light)`
- Produces: `guidance_about_scene(scene)` returning `command`, `message`, and `source`

- [ ] Write tests for model JSON, command allowlisting, red/no-light safety, and fallback.
- [ ] Run tests and confirm the existing local-only path fails the new expectations.
- [ ] Implement the focused model prompt, parsing, validation, and fallback.
- [ ] Use fused context in identify and travel-assistant paths.
- [ ] Run backend tests and confirm all pass.

### Task 3: Android Sensor Upload

**Files:**
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/model/SensorContextRequest.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/model/AssistantRequest.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/api/ApiService.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/ListActivity.java`
- Test: `03_android_client/GuideSticks_android/app/src/test/java/com/example/guidesticks_android/SensorContextRequestTest.java`

**Interfaces:**
- Consumes: Android `Location` and resolved address
- Produces: Retrofit request for `POST /android/sensor/context/`

- [ ] Write Java tests for serialized sensor and assistant context.
- [ ] Run unit tests and confirm new request types are missing.
- [ ] Implement request models and Retrofit endpoint.
- [ ] Upload after each accepted location update without blocking the map UI.
- [ ] Run Android unit tests.

### Task 4: Full Verification

**Files:**
- Verify only.

**Interfaces:**
- Consumes: completed backend and Android changes
- Produces: test and build evidence

- [ ] Run the full Django test suite.
- [ ] Run Android `testDebugUnitTest`.
- [ ] Run Android `lintDebug`.
- [ ] Run Android `assembleDebug`.
- [ ] Confirm firmware response compatibility and summarize the remaining ESP32-S3 hardware requirement.

