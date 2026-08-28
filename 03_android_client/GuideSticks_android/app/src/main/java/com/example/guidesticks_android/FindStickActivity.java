package com.example.guidesticks_android;

import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.example.guidesticks_android.api.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindStickActivity extends BaseActivity {
    private TextView tvStatus;
    private CircularProgressIndicator progress;
    private MaterialButton btnRetry;
    private Call<Void> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_stick);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        tvStatus = findViewById(R.id.tvStatus);
        progress = findViewById(R.id.progressBar);
        btnRetry = findViewById(R.id.btnRetry);

        btnRetry.setOnClickListener(view -> {
            performAccessibleFeedback(view);
            sendFindSignal();
        });
        findViewById(R.id.btnCancel).setOnClickListener(view -> finish());
        sendFindSignal();
    }

    private void sendFindSignal() {
        if (activeCall != null) {
            activeCall.cancel();
        }
        tvStatus.setText(R.string.find_sending);
        tvStatus.setContentDescription(getString(R.string.find_sending));
        speakImportant(tvStatus, getString(R.string.find_sending));
        progress.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);

        Call<Void> call = RetrofitClient.getInstance(this).getApiService().findStick();
        activeCall = call;
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> request, Response<Void> response) {
                if (activeCall != request || isFinishing()) {
                    return;
                }
                activeCall = null;
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    tvStatus.setText(R.string.find_sent);
                    tvStatus.setContentDescription(getString(R.string.find_sent));
                    performAccessibleFeedback(tvStatus);
                    speakImportant(tvStatus, getString(R.string.find_sent));
                } else {
                    showFailure();
                }
            }

            @Override
            public void onFailure(Call<Void> request, Throwable error) {
                if (activeCall != request || request.isCanceled() || isFinishing()) {
                    return;
                }
                activeCall = null;
                showFailure();
            }
        });
    }

    private void showFailure() {
        progress.setVisibility(View.GONE);
        tvStatus.setText(R.string.find_failed_detail);
        tvStatus.setContentDescription(getString(R.string.find_failed_detail));
        btnRetry.setVisibility(View.VISIBLE);
        speakImportant(tvStatus, getString(R.string.find_failed_detail));
        btnRetry.post(() -> btnRetry.sendAccessibilityEvent(
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED));
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
        super.onDestroy();
    }
}
