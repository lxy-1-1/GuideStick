package com.example.guidesticks_android;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {
    protected AppSettings appSettings;
    private TextToSpeech textToSpeech;
    private boolean ttsReady;
    private boolean destroyed;
    private String pendingSpeech;

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = new AppSettings(newBase).isLargeTextEnabled() ? 1.15f : 1.0f;
        super.attachBaseContext(newBase.createConfigurationContext(configuration));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        appSettings = new AppSettings(this);
        textToSpeech = new TextToSpeech(this, status -> {
            if (destroyed || textToSpeech == null) {
                return;
            }
            ttsReady = status == TextToSpeech.SUCCESS;
            if (!ttsReady) {
                return;
            }
            textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
            if (pendingSpeech != null && appSettings.isAutoSpeakEnabled()) {
                String message = pendingSpeech;
                pendingSpeech = null;
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH,
                        null, "pending-important-status");
            }
        });
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        applySystemBarInsets();
    }

    private void applySystemBarInsets() {
        View content = findViewById(android.R.id.content);
        int baseLeft = content.getPaddingLeft();
        int baseTop = content.getPaddingTop();
        int baseRight = content.getPaddingRight();
        int baseBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            SystemBarPadding.Values padding = SystemBarPadding.resolve(
                    baseLeft, baseTop, baseRight, baseBottom,
                    insets.left, insets.top, insets.right, insets.bottom);
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    protected void performAccessibleFeedback(View view) {
        if (appSettings != null && appSettings.isHapticsEnabled() && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    protected void performCriticalFeedback(View view) {
        if (appSettings != null && appSettings.isHapticsEnabled() && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    protected void announceStatus(View anchor, CharSequence message) {
        if (destroyed || anchor == null || message == null || message.length() == 0) {
            return;
        }
        anchor.announceForAccessibility(message);
    }

    protected void speakImportant(View anchor, CharSequence message) {
        if (destroyed || message == null || message.length() == 0) {
            return;
        }
        if (isTouchExplorationEnabled()) {
            if (anchor != null && ViewCompat.getAccessibilityLiveRegion(anchor)
                    == ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE) {
                announceStatus(anchor, message);
            }
            return;
        }
        if (appSettings == null || !appSettings.isAutoSpeakEnabled()) {
            return;
        }
        String spoken = message.toString();
        if (ttsReady && textToSpeech != null) {
            pendingSpeech = null;
            textToSpeech.speak(spoken, TextToSpeech.QUEUE_FLUSH,
                    null, "important-status");
        } else {
            pendingSpeech = spoken;
        }
    }

    protected void stopSpeaking() {
        pendingSpeech = null;
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    protected void markAccessibilityHeadings(int... viewIds) {
        for (int viewId : viewIds) {
            View view = findViewById(viewId);
            if (view != null) {
                ViewCompat.setAccessibilityHeading(view, true);
            }
        }
    }

    private boolean isTouchExplorationEnabled() {
        AccessibilityManager manager = (AccessibilityManager)
                getSystemService(ACCESSIBILITY_SERVICE);
        return manager != null && manager.isEnabled() && manager.isTouchExplorationEnabled();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        pendingSpeech = null;
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        super.onDestroy();
    }
}
