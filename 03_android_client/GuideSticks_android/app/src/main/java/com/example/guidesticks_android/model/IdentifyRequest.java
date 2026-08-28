package com.example.guidesticks_android.model;

public class IdentifyRequest {
    private final String image;

    public IdentifyRequest(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }
}

