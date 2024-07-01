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
    private DetectionResult result;
    private final Paint paint;

    public Overlay(Context context) {
        super(context);
        paint = new Paint();
        initPaint();
    }

    public Overlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        initPaint();
    }

    public Overlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        initPaint();
    }

    private void initPaint() {
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setTextSize(44);
    }

    public void update(DetectionResult result) {
        this.result = result;
        invalidate();  // View를 다시 그리도록 요청
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (result != null && result.getDetection() == 1) {
            Log.d(TAG, "onDraw: Drawing detection result");

            for (DetectionResult.Box box : result.getBoxes()) {
                Log.d(TAG, "onDraw: Drawing box: " + box.toString());

                float startX = box.getX1();
                float startY = box.getY1();
                float endX = box.getX2();
                float endY = box.getY1();  // y1 위치에서 가로줄만 그림

                // 상단 가로줄만 그림
                canvas.drawLine(startX, startY, endX, endY, paint);
                canvas.drawText(box.getLabel(), startX, startY - 10, paint);
            }
        } else {
            Log.d(TAG, "onDraw: No detection");
        }
    }
}