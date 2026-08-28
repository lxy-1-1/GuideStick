# DeepSeek Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add DeepSeek V4 Flash guidance to the existing YOLOv5 identify endpoint without breaking ESP32-CAM response parsing.

**Architecture:** A focused `deepseek_client` module calls the external API and validates its JSON. The existing view determines the YOLO label, asks the client for guidance, and uses deterministic local guidance whenever the external service fails.

**Tech Stack:** Python 3.9, Django 4.1, Django REST Framework, requests, unittest/Django test runner.

## Global Constraints

- Keep `light` equal to `red`, `green`, or `no` in every successful identify response.
- Never read, log, return, or commit `DEEPSEEK_API_KEY`.
- Use `deepseek-v4-flash`, non-thinking mode, JSON output, and a bounded timeout.
- Do not call the real DeepSeek API from automated tests.
- This workspace is not a Git repository, so commit steps are unavailable.

---

### Task 1: DeepSeek Client

**Files:**
- Create: `02_backend/GuideSticks/GuideSticks/Android/deepseek_client.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/tests.py`

**Interfaces:**
- Produces: `get_guidance(light: str) -> dict[str, str]`
- Produces: `local_guidance(light: str) -> dict[str, str]`
- Produces: `DeepSeekError`

- [ ] Write tests that require valid API JSON to become a normalized command/message pair.
- [ ] Run the targeted tests and verify failure because the module does not exist.
- [ ] Implement request construction with `requests.post`, `response_format={"type": "json_object"}`, disabled thinking, and a timeout.
- [ ] Validate the response and force red-light guidance to `STOP`.
- [ ] Run the targeted tests and verify they pass.

### Task 2: Identify Endpoint Integration

**Files:**
- Modify: `02_backend/GuideSticks/GuideSticks/Android/views.py`
- Modify: `02_backend/GuideSticks/GuideSticks/Android/tests.py`

**Interfaces:**
- Consumes: `get_guidance`, `local_guidance`, and `DeepSeekError` from Task 1.
- Produces: JSON with `light`, `command`, `message`, and `source`.

- [ ] Write an API test for a successful DeepSeek-enhanced identify response.
- [ ] Write an API test proving DeepSeek failure returns local fallback guidance with HTTP 200.
- [ ] Run the tests and verify the endpoint response lacks the new fields.
- [ ] Update `Identify.post()` to derive the light label once, invoke DeepSeek, and catch `DeepSeekError`.
- [ ] Run the API tests and verify they pass.

### Task 3: Regression Verification

**Files:**
- Verify: `02_backend/GuideSticks/GuideSticks/.gitignore`
- Verify: `02_backend/GuideSticks/GuideSticks/Android/tests.py`
- Create: `02_backend/GuideSticks/GuideSticks/GuideSticks/test_settings.py`

- [ ] Confirm `.env` is ignored without opening or printing it.
- [ ] Run the complete Django test suite with an isolated in-memory SQLite test database.
- [ ] Run `python manage.py check`.
- [ ] Review changed files for accidental secrets and response incompatibilities.
