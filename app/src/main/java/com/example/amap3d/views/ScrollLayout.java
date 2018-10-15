package com.example.amap3d.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.example.amap3d.R;

public class ScrollLayout extends LinearLayout {
    private int oldY;
    private int distanceY;
    private View childView;
    private Scroller scroller;
    private int scrollMaxHeight;
    private boolean isChildViewAtTop = false;
    private float minHeight;

    public ScrollLayout(Context context) {
        this(context, null);
    }

    public ScrollLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScrollLayout);
        minHeight = typedArray.getDimension(R.styleable.ScrollLayout_minHeight, 200);
        typedArray.recycle();
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        scrollMaxHeight = (int) (childView.getMeasuredHeight() - minHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        childView.layout(0, scrollMaxHeight, childView.getMeasuredWidth(), childView.getMeasuredHeight() + scrollMaxHeight);
    }

    private void init(Context context) {
        scroller = new Scroller(context);
        this.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() == 0 || getChildAt(0) == null) {
            throw new RuntimeException("没有子控件");
        }
        if (getChildCount() > 1) {
            throw new RuntimeException("只能有一个子控件");
        }
        childView = getChildAt(0);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldY = (int) event.getY();
                //如果子View不在顶部 && 按下的位置在子View没有显示的位置，则不消费此次滑动事件，否则消费
                return isChildViewAtTop || oldY >= scrollMaxHeight || super.onTouchEvent(event);
            case MotionEvent.ACTION_MOVE:
                int currentY = (int) event.getY();
                int dy = oldY - currentY;
                if (dy > 0) {
                    distanceY += dy;
                    if (distanceY > scrollMaxHeight) {
                        distanceY = scrollMaxHeight;
                    } else if (distanceY < scrollMaxHeight) {
                        scrollBy(0, dy);
                        setScrollListenerUp(false);
                        oldY = currentY;
                        return true;
                    }
                }
                //向下滑动时的处理，向下滑动时需要判断子View是否在顶部，如果不在顶部则不消费此次事件
                if (dy < 0 && isChildViewAtTop) {
                    distanceY += dy;
                    if (distanceY < 0) {
                        distanceY = 0;
                    } else if (distanceY > 0) {
                        scrollBy(0, dy);
                        setScrollListenerUp(false);
                    }
                    oldY = currentY;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                //手指抬起时的处理，如果向上滑动的距离超过了最大可滑动距离的1/4，并且子View不在顶部，就表示想把它拉上去
                if (distanceY > scrollMaxHeight / 4 && !isChildViewAtTop) {
                    scroller.startScroll(0, getScrollY(), 0, (scrollMaxHeight - getScrollY()));
                    setScrollListenerUp(true);
                    invalidate();
                    distanceY = scrollMaxHeight;
                    isChildViewAtTop = true;
                } else {
                    //否则就表示放弃本次滑动，让它滑到最初的位置
                    scroller.startScroll(0, getScrollY(), 0, -getScrollY());
                    setScrollListenerUp(false);
                    postInvalidate();
                    distanceY = 0;
                    isChildViewAtTop = false;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void open() {
        scroller.startScroll(0, getScrollY(), 0, (scrollMaxHeight - getScrollY()));
        setScrollListenerUp(true);
        invalidate();
        distanceY = scrollMaxHeight;
        isChildViewAtTop = true;
    }

    public void close() {
        scroller.startScroll(0, getScrollY(), 0, -getScrollY());
        setScrollListenerUp(false);
        postInvalidate();
        distanceY = 0;
        isChildViewAtTop = false;
    }

    public boolean isOpen() {
        return distanceY > 0;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(0, scroller.getCurrY());
            postInvalidate();
        }
    }

    private void setScrollListenerUp(boolean isUp) {
        if (onScrollLayoutStateChangeListener == null) {
            return;
        }
        if (isUp) {
            onScrollLayoutStateChangeListener.startingScrollUp(distanceY);
        } else {
            onScrollLayoutStateChangeListener.scrollDownEnd(distanceY);
        }
    }


    private OnScrollLayoutStateChangeListener onScrollLayoutStateChangeListener;

    public void setOnScrollLayoutStateChangeListener(OnScrollLayoutStateChangeListener onScrollLayoutStateChangeListener) {
        this.onScrollLayoutStateChangeListener = onScrollLayoutStateChangeListener;
    }

    public interface OnScrollLayoutStateChangeListener {

        void startingScrollUp(int currentHeight);

        void scrollDownEnd(int currentHeight);
    }
}

