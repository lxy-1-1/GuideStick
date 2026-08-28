package com.example.guidesticks_android;

public final class ChatMessagePresentation {
    private final boolean user;
    private final int senderLabelRes;
    private final int accessibilityFormatRes;

    private ChatMessagePresentation(boolean user, int senderLabelRes,
                                    int accessibilityFormatRes) {
        this.user = user;
        this.senderLabelRes = senderLabelRes;
        this.accessibilityFormatRes = accessibilityFormatRes;
    }

    public static ChatMessagePresentation from(String role) {
        if ("user".equals(role)) {
            return new ChatMessagePresentation(
                    true,
                    R.string.chat_sender_user,
                    R.string.chat_user_message);
        }
        return new ChatMessagePresentation(
                false,
                R.string.chat_sender_assistant,
                R.string.chat_assistant_message);
    }

    public boolean isUser() {
        return user;
    }

    public int senderLabelRes() {
        return senderLabelRes;
    }

    public int accessibilityFormatRes() {
        return accessibilityFormatRes;
    }
}
