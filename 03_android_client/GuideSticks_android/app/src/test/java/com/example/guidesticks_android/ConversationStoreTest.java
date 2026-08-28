package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConversationStoreTest {
    @Test
    public void apiHistoryKeepsLatestSixMessages() {
        ConversationStore store = new ConversationStore();
        for (int i = 0; i < 8; i++) {
            store.add(i % 2 == 0 ? "user" : "assistant", "message-" + i);
        }

        assertEquals(6, store.historyForApi().size());
        assertEquals("message-2", store.historyForApi().get(0).getContent());
        assertEquals("message-7", store.historyForApi().get(5).getContent());
    }

    @Test
    public void blankMessagesAreIgnored() {
        ConversationStore store = new ConversationStore();
        store.add("user", "   ");
        assertEquals(0, store.allMessages().size());
    }

    @Test
    public void clearRemovesAllMessagesAndHistory() {
        ConversationStore store = new ConversationStore();
        store.add("user", "question");
        store.add("assistant", "answer");

        store.clear();

        assertEquals(0, store.allMessages().size());
        assertEquals(0, store.historyForApi().size());
    }
}
