package com.example.amap3d.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class SwitchView extends View {

    public SwitchView(Context context) {
        super(context);
        init();
    }

    public SwitchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private static final int OPEN = 1;
    private static final int CLOSE = 0;

    private Paint paint;
    private Path backgroundPath, foregroundPath;
    private float viewWidth, viewHeight;
    private float halfViewWidth, halfViewHeight;

    private int shrink;
    private float x, scale;
    private int speed = 6;
    private boolean isOpen = false;
    private boolean isMoving = false;
    private int color, currentColor;

    private void init() {
        paint = new Paint();
        backgroundPath = new Path();
        foregroundPath = new Path();
        scale = 1;
        shrink = 3;
        color = Color.parseColor("#395dd7");
        currentColor = Color.parseColor("#e0e0e0");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas, currentColor);
        drawForeground(canvas);
        drawCircle(canvas, x, viewHeight / 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//        int heightSize = (int) (widthSize * 0.62f);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = (int) (heightSize * 1.61);
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        halfViewWidth = w / 2;
        halfViewHeight = h / 2;
        x = h / 2;
        setBackgroundPath(w, h);
        setForegroundPath(w, h);
    }

    private Handler invalidateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case OPEN:
                    if (x < viewWidth - halfViewHeight) {
                        currentColor = color;
                        isMoving = true;
                        x += speed;
                        scale = 1 - (x - halfViewHeight) / (viewWidth - viewHeight);
                        scale = scale < 0 ? 0 : scale;
                        invalidate();
                        invalidateHandler.sendEmptyMessageDelayed(OPEN, 16);
                    } else {
                        x = viewWidth - halfViewHeight;
                        scale = 0;
                        invalidate();
                        isMoving = false;
                        isOpen = true;
                    }
                    break;
                case CLOSE:
                    if (x > viewHeight / 2) {
                        currentColor = Color.parseColor("#e0e0e0");
                        isMoving = true;
                        x -= speed;
                        scale = 1 - (x - halfViewHeight) / (viewWidth - viewHeight);
                        invalidate();
                        invalidateHandler.sendEmptyMessageDelayed(CLOSE, 16);
                    } else {
                        x = halfViewHeight;
                        scale = 1;
                        invalidate();
                        isMoving = false;
                        isOpen = false;
                    }
                    break;
                default:
            }
            return false;
        }
    });

    private void drawBackground(Canvas canvas, int color) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawPath(backgroundPath, paint);
    }

    private void drawForeground(Canvas canvas) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        canvas.save();
        canvas.scale(scale, scale, halfViewWidth, halfViewHeight);
        canvas.drawPath(foregroundPath, paint);
        canvas.restore();
    }

    private void drawCircle(Canvas canvas, float x, float y) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#d7d7d7"));
        canvas.drawCircle(x, y, halfViewHeight - shrink, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, halfViewHeight - shrink * 1.4f, paint);
    }

    private void setBackgroundPath(int w, int h) {
        RectF rectF = new RectF(0, 0, h, h);
        backgroundPath.arcTo(rectF, 90, 180);
        backgroundPath.lineTo(w - h / 2, 0);
        rectF.left = w - h;
        rectF.right = w;
        backgroundPath.arcTo(rectF, 270, 180);
        backgroundPath.close();
    }

    private void setForegroundPath(int w, int h) {
        RectF rectF = new RectF(shrink, shrink, h - shrink, h - shrink);
        foregroundPath.arcTo(rectF, 90, 180);
        rectF.left = w - h + shrink;
        rectF.right = w - shrink;
        foregroundPath.arcTo(rectF, 270, 180);
        foregroundPath.close();
    }

    public void open() {
        if (!isMoving) {
            invalidateHandler.sendEmptyMessage(OPEN);
        }
    }

    public void close() {
        if (!isMoving) {
            invalidateHandler.sendEmptyMessage(CLOSE);
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setShrink(int shrink) {
        shrink = shrink < 0 ? 0 : shrink;
        shrink = shrink > halfViewHeight ? (int) halfViewHeight : shrink;
        this.shrink = shrink;
    }

    public void setSpeed(int speed) {
        speed = speed < 1 ? 1 : speed;
        speed = speed > halfViewHeight ? (int) halfViewHeight : speed;
        this.speed = speed;
    }

    public void setOpen() {
        this.post(new Runnable() {
            @Override
            public void run() {
                x = viewWidth - halfViewHeight;
                isOpen = true;
                currentColor = color;
                scale = 0;
                invalidate();
            }
        });
    }
}