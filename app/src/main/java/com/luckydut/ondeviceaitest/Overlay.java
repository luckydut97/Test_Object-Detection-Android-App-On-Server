package com.luckydut.ondeviceaitest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class Overlay extends View {
    private static final String TAG = "Overlay";
<<<<<<< HEAD
    private Bitmap bitmap;
=======
>>>>>>> 04dd40e (On/Off 기능 추가 및 인식 결과값 표시 UI 설계 시작)
    private DetectionResult result;
    private final Paint paint;

    public Overlay(Context context) {
        super(context);
        paint = new Paint();
        initPaint();
    }

    public Overlay(Context context, AttributeSet attrs) {
        super(context, attrs);
<<<<<<< HEAD
=======
        paint = new Paint();
        initPaint();
>>>>>>> 04dd40e (On/Off 기능 추가 및 인식 결과값 표시 UI 설계 시작)
    }

    public Overlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
<<<<<<< HEAD
    }

    public void update(Bitmap bitmap, DetectionResult result) {
        this.bitmap = bitmap;
=======
        paint = new Paint();
        initPaint();
    }

    private void initPaint() {
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setTextSize(40);
    }

    public void update(DetectionResult result) {
>>>>>>> 04dd40e (On/Off 기능 추가 및 인식 결과값 표시 UI 설계 시작)
        this.result = result;
        invalidate();  // View를 다시 그리도록 요청
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
<<<<<<< HEAD
        if (bitmap != null && result != null) {
            Log.d(TAG, "onDraw: Drawing bitmap and detection result");
            canvas.drawBitmap(bitmap, 0, 0, null);

            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setTextSize(50);
=======
        if (result != null) {
            Log.d(TAG, "onDraw: Drawing detection result");
>>>>>>> 04dd40e (On/Off 기능 추가 및 인식 결과값 표시 UI 설계 시작)

            for (DetectionResult.Object object : result.getObjects()) {
                Log.d(TAG, "onDraw: Drawing object: " + object.toString());
                canvas.drawRect(object.getX(), object.getY(), object.getX() + object.getWidth(), object.getY() + object.getHeight(), paint);
                canvas.drawText(object.getLabel(), object.getX(), object.getY() - 10, paint);
            }
        } else {
<<<<<<< HEAD
            Log.d(TAG, "onDraw: Bitmap or detection result is null");
=======
            Log.d(TAG, "onDraw: Detection result is null");
>>>>>>> 04dd40e (On/Off 기능 추가 및 인식 결과값 표시 UI 설계 시작)
        }
    }
}