package com.example.guidesticks_android.model;

public class IdentifyResponse {
    private String light;
    private String command;
    private String message;
    private String source;
    private String error;

    public String getLight() {
        return light;
    }

    public String getError() {
        return error;
    }

    public String getCommand() {
        return command;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }
}
