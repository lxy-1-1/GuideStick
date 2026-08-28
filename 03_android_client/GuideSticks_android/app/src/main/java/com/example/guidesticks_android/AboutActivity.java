package com.example.guidesticks_android;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        markAccessibilityHeadings(
                R.id.headingAboutTeam, R.id.headingCapabilities, R.id.headingPrivacy);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
}
