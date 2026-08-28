# GuideStick Contest AIoT Fusion Design

## Goal

Make the three software-side Espressif-track requirements demonstrable:

1. Fuse camera-derived traffic-light state, phone location, and stick online state.
2. Send the fused structured context to the configured DeepSeek cloud model.
3. Let the stick consume a model-produced, server-validated command without changing
   the existing ESP32 response format.

The physical ESP32-S3 core-controller requirement is outside this change.

## Data Flow

1. Android obtains latitude, longitude, accuracy, address, and capture time.
2. Android posts that snapshot to `POST /android/sensor/context/`.
3. The backend stores a time-limited phone-sensor snapshot.
4. ESP32 uploads a camera image to the existing identify endpoint.
5. YOLOv5 derives `red`, `green`, or `no`.
6. The backend combines the traffic-light result, phone snapshot, and current stick
   heartbeat status into one fused scene.
7. DeepSeek returns JSON containing `command` and `message`.
8. The server validates the command against an allowlist and traffic-safety rules.
9. The existing identify response still contains `light`, `command`, and `message`,
   so current firmware continues parsing `STOP`, `PROCEED`, and `CAUTION`.
10. The travel assistant uses the same fused scene instead of only the light state.

## Safety Rules

- Allowed commands are only `STOP`, `PROCEED`, and `CAUTION`.
- A red light can only produce `STOP`.
- No detected light can only produce `CAUTION`.
- A green light may produce `PROCEED` or the more conservative `CAUTION`.
- Missing keys, invalid JSON, network errors, stale phone context, or unsafe model
  output fall back to the existing deterministic guidance.
- The API key remains server-side.
- Location is rounded before it is sent to the cloud model and stale data is marked
  explicitly.

## Compatibility

- No firmware reflash is required for this software change.
- Existing endpoint paths and existing identify response fields remain valid.
- Find-stick polling, heartbeat, emergency calling, TTS, and fixed voice commands are
  unchanged.
- Android may run without location permission; the fused context then marks location
  unavailable and recognition still works.

## Verification

- Django tests cover context validation, freshness, fusion, model JSON parsing,
  command safety, fallback, identify integration, and assistant integration.
- Android unit tests cover the sensor-context request and the expanded assistant
  request.
- Full Django tests, Android unit tests, lint, and debug APK build must pass.

