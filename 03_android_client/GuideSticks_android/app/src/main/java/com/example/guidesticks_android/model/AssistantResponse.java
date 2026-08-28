package com.example.guidesticks_android.model;

public class AssistantResponse {
    private String answer;
    private String intent;
    private Scene scene;
    private String source;

    public String getAnswer() {
        return answer;
    }

    public String getIntent() {
        return intent;
    }

    public Scene getScene() {
        return scene;
    }

    public String getSource() {
        return source;
    }

    public static class Scene {
        private String light;
        private int age_seconds;
        private boolean fresh;

        public String getLight() {
            return light;
        }

        public int getAgeSeconds() {
            return age_seconds;
        }

        public boolean isFresh() {
            return fresh;
        }
    }
}
