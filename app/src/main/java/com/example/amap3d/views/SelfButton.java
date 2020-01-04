package com.example.amap3d.views;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.utils.Utils;


/**
 * Created by BieTong on 2018/3/19.
 */

public class SelfButton extends android.support.v7.widget.AppCompatButton {
    protected int buttonWidth;
    protected Paint paint;

    public SelfButton(Context context) {
        super(context);
        init();
    }

    public SelfButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SelfButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#808080"));
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void init() {
//        buttonWidth = Utils.getScreenWidth(MainActivity.getInstance()) / 10;
        buttonWidth = Utils.px(MainActivity.getInstance(), 42);
        this.setBackground(getResources().getDrawable(R.drawable.bg_shadow));
        this.setElevation(Utils.px(MainActivity.getInstance(),4));
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, left + buttonWidth, top + buttonWidth);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int myWidthMeasureSpec = MeasureSpec.makeMeasureSpec(buttonWidth, MeasureSpec.EXACTLY);
        super.onMeasure(myWidthMeasureSpec, myWidthMeasureSpec);
    }

    private int dy = 0;

//    public void moveByY(int dy) {
//        int x = (int) getX();
//        int y = (int) getY();
//        this.dy = dy;
//        moveHandler.sendEmptyMessage(0);
//        layout(x, y + dy, x + buttonWidth, y + buttonWidth + dy);
//    }

//    private Handler moveHandler = new Handler(new Handler.Callback() {
//        @Override
//        public boolean handleMessage(Message msg) {
//            switch (msg.what) {
//                case 0:
//                    int x = (int) getX();
//                    int y = (int) getY();
//                    if (dy > 0) {
//                        layout(x, y + 1, x + buttonWidth, y + buttonWidth + 1);
//                        dy--;
//                        moveHandler.sendEmptyMessageDelayed(0, 16);
//                    } else if (dy < 0) {
//                        layout(x, y - 1, x + buttonWidth, y + buttonWidth - 1);
//                        dy++;
//                        moveHandler.sendEmptyMessageDelayed(0, 16);
//                    }
//            }
//            return false;
//        }
//    });

    public void moveByY(int dy) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                this,
                "translationY",
                dy);
        animator.setDuration(200);
        animator.start();
    }
}
