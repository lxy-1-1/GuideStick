package com.example.guidesticks_android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.guidesticks_android.api.RetrofitClient;
import com.example.guidesticks_android.model.IdentifyRequest;
import com.example.guidesticks_android.model.IdentifyResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IdentifyActivity extends BaseActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private TextureView textureView;
    private FloatingActionButton fabCapture;
    private TextView tvResult;
    private TextView tvCommand;
    private TextView tvCameraStatus;
    private View resultPanel;
    private CircularProgressIndicator progressRecognition;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private ExecutorService executorService;
    private Call<IdentifyResponse> activeCall;
    private boolean processing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);
        markAccessibilityHeadings(R.id.headingRecognition);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        textureView = findViewById(R.id.textureView);
        fabCapture = findViewById(R.id.fabCapture);
        tvResult = findViewById(R.id.tvResult);
        tvCommand = findViewById(R.id.tvCommand);
        tvCameraStatus = findViewById(R.id.tvCameraStatus);
        resultPanel = findViewById(R.id.resultPanel);
        progressRecognition = findViewById(R.id.progressRecognition);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });

        fabCapture.setOnClickListener(v -> takePicture());
        executorService = Executors.newSingleThreadExecutor();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    Toast.makeText(IdentifyActivity.this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void createCameraPreviewSession() {
        if (cameraDevice == null || !textureView.isAvailable()) {
            return;
        }
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                return;
            }

            texture.setDefaultBufferSize(1280, 720);
            Surface previewSurface = new Surface(texture);

            imageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image == null) {
                    return;
                }
                executorService.execute(() -> processCapturedImage(image));
            }, backgroundHandler);

            CaptureRequest.Builder previewRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }
                            captureSession = session;
                            try {
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                );
                                captureSession.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        null,
                                        backgroundHandler
                                );
                                runOnUiThread(() -> {
                                    tvCameraStatus.setText(R.string.camera_ready);
                                    speakImportant(tvCameraStatus,
                                            getString(R.string.camera_ready));
                                });
                            } catch (CameraAccessException e) {
                                Toast.makeText(IdentifyActivity.this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(IdentifyActivity.this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
                        }
                    },
                    backgroundHandler
            );
        } catch (CameraAccessException e) {
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void takePicture() {
        if (processing || cameraDevice == null || captureSession == null || imageReader == null) {
            return;
        }
        setProcessing(true);
        try {
            CaptureRequest.Builder captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            );

            captureSession.stopRepeating();
            captureSession.capture(
                    captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            createCameraPreviewSession();
                        }
                    },
                    backgroundHandler
            );
            tvResult.setText(R.string.recognition_processing);
            tvCommand.setVisibility(View.GONE);
            resultPanel.setContentDescription(getString(R.string.recognition_processing));
            speakImportant(resultPanel, getString(R.string.recognition_processing));
        } catch (CameraAccessException e) {
            setProcessing(false);
            Toast.makeText(this, R.string.recognition_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void processCapturedImage(Image image) {
        byte[] jpegBytes;
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            jpegBytes = new byte[buffer.remaining()];
            buffer.get(jpegBytes);
        } finally {
            image.close();
        }

        String base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
        IdentifyRequest request = new IdentifyRequest(base64Image);

        Call<IdentifyResponse> call = RetrofitClient.getInstance(this)
                .getApiService().identify(request);
        activeCall = call;
        call.enqueue(new Callback<IdentifyResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<IdentifyResponse> call, @NonNull Response<IdentifyResponse> response) {
                        if (activeCall != call || isFinishing()) {
                            return;
                        }
                        activeCall = null;
                        setProcessing(false);
                        if (!response.isSuccessful() || response.body() == null) {
                            showRecognitionError();
                            return;
                        }
                        IdentifyResponse body = response.body();
                        if (body.getError() != null && !body.getError().isEmpty()) {
                            showRecognitionError();
                            return;
                        }
                        showRecognitionResult(body);
                    }

                    @Override
                    public void onFailure(@NonNull Call<IdentifyResponse> call, @NonNull Throwable t) {
                        if (activeCall != call || call.isCanceled() || isFinishing()) {
                            return;
                        }
                        activeCall = null;
                        setProcessing(false);
                        showRecognitionError();
                    }
                });
    }

    private void showRecognitionResult(IdentifyResponse response) {
        String light = response.getLight();
        String message = response.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = formatLightResult(light);
        }
        tvResult.setText(message);
        if (response.getCommand() != null && !response.getCommand().isEmpty()) {
            boolean cloudDecision = "deepseek".equalsIgnoreCase(response.getSource());
            tvCommand.setText(getString(
                    R.string.recognition_command_source,
                    response.getCommand(),
                    getString(cloudDecision
                            ? R.string.recognition_source_cloud
                            : R.string.recognition_source_local)));
            tvCommand.setVisibility(View.VISIBLE);
        } else {
            tvCommand.setVisibility(View.GONE);
        }
        if ("red".equalsIgnoreCase(light)) {
            resultPanel.setBackgroundResource(R.drawable.bg_status_danger);
        } else if ("green".equalsIgnoreCase(light)) {
            resultPanel.setBackgroundResource(R.drawable.bg_status_success);
        } else {
            resultPanel.setBackgroundResource(R.drawable.bg_status_warning);
        }
        appSettings.saveLastRecognition(light, response.getCommand(), message);
        String spoken = AccessibilityTextFormatter.recognition(
                message, response.getCommand());
        resultPanel.setContentDescription(spoken);
        speakImportant(resultPanel, spoken);
    }

    private String formatLightResult(String light) {
        if ("red".equalsIgnoreCase(light)) {
            return getString(R.string.recognition_red);
        }
        if ("green".equalsIgnoreCase(light)) {
            return getString(R.string.recognition_green);
        }
        return getString(R.string.recognition_unknown);
    }

    private void showRecognitionError() {
        tvResult.setText(R.string.recognition_failed);
        tvCommand.setText(getString(R.string.network_error_with_address,
                appSettings.getServerUrl()));
        tvCommand.setVisibility(View.VISIBLE);
        resultPanel.setBackgroundResource(R.drawable.bg_status_warning);
        String spoken = getString(R.string.recognition_failed) + "。"
                + getString(R.string.recognition_retry_instruction);
        resultPanel.setContentDescription(spoken);
        speakImportant(resultPanel, spoken);
    }

    private void setProcessing(boolean isProcessing) {
        processing = isProcessing;
        fabCapture.setEnabled(!isProcessing);
        fabCapture.setAlpha(isProcessing ? 0.5f : 1f);
        progressRecognition.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                speakImportant(fabCapture, getString(R.string.camera_permission_required));
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread == null) {
            return;
        }
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
        } catch (InterruptedException ignored) {
        } finally {
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
