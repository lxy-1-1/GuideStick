package com.example.guidesticks_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends BaseActivity {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_EMERGENCY_PHONE = "emergency_phone";

    private SharedPreferences preferences;
    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvEmergencyPhone;
    private View profileInfoPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        markAccessibilityHeadings(R.id.headingAccountSafety, R.id.headingAppSettings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        tvUsername = findViewById(R.id.tvProfileUsername);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvEmergencyPhone = findViewById(R.id.tvEmergencyPhone);
        profileInfoPanel = findViewById(R.id.profileInfoPanel);
        View btnEmergencySetting = findViewById(R.id.btnEmergencySetting);
        View btnSettings = findViewById(R.id.btnSettings);
        View btnAbout = findViewById(R.id.btnAbout);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        btnEmergencySetting.setOnClickListener(v -> {
            performAccessibleFeedback(v);
            showEmergencyPhoneDialog();
        });
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        btnLogout.setOnClickListener(v -> logout(v));

        configureBottomNavigation();
        refreshProfileInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProfileInfo();
    }

    private void configureBottomNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottomNavigationView);
        navigation.setSelectedItemId(R.id.navigation_profile);
        navigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            }
            if (itemId == R.id.navigation_list) {
                startActivity(new Intent(this, ListActivity.class));
                finish();
                return true;
            }
            return itemId == R.id.navigation_profile;
        });
    }

    private void refreshProfileInfo() {
        if (preferences == null || tvUsername == null) return;
        String username = preferences.getString(KEY_USERNAME, getString(R.string.not_logged_in));
        String email = preferences.getString(KEY_EMAIL, getString(R.string.email_not_set));
        tvUsername.setText(username);
        tvEmail.setText(email);
        String phone = preferences.getString(KEY_EMERGENCY_PHONE, "");
        tvEmergencyPhone.setText(TextUtils.isEmpty(phone)
                ? getString(R.string.contact_not_set)
                : getString(R.string.contact_value, phone));
        profileInfoPanel.setContentDescription(getString(
                R.string.profile_summary,
                username,
                email,
                getString(TextUtils.isEmpty(phone)
                        ? R.string.contact_status_not_set : R.string.contact_status_set)));
    }

    private void showEmergencyPhoneDialog() {
        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_emergency_contact, null, false);
        EditText input = dialogView.findViewById(R.id.etEmergencyPhone);
        input.setText(preferences.getString(KEY_EMERGENCY_PHONE, ""));
        input.setSelection(input.length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.set_emergency_contact)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String phone = input.getText().toString().trim();
                    if (TextUtils.isEmpty(phone)) {
                        preferences.edit().remove(KEY_EMERGENCY_PHONE).apply();
                        Toast.makeText(this, R.string.contact_cleared, Toast.LENGTH_SHORT).show();
                    } else {
                        preferences.edit().putString(KEY_EMERGENCY_PHONE, phone).apply();
                        Toast.makeText(this, R.string.contact_saved, Toast.LENGTH_SHORT).show();
                    }
                    refreshProfileInfo();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void logout(View source) {
        performAccessibleFeedback(source);
        preferences.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .apply();
        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
