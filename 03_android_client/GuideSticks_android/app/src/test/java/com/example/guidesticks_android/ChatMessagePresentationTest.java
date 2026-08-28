package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatMessagePresentationTest {
    @Test
    public void userRoleUsesUserPresentation() {
        ChatMessagePresentation item = ChatMessagePresentation.from("user");

        assertTrue(item.isUser());
        assertEquals(R.string.chat_sender_user, item.senderLabelRes());
        assertEquals(R.string.chat_user_message, item.accessibilityFormatRes());
    }

    @Test
    public void assistantRoleUsesAssistantPresentation() {
        ChatMessagePresentation item = ChatMessagePresentation.from("assistant");

        assertFalse(item.isUser());
        assertEquals(R.string.chat_sender_assistant, item.senderLabelRes());
        assertEquals(R.string.chat_assistant_message, item.accessibilityFormatRes());
    }

    @Test
    public void unknownRoleFallsBackToAssistantPresentation() {
        assertFalse(ChatMessagePresentation.from("unknown").isUser());
        assertFalse(ChatMessagePresentation.from(null).isUser());
    }
}
