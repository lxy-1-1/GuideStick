package com.example.guidesticks_android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.example.guidesticks_android.api.RetrofitClient;
import com.example.guidesticks_android.model.DeviceStatusResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends BaseActivity {
    private static final long DEVICE_STATUS_REFRESH_MS = 5000L;

    private View serverStatusPanel;
    private TextView tvServerStatus;
    private View deviceStatusPanel;
    private TextView tvDeviceStatus;
    private TextView tvDeviceLastSeen;
    private View latestScenePanel;
    private TextView tvLatestScene;
    private TextView tvLatestSceneTime;
    private Call<ResponseBody> healthCall;
    private Call<DeviceStatusResponse> deviceStatusCall;
    private final Handler deviceStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable deviceStatusRefresh = new Runnable() {
        @Override
        public void run() {
            checkDeviceStatus();
            deviceStatusHandler.postDelayed(this, DEVICE_STATUS_REFRESH_MS);
        }
    };
    private String lastServerAnnouncement = "";
    private String lastDeviceAnnouncement = "";
    private Long lastDeviceSeenAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        markAccessibilityHeadings(
                R.id.headingBrand, R.id.headingFeatures, R.id.headingLatestScene);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        serverStatusPanel = findViewById(R.id.serverStatusPanel);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        deviceStatusPanel = findViewById(R.id.deviceStatusPanel);
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus);
        tvDeviceLastSeen = findViewById(R.id.tvDeviceLastSeen);
        latestScenePanel = findViewById(R.id.latestScenePanel);
        tvLatestScene = findViewById(R.id.tvLatestScene);
        tvLatestSceneTime = findViewById(R.id.tvLatestSceneTime);
        ImageButton refreshServer = findViewById(R.id.btnRefreshServer);

        bindAction(R.id.cardAssistant, AssistantActivity.class);
        bindAction(R.id.cardIdentify, IdentifyActivity.class);
        bindAction(R.id.cardFindStick, FindStickActivity.class);
        bindAction(R.id.cardEmergency, EmergencyActivity.class);
        refreshServer.setOnClickListener(view -> {
            performAccessibleFeedback(view);
            checkServerHealth();
            checkDeviceStatus();
        });

        BottomNavigationView navigation = findViewById(R.id.bottomNavigationView);
        navigation.setSelectedItemId(R.id.navigation_home);
        navigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                return true;
            }
            if (id == R.id.navigation_list) {
                startActivity(new Intent(this, ListActivity.class));
                finish();
                return true;
            }
            if (id == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void bindAction(int viewId, Class<?> destination) {
        findViewById(viewId).setOnClickListener(view -> {
            performAccessibleFeedback(view);
            startActivity(new Intent(this, destination));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLatestScene();
        checkServerHealth();
        checkDeviceStatus();
        deviceStatusHandler.removeCallbacks(deviceStatusRefresh);
        deviceStatusHandler.postDelayed(deviceStatusRefresh, DEVICE_STATUS_REFRESH_MS);
    }

    @Override
    protected void onPause() {
        deviceStatusHandler.removeCallbacks(deviceStatusRefresh);
        if (deviceStatusCall != null) {
            deviceStatusCall.cancel();
            deviceStatusCall = null;
        }
        super.onPause();
    }

    private void checkServerHealth() {
        if (healthCall != null) {
            healthCall.cancel();
        }
        tvServerStatus.setText(R.string.server_checking);
        serverStatusPanel.setBackgroundResource(R.drawable.bg_status_neutral);
        tvServerStatus.setContentDescription(AccessibilityTextFormatter.serverStatus(
                getString(R.string.server_checking)));
        Call<ResponseBody> call = RetrofitClient.getInstance(this).getApiService().health();
        healthCall = call;
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> request, Response<ResponseBody> response) {
                if (healthCall != request || isFinishing()) {
                    return;
                }
                healthCall = null;
                boolean online = response.isSuccessful();
                showServerStatus(online ? R.string.server_online : R.string.server_offline,
                        online ? R.drawable.bg_status_success : R.drawable.bg_status_warning);
            }

            @Override
            public void onFailure(Call<ResponseBody> request, Throwable error) {
                if (healthCall != request || request.isCanceled() || isFinishing()) {
                    return;
                }
                healthCall = null;
                showServerStatus(R.string.server_offline, R.drawable.bg_status_warning);
            }
        });
    }

    private void showServerStatus(int statusRes, int backgroundRes) {
        String status = getString(statusRes);
        String spoken = AccessibilityTextFormatter.serverStatus(status);
        tvServerStatus.setText(status);
        tvServerStatus.setContentDescription(spoken);
        serverStatusPanel.setBackgroundResource(backgroundRes);
        if (!spoken.equals(lastServerAnnouncement)) {
            lastServerAnnouncement = spoken;
            speakImportant(serverStatusPanel, spoken);
        }
    }

    private void checkDeviceStatus() {
        if (deviceStatusCall != null) {
            deviceStatusCall.cancel();
        }
        Call<DeviceStatusResponse> call = RetrofitClient.getInstance(this)
                .getApiService().deviceStatus();
        deviceStatusCall = call;
        call.enqueue(new Callback<DeviceStatusResponse>() {
            @Override
            public void onResponse(Call<DeviceStatusResponse> request,
                                   Response<DeviceStatusResponse> response) {
                if (deviceStatusCall != request || isFinishing()) {
                    return;
                }
                deviceStatusCall = null;
                DeviceStatusResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    showDeviceStatus(null, lastDeviceSeenAt);
                    return;
                }
                lastDeviceSeenAt = body.getLastSeenEpochMs();
                showDeviceStatus(body.isOnline(), lastDeviceSeenAt);
            }

            @Override
            public void onFailure(Call<DeviceStatusResponse> request, Throwable error) {
                if (deviceStatusCall != request || request.isCanceled() || isFinishing()) {
                    return;
                }
                deviceStatusCall = null;
                showDeviceStatus(null, lastDeviceSeenAt);
            }
        });
    }

    private void showDeviceStatus(Boolean online, Long lastSeenAt) {
        String status = getString(DeviceStatusPresentation.statusRes(online));
        String lastSeen;
        if (lastSeenAt == null) {
            lastSeen = getString(R.string.device_never_seen);
        } else {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date(lastSeenAt));
            lastSeen = getString(R.string.device_last_seen, time);
        }

        tvDeviceStatus.setText(status);
        tvDeviceLastSeen.setText(lastSeen);
        deviceStatusPanel.setBackgroundResource(
                DeviceStatusPresentation.backgroundRes(online));

        String spoken = status + ". " + lastSeen;
        deviceStatusPanel.setContentDescription(spoken);
        if (!status.equals(lastDeviceAnnouncement)) {
            lastDeviceAnnouncement = status;
            speakImportant(deviceStatusPanel, spoken);
        }
    }

    private void updateLatestScene() {
        String light = appSettings.getLastLight();
        if (light.isEmpty()) {
            latestScenePanel.setBackgroundResource(R.drawable.bg_status_neutral);
            tvLatestScene.setText(R.string.latest_scene_empty);
            tvLatestSceneTime.setVisibility(View.GONE);
            latestScenePanel.setContentDescription(getString(R.string.latest_scene_empty));
            return;
        }

        String message = appSettings.getLastMessage();
        if (message.isEmpty()) {
            message = sceneFallback(light);
        }
        tvLatestScene.setText(message);
        int background;
        if ("red".equalsIgnoreCase(light)) {
            background = R.drawable.bg_status_danger;
        } else if ("green".equalsIgnoreCase(light)) {
            background = R.drawable.bg_status_success;
        } else {
            background = R.drawable.bg_status_warning;
        }
        latestScenePanel.setBackgroundResource(background);

        long timestamp = appSettings.getLastRecognitionTime();
        if (timestamp > 0L) {
            String time = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
            tvLatestSceneTime.setText(getString(R.string.latest_scene_time, time));
            tvLatestSceneTime.setVisibility(View.VISIBLE);
            latestScenePanel.setContentDescription(
                    AccessibilityTextFormatter.recognition(
                            message, appSettings.getLastCommand())
                            + getString(R.string.latest_scene_time, time) + "。");
        } else {
            latestScenePanel.setContentDescription(
                    AccessibilityTextFormatter.recognition(
                            message, appSettings.getLastCommand()));
        }
    }

    private String sceneFallback(String light) {
        if ("red".equalsIgnoreCase(light)) {
            return getString(R.string.recognition_red);
        }
        if ("green".equalsIgnoreCase(light)) {
            return getString(R.string.recognition_green);
        }
        return getString(R.string.recognition_unknown);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                    .remove("token")
                    .remove("username")
                    .remove("email")
                    .apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (healthCall != null) {
            healthCall.cancel();
            healthCall = null;
        }
        deviceStatusHandler.removeCallbacks(deviceStatusRefresh);
        if (deviceStatusCall != null) {
            deviceStatusCall.cancel();
            deviceStatusCall = null;
        }
        super.onDestroy();
    }
}
