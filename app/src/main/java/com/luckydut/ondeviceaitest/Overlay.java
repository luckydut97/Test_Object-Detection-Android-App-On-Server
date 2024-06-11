package com.luckydut.ondeviceaitest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class Overlay extends View {
    private static final String TAG = "Overlay";
    private Bitmap bitmap;
    private DetectionResult result;

    public Overlay(Context context) {
        super(context);
    }

    public Overlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Overlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void update(Bitmap bitmap, DetectionResult result) {
        this.bitmap = bitmap;
        this.result = result;
        invalidate();  // View를 다시 그리도록 요청
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null && result != null) {
            Log.d(TAG, "onDraw: Drawing bitmap and detection result");
            canvas.drawBitmap(bitmap, 0, 0, null);

            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setTextSize(50);

            for (DetectionResult.Object object : result.getObjects()) {
                Log.d(TAG, "onDraw: Drawing object: " + object.toString());
                canvas.drawRect(object.getX(), object.getY(), object.getX() + object.getWidth(), object.getY() + object.getHeight(), paint);
                canvas.drawText(object.getLabel(), object.getX(), object.getY() - 10, paint);
            }
        } else {
            Log.d(TAG, "onDraw: Bitmap or detection result is null");
        }
    }
}