package com.example.guidesticks_android;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.guidesticks_android.api.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends BaseActivity {
    private final OkHttpClient healthClient = new OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build();

    private TextInputLayout layoutServerAddress;
    private TextInputEditText etServerAddress;
    private MaterialButton btnTestServer;
    private TextView tvServerTestStatus;
    private Call healthCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        markAccessibilityHeadings(
                R.id.headingAccessibilitySettings, R.id.headingServerSettings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SwitchMaterial switchLargeText = findViewById(R.id.switchLargeText);
        SwitchMaterial switchAutoSpeak = findViewById(R.id.switchAutoSpeak);
        SwitchMaterial switchHaptics = findViewById(R.id.switchHaptics);
        layoutServerAddress = findViewById(R.id.layoutServerAddress);
        etServerAddress = findViewById(R.id.etServerAddress);
        MaterialButton btnSaveServer = findViewById(R.id.btnSaveServer);
        btnTestServer = findViewById(R.id.btnTestServer);
        tvServerTestStatus = findViewById(R.id.tvServerTestStatus);

        switchLargeText.setChecked(appSettings.isLargeTextEnabled());
        switchAutoSpeak.setChecked(appSettings.isAutoSpeakEnabled());
        switchHaptics.setChecked(appSettings.isHapticsEnabled());
        etServerAddress.setText(appSettings.getServerUrl());

        switchLargeText.setOnCheckedChangeListener((button, enabled) -> {
            appSettings.setLargeTextEnabled(enabled);
            performAccessibleFeedback(button);
            button.post(this::recreate);
        });
        switchAutoSpeak.setOnCheckedChangeListener((button, enabled) -> {
            appSettings.setAutoSpeakEnabled(enabled);
            performAccessibleFeedback(button);
            Toast.makeText(this, R.string.setting_applied, Toast.LENGTH_SHORT).show();
        });
        switchHaptics.setOnCheckedChangeListener((button, enabled) -> {
            if (enabled) {
                appSettings.setHapticsEnabled(true);
                performAccessibleFeedback(button);
            } else {
                performAccessibleFeedback(button);
                appSettings.setHapticsEnabled(false);
            }
        });

        btnSaveServer.setOnClickListener(v -> saveServerAddress(v));
        btnTestServer.setOnClickListener(v -> testServerAddress(v));
    }

    private void saveServerAddress(View source) {
        String normalized = normalizedInputOrShowError();
        if (normalized == null) {
            return;
        }
        appSettings.setServerUrl(normalized);
        RetrofitClient.reset();
        etServerAddress.setText(normalized);
        layoutServerAddress.setError(null);
        performAccessibleFeedback(source);
        Toast.makeText(this, R.string.server_saved, Toast.LENGTH_SHORT).show();
        announceStatus(layoutServerAddress, getString(R.string.server_saved));
    }

    private void testServerAddress(View source) {
        String normalized = normalizedInputOrShowError();
        if (normalized == null) {
            return;
        }
        if (healthCall != null) {
            healthCall.cancel();
        }
        performAccessibleFeedback(source);
        setTesting(true);
        Request request = new Request.Builder()
                .url(normalized + "android/test/")
                .get()
                .build();
        healthCall = healthClient.newCall(request);
        healthCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!call.isCanceled()) {
                    runOnUiThread(() -> showTestResult(false));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                boolean success = response.isSuccessful();
                response.close();
                runOnUiThread(() -> showTestResult(success));
            }
        });
    }

    private String normalizedInputOrShowError() {
        try {
            String raw = etServerAddress.getText() == null
                    ? "" : etServerAddress.getText().toString();
            String normalized = ServerAddress.normalize(raw);
            layoutServerAddress.setError(null);
            return normalized;
        } catch (IllegalArgumentException error) {
            layoutServerAddress.setError(getString(R.string.server_invalid));
            return null;
        }
    }

    private void setTesting(boolean testing) {
        btnTestServer.setEnabled(!testing);
        tvServerTestStatus.setVisibility(View.VISIBLE);
        tvServerTestStatus.setBackgroundResource(R.drawable.bg_status_neutral);
        tvServerTestStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvServerTestStatus.setText(R.string.server_testing);
    }

    private void showTestResult(boolean success) {
        btnTestServer.setEnabled(true);
        tvServerTestStatus.setVisibility(View.VISIBLE);
        tvServerTestStatus.setBackgroundResource(success
                ? R.drawable.bg_status_success : R.drawable.bg_status_danger);
        tvServerTestStatus.setTextColor(ContextCompat.getColor(this,
                success ? R.color.success : R.color.danger));
        tvServerTestStatus.setText(success
                ? R.string.server_test_success : R.string.server_test_failed);
        speakImportant(tvServerTestStatus, tvServerTestStatus.getText());
    }

    @Override
    protected void onDestroy() {
        if (healthCall != null) {
            healthCall.cancel();
        }
        super.onDestroy();
    }
}
