# 智途 Android 客户端全面改版 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Java/XML Android 客户端改造成无障碍优先、适合竞赛演示且可稳定连接现有 Django 服务的“智途导盲助手”。

**Architecture:** 保留 Activity + Retrofit 架构，引入小型配置、地址校验和错误解析组件，统一所有页面的 Material 视觉资源。服务器和无障碍设置持久化在独立 SharedPreferences 中，业务接口保持现有路径。

**Tech Stack:** Java 11、Android XML、Material Components、Retrofit 2、OkHttp、Gson、JUnit 4。

## Global Constraints

- 团队名称固定为“智途”，口号固定为“智行无碍，途达万家”。
- “关于我们”不得出现成员、学校、教师、电话等个人信息。
- 保留 Java + XML，不迁移 Jetpack Compose，不引入地图 SDK。
- 主要按钮高度至少 56dp，图标按钮至少 48dp，正文默认至少 16sp。
- 红灯、未知和紧急状态必须同时使用中文文字与语义色。
- 不伪造尚未接入的 XIAO ESP32S3、测距模块或设备在线状态。
- 当前工作区不是 Git 仓库；每个任务以测试和文件检查点代替提交。

---

### Task 1: 配置、地址校验和错误解析基础

**Files:**
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/AppSettings.java`
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/ServerAddress.java`
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/api/ApiErrorParser.java`
- Create: `03_android_client/GuideSticks_android/app/src/test/java/com/example/guidesticks_android/ServerAddressTest.java`
- Create: `03_android_client/GuideSticks_android/app/src/test/java/com/example/guidesticks_android/api/ApiErrorParserTest.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/api/RetrofitClient.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/api/ApiService.java`

**Interfaces:**
- `ServerAddress.normalize(String): String` validates HTTP(S), host and port, then appends `/`.
- `ApiErrorParser.parse(String, String): String` returns the first useful DRF error.
- `AppSettings` stores server URL, large text, auto-speak, haptics and latest recognition.
- `RetrofitClient.getInstance(Context): RetrofitClient` rebuilds when the configured URL changes.

- [ ] Write tests for URL normalization, invalid schemes, DRF field errors and `non_field_errors`.
- [ ] Run `gradlew.bat testDebugUnitTest` and confirm the new tests fail before implementation.
- [ ] Implement `ServerAddress`, `ApiErrorParser` and `AppSettings` with default URL `http://172.20.10.3:8000/`.
- [ ] Refactor `RetrofitClient` to use application context, 10-second connect/read/write timeouts and configured base URL.
- [ ] Add `ApiService.health()` using `@GET("android/test/") Call<ResponseBody>`.
- [ ] Run unit tests and record a passing checkpoint.

### Task 2: 统一视觉资源、中文文案和无障碍基础

