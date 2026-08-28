# Android Blind Accessibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让视障用户通过 TalkBack、语音播报和触觉反馈独立完成出行问答、交通灯识别、位置获取、寻杖和紧急求助。

**Architecture:** 在 `BaseActivity` 集中管理 TalkBack 状态通知、TTS 和不同强度的触觉反馈；使用纯 Java 的 `AccessibilityTextFormatter` 生成稳定、可测试的播报文本；各 Activity 只负责在业务状态变化时调用统一反馈接口，XML 负责控件语义和焦点顺序。

**Tech Stack:** Android Java、Material Components、Android TalkBack APIs、TextToSpeech、JUnit 4、Gradle Kotlin DSL。

## Global Constraints

- 不修改 Django API、ESP32-CAM 固件或现有接口路径。
- 不增加常驻麦克风监听或新的第三方依赖。
- 地图继续供家属查看，但不进入 TalkBack 焦点遍历。
- 关键结果不能只使用颜色表达。
- 当前 `E:\GuideStick` 不是 Git 仓库，因此执行测试驱动步骤但不执行 Git 提交。

---

## File Structure

- Create `app/src/main/java/com/example/guidesticks_android/AccessibilityTextFormatter.java`: 生成位置、识别和服务器状态播报文本。
- Create `app/src/test/java/com/example/guidesticks_android/AccessibilityTextFormatterTest.java`: 验证播报文本不会遗漏关键安全信息。
- Modify `BaseActivity.java`: 集中管理 TTS、TalkBack 通知和强触觉反馈。
- Modify `HomeActivity.java`, `IdentifyActivity.java`, `ListActivity.java`, `FindStickActivity.java`, `EmergencyActivity.java`, `AssistantActivity.java`, `SettingsActivity.java`: 在业务状态变化时触发反馈。
- Modify `activity_home.xml`, `activity_identify.xml`, `activity_list.xml`, `activity_find_stick.xml`, `activity_emergency.xml`, `activity_assistant.xml`, `activity_profile.xml`, `activity_settings.xml`: 建立标题、实时区域、焦点和控件描述。
- Modify `strings.xml`: 集中保存无障碍标签和播报文案。

### Task 1: 可测试的播报文本

**Files:**
- Create: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/AccessibilityTextFormatter.java`
- Create: `03_android_client/GuideSticks_android/app/src/test/java/com/example/guidesticks_android/AccessibilityTextFormatterTest.java`

**Interfaces:**
- Produces: `location(String address, String accuracy, String updated): String`
- Produces: `recognition(String message, String command): String`
- Produces: `serverStatus(String status): String`

- [ ] **Step 1: Write failing formatter tests**

```java
@Test public void locationIncludesAddressAccuracyAndTime() {
    assertEquals("当前位置，人民路 1 号。精度约 5 米。更新于 10:30。",
            AccessibilityTextFormatter.location("人民路 1 号", "精度约 5 米", "更新于 10:30"));
}

@Test public void recognitionIncludesOptionalCommand() {
    assertEquals("识别结果，前方为红灯。安全指令，停止前进。",
            AccessibilityTextFormatter.recognition("前方为红灯", "停止前进"));
}
```

- [ ] **Step 2: Run the focused tests and confirm failure**

Run: `./gradlew.bat testDebugUnitTest --tests "com.example.guidesticks_android.AccessibilityTextFormatterTest" --console=plain`

Expected: FAIL because `AccessibilityTextFormatter` does not exist.

- [ ] **Step 3: Implement deterministic formatting**

```java
public final class AccessibilityTextFormatter {
    public static String location(String address, String accuracy, String updated) {
        return join("当前位置", address, accuracy, updated);
    }

    public static String recognition(String message, String command) {
        String result = join("识别结果", message);
        return isBlank(command) ? result : result + "安全指令，" + sentence(command);
    }
}
```

- [ ] **Step 4: Run the focused tests and confirm pass**

Expected: all `AccessibilityTextFormatterTest` tests PASS.

### Task 2: 统一语音和触觉反馈

**Files:**
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/BaseActivity.java`
- Modify: `03_android_client/GuideSticks_android/app/src/main/java/com/example/guidesticks_android/AssistantActivity.java`

**Interfaces:**
- Produces: `announceStatus(View anchor, CharSequence message): void`
- Produces: `speakImportant(View anchor, CharSequence message): void`
- Produces: `performCriticalFeedback(View view): void`
- Produces: `stopSpeaking(): void`

- [ ] **Step 1: Add TTS lifecycle and accessibility manager handling to BaseActivity**

```java
protected void speakImportant(View anchor, CharSequence message) {
    if (message == null || message.length() == 0) return;
    if (isTouchExplorationEnabled()) {
        anchor.announceForAccessibility(message);
    } else if (appSettings.isAutoSpeakEnabled() && ttsReady) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "important-status");
    }
}
```

- [ ] **Step 2: Add strong emergency haptic feedback**

Use `HapticFeedbackConstants.LONG_PRESS` for emergency actions while preserving the existing configurable virtual-key feedback for ordinary actions.

- [ ] **Step 3: Migrate AssistantActivity to BaseActivity speech APIs**

