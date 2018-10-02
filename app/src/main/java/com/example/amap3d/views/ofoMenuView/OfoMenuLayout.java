package com.example.amap3d.views.ofoMenuView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.amap3d.R;

import java.util.List;

/**
 * Created by xiangcheng on 17/9/19.
 */
//实质上是一个相对布局
public class OfoMenuLayout extends RelativeLayout {

//    分为两部分：上面标题下面内容
    private View titleView;
    private View contentView;
    //动画对象（两个分别对应以上两个部分）
    private ObjectAnimator titleAnimator, contentAnimator;
    //title起始和终止坐标，主要为动画做准备
    private int titleStartY, titleEndY;
    //content起始和终止坐标，主要为动画做准备
    private int contentStartY, contentEndY;
    //title动画标志，为事件分发做准备
    private boolean titleAnimationing;
    //content动画标志，为事件分发做准备
    private boolean contentAnimationing;

    //content中列表内容布局，它里面也有自己的动画，跳转进入查看 todo
    private OfoContentLayout ofoContentLayout;

//    menu
    FrameLayout menu;

//    实质上是个drawable，跳转进入查看 todo
    MenuBrawable menuBrawable;

    public boolean isOpen() {
        return isOpen;
    }

    private boolean isOpen;

//    OfoMenu状态监听器接口实例
    private OfoMenuStatusListener ofoMenuStatusListener;

    public void setUserIcon(@DrawableRes int userIcon) {
        if (menuBrawable != null) {
            menuBrawable.setBitmap(BitmapFactory.decodeResource(getResources(), userIcon));
        }
    }

//    OfoMenu状态监听器接口，两个监听方法监听打开时，关闭时
    public interface OfoMenuStatusListener {
        void onOpen();

        void onClose();
    }

    public interface OfoUserIconListener {
        void onClick();
    }

    @SuppressLint("CutPasteId")
    public OfoMenuLayout(Context context, int radian, @ColorRes int ofoBackColor, @DimenRes int dimens,
                         @DrawableRes int closeIcon, @DrawableRes int userIcon, List<View> itemContentViews) {
        super(context);
        View.inflate(context, R.layout.ofo_menu_view_layout, this);
        ofoContentLayout = findViewById(R.id.ofo_content);
        menu = findViewById(R.id.menu_content);
        if (ofoBackColor != -1) {
            findViewById(R.id.top_back).setBackgroundColor(context.getResources().getColor(ofoBackColor));
        }
        if (userIcon == -1) {
            userIcon = R.mipmap.default_avatar_img;
        }
        menuBrawable = new MenuBrawable(BitmapFactory.decodeResource(getResources(), userIcon), context, menu, radian);
        menu.setBackground(menuBrawable);
        if (dimens != -1) {
            float dimension = context.getResources().getDimension(dimens);
            ViewGroup.LayoutParams layoutParams = findViewById(R.id.top_back).getLayoutParams();
            layoutParams.height = (int) dimension;
            findViewById(R.id.top_back).setLayoutParams(layoutParams);
            ViewGroup menuContent = findViewById(R.id.menu_content);
            RelativeLayout.LayoutParams contentLp = (LayoutParams) menuContent.getLayoutParams();
            contentLp.setMargins(0, (int) (dimension - menuBrawable.getArcY()), 0, 0);
            menuContent.setLayoutParams(contentLp);
        }
        if (closeIcon != -1) {
            ((ImageView) findViewById(R.id.close)).setImageResource(closeIcon);
        }

        //给content部分设置item
        ofoContentLayout.setItemContentViews(itemContentViews);

        //关闭menu
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        setVisibility(INVISIBLE);
    }

    public void setOnItemClickListener(OfoContentLayout.OnItemClickListener onItemClickListener) {
        if (ofoContentLayout != null) {
            ofoContentLayout.setOnItemClickListener(onItemClickListener);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        titleView = getChildAt(0);
        contentView = getChildAt(1);
    }

    //定义动画部分
    private void definitAnimation() {
        PropertyValuesHolder titlePropertyValuesHolder = PropertyValuesHolder.ofFloat("translationY", titleStartY, titleEndY);
        titleAnimator = ObjectAnimator.ofPropertyValuesHolder(titleView, titlePropertyValuesHolder);
        titleAnimator.setDuration(300);

        contentAnimator = ObjectAnimator.ofFloat(contentView, "translationY", contentStartY, contentEndY);
        contentAnimator.setDuration(500);

        titleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                titleAnimationing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                titleAnimationing = false;
            }
        });

        contentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                contentAnimationing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                contentAnimationing = false;
                isOpen = !isOpen;
                setVisibility(isOpen ? VISIBLE : INVISIBLE);
                if (isOpen) {
                    if (ofoMenuStatusListener != null) {
                        ofoMenuStatusListener.onOpen();
                    }
                } else {
                    if (ofoMenuStatusListener != null) {
                        ofoMenuStatusListener.onClose();
                    }
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return titleAnimationing || contentAnimationing || ofoContentLayout.isAnimationing();
    }

    //菜单打开的动画
    public void open() {
        setVisibility(View.VISIBLE);

        int titleHeight = titleView.getLayoutParams().height;
        titleStartY = -titleHeight;
        titleEndY = 0;

        contentStartY = getHeight() + contentView.getHeight();
        contentEndY = 0;
        definitAnimation();
        titleAnimator.start();
        contentAnimator.start();
        ofoContentLayout.open();
    }

    //菜单关闭的动画
    public void close() {
        int titleHeight = titleView.getLayoutParams().height;
        titleStartY = 0;
        titleEndY = -titleHeight;

        contentStartY = 0;
        contentEndY = getHeight() + contentView.getHeight();

        definitAnimation();

        titleAnimator.start();
        contentAnimator.start();
    }

    public void setOfoMenuStatusListener(OfoMenuStatusListener ofoMenuStatusListener) {
        this.ofoMenuStatusListener = ofoMenuStatusListener;
    }

    public void setOfoUserIconListener(final OfoUserIconListener ofoUserIconListener) {
        menuBrawable.setOnBitmapClickListener(new MenuBrawable.OnBitmapClickListener() {
            @Override
            public void bitmapClick() {
                if (ofoUserIconListener != null) {
                    ofoUserIconListener.onClick();
                }
            }
        });
    }
}
