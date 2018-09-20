package com.example.amap3d.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.example.amap3d.R;

/**
 * Created by BieTong on 2018/3/19.
 */

public class RefreshButton extends android.support.v7.widget.AppCompatButton {
    private boolean isRefreshing = false;

    private int width;
    private int height;

    private Bitmap bitmap;
    private Paint paint = new Paint();
    Matrix matrix = new Matrix();

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
        this.setBackground(getResources().getDrawable(R.drawable.button_ground));
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#808080"));

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
            matrix.postRotate(3, width / 2, height / 2);
            invalidate();
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = getWidth();
        height = getHeight();
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.refresh);
        float scaleWidth = ((float) width) / bitmap.getWidth();
        float scaleHeight = ((float) height) / bitmap.getHeight();
        matrix.setTranslate(0, 0);
        matrix.postScale(scaleWidth, scaleHeight);
    }
}
