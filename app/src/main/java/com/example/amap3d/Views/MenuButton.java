package com.example.amap3d.Views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.Button;

import com.example.amap3d.R;

/**
 * Created by BieTong on 2018/4/1.
 */

public class MenuButton extends android.support.v7.widget.AppCompatButton {
    private Paint paint=new Paint();

    private int width;
    private int height;

    public MenuButton(Context context) {
        super(context);
        init();
    }

    public MenuButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MenuButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        this.setBackground(getResources().getDrawable(R.drawable.button_ground));
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#808080"));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width=getWidth();
        height=getHeight();
    }
}
