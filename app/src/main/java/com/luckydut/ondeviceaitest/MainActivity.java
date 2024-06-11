package com.luckydut.ondeviceaitest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private ApiService apiService;
    private Handler mainHandler;
    private long lastUploadedTime = 0;

    private Overlay overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: Initializing views");
        previewView = findViewById(R.id.camera_preview);
        overlay = findViewById(R.id.overlay);

        if (overlay == null) {
            Log.e(TAG, "onCreate: Overlay view is null");
        } else {
            Log.d(TAG, "onCreate: Overlay view initialized");
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        apiService = ApiClient.getClient().create(ApiService.class);
        mainHandler = new Handler(Looper.getMainLooper());
        requestCameraPermission();
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is necessary", Toast.LENGTH_LONG).show();
        }
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
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
                .setTargetResolution(new Size(3840, 2160))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(10)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime - lastUploadedTime >= 1000) { // 1초 간격 확인
                lastUploadedTime = currentTime; // 마지막 업로드 시간 업데이트

                mainHandler.post(() -> {
                    Bitmap bitmap = previewView.getBitmap();
                    if (bitmap != null) {
                        String base64Image = ImageUtils.bitmapToBase64(bitmap);
                        ImageData imageData = new ImageData(base64Image);
                        Log.d(TAG, "Starting API call to upload image");
                        apiService.uploadImage(imageData).enqueue(new Callback<DetectionResult>() {
                            @Override
                            public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
                                Log.d(TAG, "Response received");
                                if (response.isSuccessful() && response.body() != null) {
                                    DetectionResult detectionResult = response.body();
                                    Log.d(TAG, "Detection result: " + detectionResult.getObjects().toString());
                                    overlay.update(bitmap, detectionResult); // Overlay 업데이트
                                } else {
                                    try {
                                        String errorBody = response.errorBody().string();
                                        Log.e(TAG, "Response error: " + errorBody);
                                    } catch (IOException e) {
                                        Log.e(TAG, "Error reading response error body", e);
                                    }
                                    Toast.makeText(MainActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<DetectionResult> call, Throwable t) {
                                Log.e(TAG, "Request failed: " + t.getMessage());
                                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    image.close();
                });
            } else {
                image.close();
            }
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}