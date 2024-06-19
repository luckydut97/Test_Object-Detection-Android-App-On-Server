package com.luckydut.ondeviceaitest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private ExecutorService imageProcessingExecutor;
    private ApiService apiService;
    private Handler mainHandler;
    private long lastUploadedTime = 0;

    private Overlay overlay;
    private AppCompatButton actionButton; //start/stop 버튼
    private boolean isRunning = false; // start/stop 확인 버튼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: App started");
        previewView = findViewById(R.id.camera_preview);
        overlay = findViewById(R.id.overlay);
        actionButton = findViewById(R.id.action_btn);

        actionButton.setOnClickListener(v -> toggleActionButton());

        if (overlay == null) {
            Log.e(TAG, "onCreate: Overlay view is null");
        } else {
            Log.d(TAG, "onCreate: Overlay view initialized");
        }

        // 초기 상태 설정
        updateButtonState();

        cameraExecutor = Executors.newSingleThreadExecutor();
        imageProcessingExecutor = Executors.newSingleThreadExecutor();
        apiService = ApiClient.getClient().create(ApiService.class);
        mainHandler = new Handler(Looper.getMainLooper());
        requestCameraPermission();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, "onCreate: Handling display cutout");
            handleDisplayCutout();
        }
    }

    private void toggleActionButton() {
        isRunning = !isRunning;
        Log.d("isRunning", "isRunning: " + isRunning);
        updateButtonState();
    }

    private void updateButtonState() {
        if (isRunning) {
            actionButton.setText("STOP");
            actionButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            startCamera();
        } else {
            actionButton.setText("START");
            actionButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
        }
    }

    private void requestCameraPermission() {
        Log.d(TAG, "requestCameraPermission: Checking camera permission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            Log.d(TAG, "requestCameraPermission: Camera permission already granted");
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: Request code: " + requestCode);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult: Camera permission granted");
            startCamera();
        } else {
            Log.d(TAG, "onRequestPermissionsResult: Camera permission denied");
            Toast.makeText(this, "Camera permission is necessary", Toast.LENGTH_LONG).show();
        }
    }

    private void startCamera() {
        Log.d(TAG, "startCamera: Starting camera");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "bindCameraUseCases: Binding camera use cases");
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(100)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1080, 1520))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(10)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime - lastUploadedTime >= 1000 && isRunning) { // 1초 간격 확인 및 isRunning 상태 확인
                lastUploadedTime = currentTime; // 마지막 업로드 시간 업데이트

                mainHandler.post(() -> {
                    Bitmap bitmap = previewView.getBitmap();
                    if (bitmap != null) {
                        Bitmap finalBitmap = rotateBitmap(bitmap, 0); // 필요시 각도 조절
                        image.close(); // image를 close 처리합니다.

                        imageProcessingExecutor.execute(() -> {
                            String base64Image = ImageUtils.bitmapToBase64(finalBitmap);
                            ImageData imageData = new ImageData(base64Image);
                            Log.d(TAG, "Starting API call to upload image");
                            apiService.uploadImage(imageData).enqueue(new Callback<DetectionResult>() {
                                @Override
                                public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
                                    Log.d(TAG, "Response received");
                                    if (response.isSuccessful() && response.body() != null) {
                                        DetectionResult detectionResult = response.body();
                                        Log.d(TAG, "Detection result: " + detectionResult.getObjects().toString());

                                        mainHandler.post(() -> overlay.update(detectionResult)); // UI 업데이트를 메인 스레드에서 실행
                                    } else {
                                        try {
                                            String errorBody = response.errorBody().string();
                                            Log.e(TAG, "Response error: " + errorBody);
                                        } catch (IOException e) {
                                            Log.e(TAG, "Error reading response error body", e);
                                        }
                                        mainHandler.post(() -> Toast.makeText(MainActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
                                    }
                                }

                                @Override
                                public void onFailure(Call<DetectionResult> call, Throwable t) {
                                    Log.e(TAG, "Request failed: " + t.getMessage());
                                    mainHandler.post(() -> Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            });
                        });
                    } else {
                        image.close();
                    }
                });
            } else {
                image.close();
            }
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        imageProcessingExecutor.shutdown();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void handleDisplayCutout() {
        getWindow().getDecorView().setOnApplyWindowInsetsListener((v, insets) -> {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                Log.d(TAG, "Display Cutout: " + cutout.toString());
                View view = findViewById(R.id.camera_preview);
                if (view != null) {
                    int left = cutout.getSafeInsetLeft();
                    int top = cutout.getSafeInsetTop();
                    int right = cutout.getSafeInsetRight();
                    int bottom = cutout.getSafeInsetBottom();
                    Log.d(TAG, "Padding values - Left: " + left + ", Top: " + top + ", Right: " + right + ", Bottom: " + bottom);
                    view.setPadding(left, top, right, bottom);
                } else {
                    Log.e(TAG, "handleDisplayCutout: camera_preview view is null");
                }
            } else {
                Log.d(TAG, "handleDisplayCutout: No cutout found");
            }
            return v.onApplyWindowInsets(insets);
        });
    }
}