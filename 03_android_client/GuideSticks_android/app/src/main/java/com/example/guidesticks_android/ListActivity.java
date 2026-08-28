package com.example.guidesticks_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.example.guidesticks_android.api.RetrofitClient;
import com.example.guidesticks_android.model.SensorContextRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListActivity extends BaseActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final long LOCATION_TIMEOUT_MS = 10000L;
    private static final long MAP_LOAD_TIMEOUT_MS = 12000L;

    private TextView tvAddress;
    private TextView tvCoordinates;
    private TextView tvAccuracy;
    private TextView tvLocationTime;
    private TextView tvMapStatus;
    private ProgressBar mapProgress;
    private View mapStatusOverlay;
    private WebView mapWebView;
    private MaterialButton btnOpenMap;
    private MaterialButton btnSpeakLocation;
    private View locationInfoPanel;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentLocation;
    private Call<ResponseBody> sensorContextCall;
    private boolean hasLocationResult;
    private boolean mapLoadFailed;
    private final Runnable mapLoadTimeout = this::showMapLoadError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tvAddress = findViewById(R.id.tvList);
        tvCoordinates = findViewById(R.id.tvCoordinates);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvLocationTime = findViewById(R.id.tvLocationTime);
        tvMapStatus = findViewById(R.id.tvMapStatus);
        mapProgress = findViewById(R.id.mapProgress);
        mapStatusOverlay = findViewById(R.id.mapStatusOverlay);
        mapWebView = findViewById(R.id.mapWebView);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnSpeakLocation = findViewById(R.id.btnSpeakLocation);
        locationInfoPanel = findViewById(R.id.locationInfoPanel);
        MaterialButton btnRefreshLocation = findViewById(R.id.btnRefreshLocation);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        configureMap();
        btnRefreshLocation.setOnClickListener(v -> {
            performAccessibleFeedback(v);
            getLocation();
        });
        btnOpenMap.setOnClickListener(v -> {
            performAccessibleFeedback(v);
            openCurrentLocation();
        });
        btnSpeakLocation.setOnClickListener(v -> {
            performAccessibleFeedback(v);
            speakCurrentLocation();
        });
        configureBottomNavigation();
        getLocation();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureMap() {
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setGeolocationEnabled(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mapLoadFailed = false;
                showMapStatus(R.string.map_loading, true);
                scheduleMapLoadTimeout();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!mapLoadFailed) {
                    cancelMapLoadTimeout();
                    Uri loadedUri = Uri.parse(url);
                    if (MapPageSanitizer.supports(
                            loadedUri.getScheme(), loadedUri.getHost())) {
                        view.evaluateJavascript(MapPageSanitizer.script(), null);
                    }
                    mapStatusOverlay.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view,
                                                    WebResourceRequest request) {
                return !isAllowedMapUri(request.getUrl());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    showMapLoadError();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
                    showMapLoadError();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                           SslError error) {
                handler.cancel();
                showMapLoadError();
            }
        });
    }

    private boolean isAllowedMapUri(Uri uri) {
        return uri != null && MapUrlBuilder.isAllowedMapUri(uri.getScheme(), uri.getHost());
    }

    private void configureBottomNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottomNavigationView);
        navigation.setSelectedItemId(R.id.navigation_list);
        navigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            }
            if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return itemId == R.id.navigation_list;
        });
    }

    private void getLocation() {
        setLocationMessage(R.string.location_getting);
        boolean hasFine = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasFine && !hasCoarse) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
            return;
        }
        if (locationManager == null) {
            setLocationMessage(R.string.location_failed);
            return;
        }

        boolean gpsEnabled;
        boolean networkEnabled;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (RuntimeException error) {
            setLocationMessage(R.string.location_failed);
            return;
        }
        if (!gpsEnabled && !networkEnabled) {
            setLocationMessage(R.string.location_disabled);
            return;
        }

        Location lastKnown = getBestLastKnownLocation(gpsEnabled, networkEnabled);
        if (lastKnown != null) {
            hasLocationResult = true;
            showLocation(lastKnown);
        } else {
            requestSingleLocationUpdate(gpsEnabled, networkEnabled);
        }
    }

    private Location getBestLastKnownLocation(boolean gpsEnabled, boolean networkEnabled) {
        Location best = null;
        try {
            if (gpsEnabled && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                best = chooseBetter(
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), best);
            }
            if (networkEnabled && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                best = chooseBetter(
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), best);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        if (best == null
                || !LocationFreshness.isFresh(best.getTime(), System.currentTimeMillis())) {
            return null;
        }
        return best;
    }

    private Location chooseBetter(Location candidate, Location best) {
        if (candidate == null) return best;
        if (best == null) return candidate;
        if (candidate.getTime() > best.getTime() + 120000L) return candidate;
        return candidate.getAccuracy() < best.getAccuracy() ? candidate : best;
    }

    private void requestSingleLocationUpdate(boolean gpsEnabled, boolean networkEnabled) {
        removeLocationUpdates();
        hasLocationResult = false;
        locationListener = location -> {
            if (location == null || hasLocationResult) return;
            hasLocationResult = true;
            removeLocationUpdates();
            showLocation(location);
        };

        try {
            if (networkEnabled && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        0L, 0f, locationListener);
            }
            if (gpsEnabled && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0L, 0f, locationListener);
            }
        } catch (RuntimeException error) {
            setLocationMessage(R.string.location_failed);
            return;
        }

        tvAddress.postDelayed(() -> {
            if (!hasLocationResult) {
                removeLocationUpdates();
                setLocationMessage(R.string.location_failed);
            }
        }, LOCATION_TIMEOUT_MS);
    }

    private void showLocation(Location location) {
        currentLocation = location;
        btnOpenMap.setEnabled(true);
        btnSpeakLocation.setEnabled(true);
        loadMap(location);

        tvCoordinates.setText(getString(R.string.location_coordinates_value,
                location.getLatitude(), location.getLongitude()));
        String accuracy = getString(R.string.location_accuracy, location.getAccuracy());
        tvAccuracy.setText(accuracy);
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(
                new Date(location.getTime()));
        String updated = getString(R.string.location_updated, time);
        tvLocationTime.setText(updated);

        String resolvedAddress = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()
                    && addresses.get(0).getAddressLine(0) != null) {
                resolvedAddress = addresses.get(0).getAddressLine(0);
                tvAddress.setText(resolvedAddress);
            } else {
                tvAddress.setText(R.string.location_address_failed);
            }
        } catch (IOException | IllegalArgumentException error) {
            tvAddress.setText(R.string.location_address_failed);
        }
        updateLocationSummary(true);
        uploadSensorContext(location, resolvedAddress);
    }

    private void uploadSensorContext(Location location, String address) {
        if (!LocationFreshness.isFresh(location.getTime(), System.currentTimeMillis())) {
            return;
        }
        if (sensorContextCall != null) {
            sensorContextCall.cancel();
        }
        SensorContextRequest request = new SensorContextRequest(
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                address,
                location.getTime()
        );
        Call<ResponseBody> call = RetrofitClient.getInstance(this)
                .getApiService()
                .postSensorContext(request);
        sensorContextCall = call;
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> requestCall,
                                   Response<ResponseBody> response) {
                if (sensorContextCall == requestCall) {
                    sensorContextCall = null;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> requestCall, Throwable error) {
                if (sensorContextCall == requestCall) {
                    sensorContextCall = null;
                }
            }
        });
    }

    private void speakCurrentLocation() {
        if (currentLocation == null) {
            return;
        }
        speakImportant(btnSpeakLocation, updateLocationSummary(false));
    }

    private String updateLocationSummary(boolean announce) {
        String spoken = AccessibilityTextFormatter.location(
                tvAddress.getText().toString(),
                tvAccuracy.getText().toString(),
                tvLocationTime.getText().toString());
        locationInfoPanel.setContentDescription(spoken);
        if (announce) {
            speakImportant(locationInfoPanel, spoken);
        }
        return spoken;
    }

    private void loadMap(Location location) {
        mapLoadFailed = false;
        showMapStatus(R.string.map_loading, true);
        scheduleMapLoadTimeout();
        mapWebView.loadUrl(MapUrlBuilder.embed(
                location.getLatitude(), location.getLongitude()));
    }

    private void showMapStatus(int stringRes, boolean showProgress) {
        mapProgress.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        tvMapStatus.setText(stringRes);
        mapStatusOverlay.setVisibility(View.VISIBLE);
    }

    private void showMapLoadError() {
        cancelMapLoadTimeout();
        mapLoadFailed = true;
        showMapStatus(R.string.map_unavailable, false);
    }

    private void scheduleMapLoadTimeout() {
        cancelMapLoadTimeout();
        mapWebView.postDelayed(mapLoadTimeout, MAP_LOAD_TIMEOUT_MS);
    }

    private void cancelMapLoadTimeout() {
        if (mapWebView != null) {
            mapWebView.removeCallbacks(mapLoadTimeout);
        }
    }

    private void openCurrentLocation() {
        Location location = currentLocation;
        if (location == null) {
            Toast.makeText(this, R.string.location_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String coordinates = String.format(
                Locale.US, "%.6f,%.6f", location.getLatitude(), location.getLongitude());
        String label = Uri.encode(getString(R.string.current_location_marker));
        Uri geoUri = Uri.parse("geo:" + coordinates + "?q=" + coordinates + "(" + label + ")");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, geoUri));
        } catch (ActivityNotFoundException noMapApp) {
            try {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(MapUrlBuilder.browser(
                                location.getLatitude(), location.getLongitude()))));
            } catch (ActivityNotFoundException noBrowser) {
                Toast.makeText(this, R.string.map_unavailable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setLocationMessage(int stringRes) {
        currentLocation = null;
        btnOpenMap.setEnabled(false);
        btnSpeakLocation.setEnabled(false);
        tvAddress.setText(stringRes);
        tvCoordinates.setText("—");
        tvAccuracy.setText("");
        tvLocationTime.setText("");
        String spoken = getString(stringRes);
        locationInfoPanel.setContentDescription(spoken);
        speakImportant(locationInfoPanel, spoken);
        showMapStatus(R.string.map_waiting_location, false);
    }

    private void removeLocationUpdates() {
        if (locationManager == null || locationListener == null) return;
        try {
            locationManager.removeUpdates(locationListener);
        } catch (RuntimeException ignored) {
        }
        locationListener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION_PERMISSION) return;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                getLocation();
                return;
            }
        }
        setLocationMessage(R.string.location_permission_required);
        Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        removeLocationUpdates();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (sensorContextCall != null) {
            sensorContextCall.cancel();
            sensorContextCall = null;
        }
        if (mapWebView != null) {
            cancelMapLoadTimeout();
            mapWebView.stopLoading();
            mapWebView.destroy();
        }
        super.onDestroy();
    }
}
