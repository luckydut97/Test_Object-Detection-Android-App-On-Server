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
        if (result != null) {
            Log.d(TAG, "onDraw: Drawing detection result");

            for (DetectionResult.Object object : result.getObjects()) {
                Log.d(TAG, "onDraw: Drawing object: " + object.toString());
                float left = object.getCenterX() - object.getWidth() / 2;
                float top = object.getCenterY() - object.getHeight() / 2;
                float right = object.getCenterX() + object.getWidth() / 2;
                float bottom = object.getCenterY() + object.getHeight() / 2;

                canvas.drawRect(left, top, right, bottom, paint);
                canvas.drawText(object.getLabel(), left, top - 10, paint);
            }
        } else {
            Log.d(TAG, "onDraw: Detection result is null");
        }
    }
}