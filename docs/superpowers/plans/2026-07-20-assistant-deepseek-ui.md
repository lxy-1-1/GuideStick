# Assistant DeepSeek Style UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将出行助手改为 DeepSeek 式轻量对话布局，增大文字与操作控件，同时保留语音输入、自动播报和 TalkBack。

**Architecture:** 使用纯 Java `ChatMessagePresentation` 将消息角色映射为来源标签和无障碍格式资源；`ChatMessageAdapter` 只根据展示模型绑定左右位置、颜色和间距；XML 负责稳定尺寸、消息排版和输入区结构。

**Tech Stack:** Android Java、Material Components、JUnit 4、Gradle Kotlin DSL。

## Global Constraints

- 不修改 Django、DeepSeek API、请求模型和对话历史逻辑。
- 不增加第三方依赖。
- 消息正文使用 `18sp`，来源标签使用 `14sp`。
- 语音与发送控件不小于 `56dp`。
- 当前目录不是 Git 仓库，执行测试驱动步骤但不执行 Git 提交。

---

### Task 1: 消息角色展示模型

**Files:**
- Create: `app/src/main/java/com/example/guidesticks_android/ChatMessagePresentation.java`
- Create: `app/src/test/java/com/example/guidesticks_android/ChatMessagePresentationTest.java`

**Interfaces:**
- Produces: `ChatMessagePresentation.from(String role)`
- Produces: `isUser(): boolean`
- Produces: `senderLabelRes(): int`
- Produces: `accessibilityFormatRes(): int`

- [ ] **Step 1: Write failing role mapping tests**

```java
@Test public void userRoleUsesUserPresentation() {
    ChatMessagePresentation item = ChatMessagePresentation.from("user");
    assertTrue(item.isUser());
    assertEquals(R.string.chat_sender_user, item.senderLabelRes());
}

@Test public void assistantAndUnknownRolesUseAssistantPresentation() {
    assertFalse(ChatMessagePresentation.from("assistant").isUser());
    assertFalse(ChatMessagePresentation.from("unknown").isUser());
}
```

- [ ] **Step 2: Run focused test and confirm missing-class failure**

Run: `./gradlew.bat testDebugUnitTest --tests "com.example.guidesticks_android.ChatMessagePresentationTest" --console=plain`

- [ ] **Step 3: Implement immutable presentation mapping**

Map only exact `user` to the user appearance; map all other roles to assistant appearance so malformed server roles never look like user-authored content.

- [ ] **Step 4: Run focused test and confirm pass**

Expected: all presentation tests PASS.

### Task 2: DeepSeek 风格消息项

**Files:**
- Modify: `app/src/main/res/layout/item_chat_message.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/ChatMessageAdapter.java`
- Create: `app/src/main/res/drawable/bg_chat_user.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ChatMessagePresentation.from`

- [ ] **Step 1: Replace the single TextView with sender and body views**

Use `messageContainer`, `tvSender`, and `tvMessage`. Body text is `18sp`, line spacing is `5dp`, and the sender is `14sp` bold.

- [ ] **Step 2: Add distinct user and assistant bubble backgrounds**

Use a solid `chat_user_container` with 8dp corners. Assistant messages use `#F1F5F4`, a `#D5DFDC` 1dp stroke, 8dp corners, and the same readable internal padding.

- [ ] **Step 3: Bind every recycled-view property**

For every row set gravity, sender text, sender gravity, body background, body text color, body padding, and max width. Never rely on properties left by the previous recycled row.

- [ ] **Step 4: Preserve TalkBack descriptions**

Set the body description using `chat_user_message` or `chat_assistant_message`; set the visible sender label to `importantForAccessibility=no` to prevent duplicate reading.

- [ ] **Step 5: Run all unit tests**

Expected: BUILD SUCCESSFUL.

### Task 3: 快捷问题与输入区

**Files:**
- Modify: `app/src/main/res/layout/activity_assistant.xml`
- Create: `app/src/main/res/drawable/bg_assistant_composer.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Existing IDs `chipScene`, `chipCrossing`, `chipCalm`, `etQuestion`, `btnMic`, and `btnSend` remain unchanged.

- [ ] **Step 1: Restyle prompt chips**

Use 52dp height, 16sp text, filled `assistant_suggestion` background, no visible stroke, and 8dp spacing.

- [ ] **Step 2: Increase list breathing room**

Increase top and bottom padding, keep the list full-width, and use `stackFromBottom=true` so active conversation remains near the composer.

- [ ] **Step 3: Build the composer**

Use a white bottom bar with a 1dp top divider. The text input has a soft filled background, no exposed outlined box, `18sp` text, 56dp minimum height, and the hint “向智途助手提问”.

- [ ] **Step 4: Increase voice and send buttons**

Set both to 56dp, preserve content descriptions, and keep send as the only solid primary action.

- [ ] **Step 5: Verify resource compilation and lint**

Run: `./gradlew.bat testDebugUnitTest lintDebug --console=plain`

Expected: BUILD SUCCESSFUL and no accessibility errors.

### Task 4: 真实的自动播报开关

**Files:**
- Create: `app/src/main/java/com/example/guidesticks_android/SpeechMenuState.java`
- Create: `app/src/test/java/com/example/guidesticks_android/SpeechMenuStateTest.java`
- Create: `app/src/main/res/drawable/ic_speaker_on.xml`
- Modify: `app/src/main/res/menu/menu_assistant.xml`
- Modify: `app/src/main/java/com/example/guidesticks_android/AssistantActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `SpeechMenuState.iconRes(boolean enabled): int`
- Produces: `SpeechMenuState.titleRes(boolean enabled): int`
- Produces: `SpeechMenuState.statusRes(boolean enabled): int`

- [ ] **Step 1: Write failing state resource tests**

Verify that enabled state returns the speaker icon and “关闭自动语音播报”; disabled state returns the muted icon and “开启自动语音播报”.

- [ ] **Step 2: Run focused test and confirm failure**

Run: `./gradlew.bat testDebugUnitTest --tests "com.example.guidesticks_android.SpeechMenuStateTest" --console=plain`

- [ ] **Step 3: Implement state mapping and speaker icon**

Add a standard volume-up vector for enabled state and reuse `ic_stop_speaking` for disabled state.

- [ ] **Step 4: Replace the one-shot stop action with a toggle**

On click invert `AppSettings.isAutoSpeakEnabled()`, persist it, stop current speech when disabling, update icon/title, and announce the resulting state. Refresh the menu state in `onResume` so changes made on the settings page remain synchronized.

- [ ] **Step 5: Run all unit tests and lint**

Expected: BUILD SUCCESSFUL with zero accessibility errors.

### Task 5: Final verification and APK

**Files:**
- Verify: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Run full verification**

Run: `./gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Inspect unit-test and lint reports**

Require zero test failures, zero lint errors, and zero accessibility lint issues.

- [ ] **Step 3: Record APK size, timestamp and SHA-256**

Use `Get-FileHash app-debug.apk -Algorithm SHA256` and report the shared workspace path.

- [ ] **Step 4: Record device-only validation**

The final report must state that keyboard resizing, large-font wrapping and visual alignment still require one device screenshot because no emulator screenshot is generated by JVM tests.
