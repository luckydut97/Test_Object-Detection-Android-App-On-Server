package com.luckydut.ondeviceaitest;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TimePicker;

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
import java.util.Calendar;
import java.util.Map;
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
    private AppCompatButton actionButton; // start/stop 버튼
    private ImageView settingsButton; // 설정 버튼
    private boolean isRunning = false; // start/stop 확인 버튼
    private TableLayout tableLayout;

    // 특정 시간대 설정 (초기값)
    private int startHour = 0;
    private int startMinute = 0;
    private int endHour = 0;
    private int endMinute = 0;

    private Handler timeHandler = new Handler(Looper.getMainLooper());
    private Runnable timeCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndToggleAction();
            // 2초마다 실행
            timeHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: App started");
        previewView = findViewById(R.id.camera_preview);
        overlay = findViewById(R.id.overlay);
        actionButton = findViewById(R.id.action_btn);
        settingsButton = findViewById(R.id.settings_icon);
        tableLayout = findViewById(R.id.table_layout);

        actionButton.setOnClickListener(v -> {
            if (startHour == 0 && startMinute == 0 && endHour == 0 && endMinute == 0) {
                Toast.makeText(this, "설정 버튼에서 자동 동작 시간을 설정해주세요.", Toast.LENGTH_LONG).show();
            } else {
                toggleActionButton();
            }
        });

        settingsButton.setOnClickListener(v -> showTimePickerDialog());

        if (overlay == null) {
            Log.e(TAG, "onCreate: Overlay view is null");
        } else {
            Log.d(TAG, "onCreate: Overlay view initialized");
        }

        // 설정 시간 불러오기
        loadTimePreferences();
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

        // 시간 체크 Runnable 시작
        timeCheckRunnable.run();

        /*// 비정상 종료 테스트 Test crash button
        findViewById(R.id.test_crash_button).setOnClickListener(v -> {
            throw new RuntimeException("Test crash"); // Force a crash
        });*/
    }

    private void toggleActionButton() {
        isRunning = !isRunning;
        Log.d("isRunning", "isRunning: " + isRunning);
        updateButtonState();
    }

    private void updateButtonState() {
        String timeRangeText = "설정시간: " + String.format("%02d:%02d~%02d:%02d", startHour, startMinute, endHour, endMinute);

        if (isRunning) {
            actionButton.setText("실행 중 (" + timeRangeText + ")");
            actionButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            startCamera();
        } else {
            if (startHour == 0 && startMinute == 0 && endHour == 0 && endMinute == 0) {
                actionButton.setText("시간 설정 대기 중");
            } else {
                actionButton.setText("실행 대기 (" + timeRangeText + ")");
            }
            actionButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
            stopCamera();
        }
    }

    private void checkAndToggleAction() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        boolean shouldRun = (hour > startHour || (hour == startHour && minute >= startMinute)) &&
                (hour < endHour || (hour == endHour && minute < endMinute));

        if (shouldRun && !isRunning) {
            isRunning = true;
            updateButtonState();
        } else if (!shouldRun && isRunning) {
            isRunning = false;
            updateButtonState();
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

    private void stopCamera() {
        Log.d(TAG, "stopCamera: Stopping camera");
        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error stopping camera: " + e.getMessage());
        }
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
                            ImageData imageData = new ImageData(base64Image, 2); // ID 변경(휴대폰 마다 id 다르게 해야함.)
                            Log.d(TAG, "Starting API call to upload image");
                            apiService.uploadImage(imageData).enqueue(new Callback<DetectionResult>() {
                                @Override
                                public void onResponse(Call<DetectionResult> call, Response<DetectionResult> response) {
                                    Log.d(TAG, "Response received");
                                    if (response.isSuccessful() && response.body() != null) {
                                        DetectionResult detectionResult = response.body();
                                        if (detectionResult.getDetection() == 1) {
                                            Log.d(TAG, "Detection result: " + detectionResult.getBoxes().toString());
                                            mainHandler.post(() -> {
                                                overlay.update(detectionResult); // UI 업데이트를 메인 스레드에서 실행
                                                updateTable(detectionResult.getClassCnt()); // 테이블 업데이트
                                            });
                                        } else {
                                            Log.d(TAG, "No detection");
                                            mainHandler.post(() -> {
                                                overlay.update(null); // UI 업데이트를 메인 스레드에서 실행
                                                updateTable(null); // 빈 테이블 표시
                                            });
                                        }
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

    private void updateTable(Map<String, Integer> classCnt) {
        TableLayout tableLayout = findViewById(R.id.table_layout);

        // 헤더와 첫 번째 구분선을 제외한 모든 행 제거
        int childCount = tableLayout.getChildCount();
        if (childCount > 2) {
            tableLayout.removeViews(2, childCount - 2);
        }

        if (classCnt == null) {
            return;
        }

        // 데이터 행 추가
        for (Map.Entry<String, Integer> entry : classCnt.entrySet()) {
            String productName = entry.getKey();
            int quantity = entry.getValue();

            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            row.setGravity(Gravity.CENTER_VERTICAL); // TableRow의 세로 가운데 정렬

            // 제품명 TextView
            TextView tvProductName = new TextView(this);
            tvProductName.setText(productName);
            tvProductName.setGravity(Gravity.CENTER);
            tvProductName.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            tvProductName.setTextSize(16); // 글자 크기 설정
            row.addView(tvProductName);

            // 색상 View
            LinearLayout colorLayout = new LinearLayout(this);
            colorLayout.setGravity(Gravity.CENTER);
            TableRow.LayoutParams colorLayoutParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
            colorLayout.setLayoutParams(colorLayoutParams);

            View colorView = new View(this);
            LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(30, 30); // 정사각형 설정
            colorParams.gravity = Gravity.CENTER;
            colorView.setLayoutParams(colorParams);

            // 리소스 파일에서 색상 가져오기
            if (productName.startsWith("Y")) {
                colorView.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow));
            } else if (productName.startsWith("V")) {
                colorView.setBackgroundColor(ContextCompat.getColor(this, R.color.violet));
            } else if (productName.startsWith("G")) {
                colorView.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
            }

            colorLayout.addView(colorView);
            row.addView(colorLayout);

            // 수량 TextView
            TextView tvQuantity = new TextView(this);
            tvQuantity.setText(String.valueOf(quantity));
            tvQuantity.setGravity(Gravity.CENTER);
            tvQuantity.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            tvQuantity.setTextSize(16); // 글자 크기 설정
            row.addView(tvQuantity);

            tableLayout.addView(row);

            // 구분선 추가
            View divider = new View(this);
            TableRow.LayoutParams dividerParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1);
            dividerParams.setMargins(0, 4, 0, 3);
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(Color.parseColor("#CCCCCC"));
            tableLayout.addView(divider);
        }
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
        timeHandler.removeCallbacks(timeCheckRunnable); // 타임 체크 핸들러 콜백 제거
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

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // 시작 시간 설정
        View startTitleView = getLayoutInflater().inflate(R.layout.dialog_title, null);
        TextView startTitleTextView = startTitleView.findViewById(R.id.dialogTitle);
        startTitleTextView.setText("시작 시간 설정");

        TimePickerDialog startTimePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            startHour = hourOfDay;
            startMinute = minute;

            // 종료 시간 설정
            View endTitleView = getLayoutInflater().inflate(R.layout.dialog_title, null);
            TextView endTitleTextView = endTitleView.findViewById(R.id.dialogTitle);
            endTitleTextView.setText("종료 시간 설정");

            TimePickerDialog endTimePicker = new TimePickerDialog(this, (view1, hourOfDay1, minute1) -> {
                if (hourOfDay1 < startHour || (hourOfDay1 == startHour && minute1 <= startMinute)) {
                    Toast.makeText(MainActivity.this, "종료 시간을 시작 시간보다 이전으로 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                } else {
                    endHour = hourOfDay1;
                    endMinute = minute1;
                    Toast.makeText(MainActivity.this, "시간이 설정되었습니다: " + startHour + ":" + startMinute + " - " + endHour + ":" + endMinute, Toast.LENGTH_SHORT).show();
                    saveTimePreferences(); // 시간을 저장
                    updateButtonState();
                }
            }, currentHour, currentMinute, true);

            // 종료 시간 설정 다이얼로그 타이틀 설정
            endTimePicker.setCustomTitle(endTitleView);
            endTimePicker.show();
        }, currentHour, currentMinute, true);

        // 시작 시간 설정 다이얼로그 타이틀 설정
        startTimePicker.setCustomTitle(startTitleView);
        startTimePicker.show();
    }

    private void saveTimePreferences() {
        SharedPreferences preferences = getSharedPreferences("time_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("start_hour", startHour);
        editor.putInt("start_minute", startMinute);
        editor.putInt("end_hour", endHour);
        editor.putInt("end_minute", endMinute);
        editor.apply();
    }

    private void loadTimePreferences() {
        SharedPreferences preferences = getSharedPreferences("time_prefs", MODE_PRIVATE);
        startHour = preferences.getInt("start_hour", 0);
        startMinute = preferences.getInt("start_minute", 0);
        endHour = preferences.getInt("end_hour", 0);
        endMinute = preferences.getInt("end_minute", 0);
    }
}