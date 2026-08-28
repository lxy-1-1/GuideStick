package com.example.guidesticks_android.model;

import java.util.List;

public class AssistantRequest {
    private final String question;
    private final List<ChatMessage> history;

    public AssistantRequest(String question, List<ChatMessage> history) {
        this.question = question;
        this.history = history;
    }

    public String getQuestion() {
        return question;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }
}
