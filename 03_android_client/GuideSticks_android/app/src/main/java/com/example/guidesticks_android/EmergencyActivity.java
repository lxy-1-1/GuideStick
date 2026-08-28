package com.example.guidesticks_android;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyActivity extends BaseActivity {
    private static final int REQUEST_EMERGENCY_PERMISSIONS = 1;
    private static final long POLL_INTERVAL_MS = 5000L;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_EMERGENCY_PHONE = "emergency_phone";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView tvPhoneNumber;
    private TextView tvStatus;
    private android.view.View statusPanel;
    private SharedPreferences preferences;
    private LocationManager locationManager;
    private boolean destroyed;
    private boolean pollInFlight;
    private boolean callLaunched;
    private boolean waitingPermission;
    private boolean emergencyInProgress;
    private String pendingPhone = "";
    private String lastStatusMessage = "";

    private final Runnable pollTask = this::pollEmergencySignal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);
        markAccessibilityHeadings(R.id.headingEmergencyContact);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        tvPhoneNumber = findViewById(R.id.tvEmergencyPhone);
        tvStatus = findViewById(R.id.tvStatus);
        statusPanel = findViewById(R.id.emergencyStatusPanel);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        findViewById(R.id.btnManageContact).setOnClickListener(view -> {
            performAccessibleFeedback(view);
            startActivity(new Intent(this, ProfileActivity.class));
        });
        findViewById(R.id.btnCall).setOnClickListener(view -> {
            performCriticalFeedback(view);
            beginEmergency(savedPhone());
        });

        refreshContact();
        handler.post(pollTask);
    }

    private void beginEmergency(String phone) {
        if (TextUtils.isEmpty(phone)) {
            showStatus(R.string.emergency_contact_missing, false);
            startActivity(new Intent(this, ProfileActivity.class));
            return;
        }
        if (emergencyInProgress || waitingPermission) {
            return;
        }

        pendingPhone = phone;
        List<String> missingPermissions = missingEmergencyPermissions();
        if (!missingPermissions.isEmpty()) {
            waitingPermission = true;
            showStatus(R.string.emergency_permissions_required, false);
            ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toArray(new String[0]),
                    REQUEST_EMERGENCY_PERMISSIONS);
            return;
        }
        dispatchEmergency(phone);
    }

    private List<String> missingEmergencyPermissions() {
        List<String> permissions = new ArrayList<>();
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            permissions.add(Manifest.permission.CALL_PHONE);
        }
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if (!hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return permissions;
    }

    private void dispatchEmergency(String phone) {
        emergencyInProgress = true;
        handler.removeCallbacks(pollTask);

        boolean smsQueued = sendEmergencySms(phone);
        boolean callStarted = startCall(phone);

        if (smsQueued && callStarted) {
            showStatus(R.string.emergency_action_started, true);
        } else if (callStarted) {
            showStatus(R.string.emergency_call_only, false);
        } else if (smsQueued) {
            showStatus(R.string.emergency_sms_only, false);
            emergencyInProgress = false;
            scheduleNextPoll();
        } else {
            showStatus(R.string.emergency_action_unavailable, false);
            emergencyInProgress = false;
            scheduleNextPoll();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean sendEmergencySms(String phone) {
        if (!hasPermission(Manifest.permission.SEND_SMS)
                || !getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            return false;
        }

        Location location = bestLastKnownLocation();
        String message = location == null
                ? EmergencyMessageBuilder.withoutLocation()
                : EmergencyMessageBuilder.withLocation(
                location.getLatitude(), location.getLongitude(), location.getAccuracy());

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts.size() <= 1) {
                smsManager.sendTextMessage(phone, null, message, null, null);
            } else {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
            }
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private Location bestLastKnownLocation() {
        if (locationManager == null || !hasLocationPermission()) {
            return null;
        }

        Location best = null;
        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                best = chooseBetter(
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), best);
            }
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                best = chooseBetter(
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), best);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return best;
    }

    private Location chooseBetter(Location candidate, Location current) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.getTime() > current.getTime() + 120000L) {
            return candidate;
        }
        return candidate.getAccuracy() < current.getAccuracy() ? candidate : current;
    }

    private boolean startCall(String phone) {
        if (!hasPermission(Manifest.permission.CALL_PHONE)
                || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return false;
        }
        try {
            callLaunched = true;
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
            return true;
        } catch (RuntimeException error) {
            callLaunched = false;
            return false;
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private void pollEmergencySignal() {
        if (destroyed || pollInFlight || waitingPermission || emergencyInProgress
                || callLaunched) {
            scheduleNextPoll();
            return;
        }
        String phone = savedPhone();
        if (TextUtils.isEmpty(phone)) {
            showStatus(R.string.emergency_contact_missing, false);
            scheduleNextPoll();
            return;
        }

        pollInFlight = true;
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(appSettings.getServerUrl() + "android/iscall/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                int code = connection.getResponseCode();
                if (code != 200) {
                    postPollResult(false, false, phone);
                    return;
                }
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }
                JSONObject object = new JSONObject(body.toString());
                Object value = object.opt("isCall");
                boolean shouldCall = Boolean.TRUE.equals(value)
                        || (value instanceof Number && ((Number) value).intValue() == 1)
                        || "1".equals(String.valueOf(value));
                postPollResult(true, shouldCall, phone);
            } catch (Exception ignored) {
                postPollResult(false, false, phone);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void postPollResult(boolean connected, boolean shouldCall, String phone) {
        handler.post(() -> {
            pollInFlight = false;
            if (destroyed) {
                return;
            }
            if (!connected) {
                showStatus(R.string.emergency_request_failed, false);
                scheduleNextPoll();
                return;
            }
            if (shouldCall) {
                showStatus(R.string.emergency_signal_detected, true);
                beginEmergency(phone);
                return;
            }
            showStatus(R.string.emergency_waiting, true);
            scheduleNextPoll();
        });
    }

    private void scheduleNextPoll() {
        handler.removeCallbacks(pollTask);
        if (!destroyed) {
            handler.postDelayed(pollTask, POLL_INTERVAL_MS);
        }
    }

    private void showStatus(int stringRes, boolean success) {
        String status = getString(stringRes);
        tvStatus.setText(status);
        statusPanel.setContentDescription(status);
        statusPanel.setBackgroundResource(success
                ? R.drawable.bg_status_success : R.drawable.bg_status_warning);
        if (!status.equals(lastStatusMessage)) {
            lastStatusMessage = status;
            speakImportant(statusPanel, status);
        }
    }

    private String savedPhone() {
        return preferences.getString(KEY_EMERGENCY_PHONE, "").trim();
    }

    private void refreshContact() {
        String phone = savedPhone();
        if (TextUtils.isEmpty(phone)) {
            tvPhoneNumber.setText(R.string.emergency_contact_not_set);
        } else {
            tvPhoneNumber.setText(phone);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null) {
            refreshContact();
        }
        if (callLaunched) {
            callLaunched = false;
            emergencyInProgress = false;
            showStatus(R.string.emergency_waiting, true);
            scheduleNextPoll();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_EMERGENCY_PERMISSIONS) {
            return;
        }
        waitingPermission = false;
        if (TextUtils.isEmpty(pendingPhone)) {
            showStatus(R.string.emergency_contact_missing, false);
            scheduleNextPoll();
        } else if (missingEmergencyPermissions().isEmpty()) {
            dispatchEmergency(pendingPhone);
        } else {
            showStatus(R.string.emergency_permissions_denied, false);
            scheduleNextPoll();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }
}