Remove its duplicate `TextToSpeech` lifecycle, call `speakImportant(listView, answer)` for answers, and call `stopSpeaking()` from the toolbar action.

- [ ] **Step 4: Run all unit tests**

Run: `./gradlew.bat testDebugUnitTest --console=plain`

Expected: BUILD SUCCESSFUL.

### Task 3: 首页和交通灯识别

**Files:**
- Modify: `app/src/main/res/layout/activity_home.xml`
- Modify: `app/src/main/res/layout/activity_identify.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/HomeActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/IdentifyActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `BaseActivity.announceStatus`, `BaseActivity.speakImportant`
- Consumes: `AccessibilityTextFormatter.recognition`

- [ ] **Step 1: Give the home screen a stable TalkBack hierarchy**

Set brand and section labels as `android:accessibilityHeading="true"`; set decorative icons to `android:importantForAccessibility="no"`; make each action card focusable and assign a full action description such as “红绿灯识别，拍摄并识别前方交通灯”。

- [ ] **Step 2: Mark dynamic home status as live regions**

Set the server and latest-scene panels to `android:accessibilityLiveRegion="polite"`, and update their combined `contentDescription` whenever text changes.

- [ ] **Step 3: Add recognition state announcements**

Announce camera readiness and recognition progress. On completion call:

```java
String spoken = AccessibilityTextFormatter.recognition(message, response.getCommand());
resultPanel.setContentDescription(spoken);
speakImportant(resultPanel, spoken);
```

On failure announce the network failure and retry instruction.

- [ ] **Step 4: Run unit tests and Android lint**

Run: `./gradlew.bat testDebugUnitTest lintDebug --console=plain`

Expected: BUILD SUCCESSFUL with no new accessibility lint errors.

### Task 4: 位置页面的语音优先操作

**Files:**
- Modify: `app/src/main/res/layout/activity_list.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/ListActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `AccessibilityTextFormatter.location`
- Consumes: `BaseActivity.speakImportant`

- [ ] **Step 1: Add the “播报当前位置” button**

Add `btnSpeakLocation` as a full-width Material button before refresh/open-map actions. Keep it disabled until a location exists.

- [ ] **Step 2: Remove the map from TalkBack traversal**

Set the map container to `android:importantForAccessibility="noHideDescendants"`; expose the address/status panel instead.

- [ ] **Step 3: Build and announce the location summary**

Store the formatted accuracy and update strings. On a successful location update, set the location panel description and politely announce it. On explicit button press, force the same summary through `speakImportant`.

- [ ] **Step 4: Announce location failure modes**

Permission denied, location services disabled, timeout, and geocoder failure must each set visible text and call `announceStatus` once.

- [ ] **Step 5: Run unit tests and lint**

Expected: location formatting tests pass and no unlabeled button is reported.

### Task 5: 寻杖、紧急求助和设置状态

**Files:**
- Modify: `app/src/main/res/layout/activity_find_stick.xml`
- Modify: `app/src/main/res/layout/activity_emergency.xml`
- Modify: `app/src/main/res/layout/activity_settings.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/FindStickActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/EmergencyActivity.java`
- Modify: `app/src/main/java/com/example/guidesticks_android/SettingsActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: BaseActivity feedback methods.

- [ ] **Step 1: Make status panels polite live regions**

Mark find-stick, emergency and server-test status views with `android:accessibilityLiveRegion="polite"` and remove decorative image focus.

- [ ] **Step 2: Add find-stick feedback and retry focus**

Announce sending/success/failure. On success perform ordinary haptic feedback; on failure show `btnRetry`, request TalkBack focus with `View.ACCESSIBILITY_FOCUS`, and describe that the stick firmware must support the command.

- [ ] **Step 3: Add critical emergency feedback**

Call `performCriticalFeedback` before dispatch. Announce missing contact, permissions, signal detection, call/SMS start and partial failures without reading the phone number unnecessarily.

- [ ] **Step 4: Announce settings connection results**

After server save/test, update the live status and call `announceStatus` for success or failure.

- [ ] **Step 5: Run unit tests and lint**

Expected: BUILD SUCCESSFUL.

### Task 6: 其余页面语义和最终验证

**Files:**
- Modify: `app/src/main/res/layout/activity_assistant.xml`
- Modify: `app/src/main/res/layout/activity_profile.xml`
- Modify: `app/src/main/res/layout/activity_settings.xml`
- Modify: `app/src/main/res/layout/activity_login.xml`
- Modify: `app/src/main/res/layout/activity_register.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Android XML accessibility properties and existing string resources.

- [ ] **Step 1: Remove duplicate icon focus and add headings**

Decorative icons use `importantForAccessibility="no"`; page and section labels use `accessibilityHeading="true"`; click targets expose one complete TalkBack label.

- [ ] **Step 2: Verify touch targets and dynamic text**

All critical controls remain at least 48dp; dynamic text uses `sp`; input hints remain associated through `TextInputLayout`.

- [ ] **Step 3: Run full verification**

Run: `./gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain`

Expected: BUILD SUCCESSFUL and APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Report remaining device-only validation**

Record that TalkBack focus order, spoken overlap and actual vibration strength require one final Android device pass because they cannot be proven by JVM tests or lint alone.
