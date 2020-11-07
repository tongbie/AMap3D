package com.example.amap3d.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.example.amap3d.R;

/**
 * Created by BieTong on 2018/3/19.
 */

public class RefreshButton extends SelfButton {
    private boolean isRefreshing = false;

    private Bitmap bitmap;
    private Matrix matrix = new Matrix();

    public RefreshButton(Context context) {
        super(context);
        init();
    }

    public RefreshButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

    }

    public synchronized void setRefreshing(boolean isRefreshing) {
        this.isRefreshing = isRefreshing;
        if (isRefreshing) {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, matrix, paint);
        if (isRefreshing) {
            matrix.postRotate(3, buttonWidth / 2, buttonWidth / 2);
            invalidate();
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_refresh);
        float scale = ((float) buttonWidth) / bitmap.getWidth();
        matrix.setTranslate(0, 0);
        matrix.postScale(scale, scale);
    }
}
