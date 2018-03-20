package com.example.amap3d.MyView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.example.amap3d.R;

/**
 * Created by BieTong on 2018/3/19.
 */

public class MyButton extends android.support.v7.widget.AppCompatButton {
    private int degree=0;
    private Paint paint=new Paint();
    private boolean isRefreshing=false;
    private Bitmap bitmap;
    private int width;
    private int height;
    Matrix matrix = new Matrix();

    public MyButton(Context context) {
        super(context);
//        init();
    }

    public MyButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
//        init();
    }

    public MyButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
//        init();
    }

    private void init(){
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
//        this.setBackgroundColor(Color.parseColor("#20ffffff"));
        bitmap= BitmapFactory.decodeResource(getResources(), R.drawable.refresh);
        float scaleWidth = ((float) width) / bitmap.getWidth();
        float scaleHeight = ((float) height) / bitmap.getHeight();
        matrix.postScale(scaleWidth, scaleHeight);
    }

    public void setRefreshing(boolean isRefreshing){
        this.isRefreshing=isRefreshing;
        if(isRefreshing){
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawColor(Color.parseColor("#ffffff"));
//        canvas.drawBitmap(bitmap,matrix,paint);
//        canvas.rotate(degree);
//        degree+=2;
//        if(degree>360){
//            degree=0;
//        }
//        if(isRefreshing){
//            invalidate();
//        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width=getWidth();
        height=getHeight();
    }
}
