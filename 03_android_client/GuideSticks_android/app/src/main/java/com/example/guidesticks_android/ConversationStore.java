package com.example.guidesticks_android;

import com.example.guidesticks_android.model.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationStore {
    private static final int API_HISTORY_LIMIT = 6;
    private final List<ChatMessage> messages = new ArrayList<>();

    public void add(String role, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        messages.add(new ChatMessage(role, content.trim()));
    }

    public List<ChatMessage> allMessages() {
        return Collections.unmodifiableList(messages);
    }

    public List<ChatMessage> historyForApi() {
        int start = Math.max(0, messages.size() - API_HISTORY_LIMIT);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    public void clear() {
        messages.clear();
    }
}
