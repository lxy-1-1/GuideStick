# DeepSeek Guidance Design

## Goal

Insert DeepSeek V4 Flash after the existing YOLOv5 traffic-light detector while preserving the response keywords consumed by the ESP32-CAM firmware.

## Data Flow

1. ESP32-CAM posts a base64 JPEG to `/Android/identify/`.
2. YOLOv5 classifies the traffic light as `red`, `green`, or `no`.
3. Django sends that structured label, not the image, to DeepSeek V4 Flash.
4. DeepSeek returns JSON containing `command` and `message`.
5. Django responds with `light`, `command`, `message`, and `source`.
6. Existing ESP32-CAM substring checks continue to find `red`, `green`, or `no`.

## Boundaries

- `Android/deepseek_client.py` owns API request construction, response parsing, validation, and deterministic fallback messages.
- `Android/views.py` owns image decoding, YOLO inference, and HTTP response composition.
- The API key remains in `.env` and is read through Django settings. It is never logged or returned.

## Safety And Failure Handling

- DeepSeek uses non-thinking mode, strict JSON output, a short response limit, and a network timeout.
- Only `STOP`, `PROCEED`, and `CAUTION` commands are accepted.
- A red-light result is always forced to `STOP`, even if the model returns another command.
- Missing configuration, timeout, non-200 response, malformed JSON, or invalid command falls back to deterministic local guidance while preserving HTTP 200.
- YOLO inference failures retain the existing HTTP 500 behavior.

## Verification

- Unit tests cover successful JSON parsing, mandatory red-light stop enforcement, missing-key errors, and malformed responses.
- API tests cover DeepSeek-enhanced output and fallback output without making external network calls.
- The existing Android tests remain green.