**Files:**
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/BaseActivity.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/values/colors.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/values/themes.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/values-night/themes.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/values/styles.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/values/strings.xml`
- Modify: `03_android_client/GuideSticks_android/app/src/main/res/menu/bottom_nav_menu.xml`
- Create: semantic icon and background XML files under `app/src/main/res/drawable/`

**Interfaces:**
- `BaseActivity` applies the stored font scale before view inflation.
- `BaseActivity.performAccessibleFeedback(View)` performs haptic feedback only when enabled.

- [ ] Replace the one-note blue palette with teal primary, neutral surfaces, amber warning and red danger colors.
- [ ] Define reusable toolbar, primary button, danger button, status panel and input styles with maximum 8dp card corners.
- [ ] Move every visible string from layouts and modified Activities into UTF-8 `strings.xml`.
- [ ] Replace bitmap navigation icons where suitable with vector icons and add content descriptions.
- [ ] Make all Activities extend `BaseActivity`.
- [ ] Build resources with `gradlew.bat assembleDebug` and resolve all malformed XML or missing resource errors.

### Task 3: 登录、注册和首页改版

**Files:**
- Modify: `app/src/main/res/layout/activity_login.xml`
- Modify: `app/src/main/res/layout/activity_register.xml`
- Modify: `app/src/main/res/layout/activity_home.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/LoginActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/RegisterActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/HomeActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/model/IdentifyResponse.java`

**Interfaces:**
- Authentication callbacks use `ApiErrorParser` for non-2xx responses.
- Home uses `ApiService.health()` for real service status.
- Identify stores `light`, `command`, `message` and timestamp through `AppSettings` for Home to display.

- [ ] Build scroll-safe login and registration forms with brand, slogan, outlined fields and loading indicator.
- [ ] Validate empty fields, email format and password confirmation inline; disable submission while a request is active.
- [ ] Parse and display backend registration/login messages, including duplicate username and email.
- [ ] Build Home with brand header, real server status, primary assistant entry, semantic feature actions and latest recognition panel.
- [ ] Refresh health and latest scene in `onResume`, and expose a manual service refresh action.
- [ ] Run unit tests and `assembleDebug`.

### Task 4: 出行助手对话体验

**Files:**
- Modify: `app/src/main/res/layout/activity_assistant.xml`
- Modify: `app/src/main/res/layout/item_chat_message.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/AssistantActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/ChatMessageAdapter.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/ConversationStore.java`
- Modify: `app/src/test/java/com/example/guidesticks_android/ConversationStoreTest.java`
- Create: assistant toolbar vector icons under `app/src/main/res/drawable/`

**Interfaces:**
- Quick prompts call the same `sendQuestion(String)` path as typed and spoken input.
- `AppSettings.isAutoSpeakEnabled()` controls automatic TTS.
- Clear action resets local conversation only; no server-side state exists.

- [ ] Add an initial assistant greeting and three compact quick prompts for scene, crossing safety and calming assistance.
- [ ] Add stop-speaking and clear-conversation toolbar actions with content descriptions.
- [ ] Redesign message bubbles for high contrast and stable width without negative letter spacing.
- [ ] Keep the failed question available for retry and show a concise connection error with current server address.
- [ ] Apply auto-speak preference and stop TTS when the user taps the stop action.
- [ ] Extend ConversationStore tests for clear and retry-safe history behavior.
- [ ] Run unit tests and `assembleDebug`.

### Task 5: 识别、寻杖和紧急呼叫体验

**Files:**
- Modify: `app/src/main/res/layout/activity_identify.xml`
- Modify: `app/src/main/res/layout/activity_find_stick.xml`
- Modify: `app/src/main/res/layout/activity_emergency.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/IdentifyActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/FindStickActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/EmergencyActivity.java`

**Interfaces:**
- Recognition uses `IdentifyResponse.getCommand()` and `getMessage()` and saves the latest result in `AppSettings`.
- FindStick owns and cancels its active Retrofit calls and polling Runnable.
- Emergency constructs `/android/iscall/` from `AppSettings.getServerUrl()`.

- [ ] Redesign recognition preview and result panel with loading, red, green, caution and error states.
- [ ] Prevent duplicate capture while upload/inference is active and restore capture after completion.
- [ ] Replace FindStick's indefinite spinner with sending, waiting, found, timeout and failure states plus retry/cancel actions.
- [ ] Add a 30-second find timeout and cancel all calls/callbacks on exit.
- [ ] Redesign Emergency with saved contact, monitoring status and a red call action.
- [ ] Add confirmation before manual `ACTION_CALL`; keep hardware-triggered call behavior and translate all status text to Chinese.
- [ ] Use configured server address and close `HttpURLConnection` in all outcomes.
- [ ] Run `assembleDebug` and exercise Activity lifecycle paths through code inspection.

### Task 6: 位置、我的、设置和关于我们

**Files:**
- Create: `app/src/main/java/com/example/guidesticks_android/SettingsActivity.java`
- Create: `app/src/main/res/layout/activity_settings.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/layout/activity_list.xml`
- Modify: `app/src/main/res/layout/activity_profile.xml`
- Modify: `app/src/main/res/layout/activity_about.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/ListActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/ProfileActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/AboutActivity.java`

**Interfaces:**
- Settings edits `AppSettings`; server save calls `RetrofitClient.reset()`.
- Connection test uses a temporary Retrofit instance only after normalized URL validation.
- Profile logout clears token, username and email from `UserPrefs` without clearing `GuideSticksSettings`.

- [ ] Redesign location as an unframed information surface showing address, coordinates, accuracy and update time.
- [ ] Add refresh and Settings navigation for denied permissions or disabled location services.
- [ ] Redesign Profile into account, safety and application-setting sections.
- [ ] Implement Settings switches for large text, auto-speak and haptic feedback.
- [ ] Implement server address editing, validation, save and real connection test.
- [ ] Replace About content with “智途”、“智行无碍，途达万家”、capabilities and privacy statement only.
- [ ] Verify no personal names, schools or teacher references remain in Android resources.
- [ ] Run unit tests and `assembleDebug`.

### Task 7: 全量验证与交付

**Files:**
- Create: `docs/android-client-manual-checklist.md`
- Update: implementation files only for defects found during verification.

**Interfaces:**
- APK output: `03_android_client/GuideSticks_android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] Run `gradlew.bat testDebugUnitTest --rerun-tasks` and require zero failures.
- [ ] Run `gradlew.bat lintDebug` and fix error-level findings plus actionable accessibility warnings.
- [ ] Run `gradlew.bat assembleDebug --rerun-tasks` and require `BUILD SUCCESSFUL`.
- [ ] Search source for mojibake markers, hard-coded `172.20.10.3`, personal names, and visible English status text.
- [ ] Confirm the APK exists and report its exact size and path.
- [ ] Write a manual device checklist for camera, microphone, TTS, dialing, location, haptics and server switching.
- [ ] Report any unverified hardware/device-only behavior without claiming it passed.
