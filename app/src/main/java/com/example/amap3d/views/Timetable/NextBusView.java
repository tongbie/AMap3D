package com.example.amap3d.views.Timetable;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.gsons.AdressGson;
import com.example.amap3d.gsons.TodayTimetableGson;
import com.example.amap3d.utils.Utils;
import com.example.amap3d.views.SelectView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NextBusView extends LinearLayout {
    int itemNum = 0;
    private int itemHeight = 0;
    private boolean isShowing = false;
    private boolean isClickAble = false;
    private boolean isFirstAddSummaryView = true;

    private SelectView selectView;

    private List<TodayTimetableGson> todayTimetableGsonList;
    private List<TodayTimetableGson> needShowTimetableItemList = new ArrayList<>();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void init() {
        itemHeight = Utils.px(MainActivity.getInstance(), 45);
        this.setBackground(getResources().getDrawable(R.drawable.bg_shadow));
        this.setElevation(Utils.px(MainActivity.getInstance(), 3));
        this.setOrientation(VERTICAL);
    }

    @SuppressLint("SetTextI18n")
    public void setData(List<TodayTimetableGson> todayTimetableGsonList) {
        try {
            this.todayTimetableGsonList = todayTimetableGsonList;
            showNextBus();
            initSelectView();
            isClickAble = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "TimetableLayout.setData: " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    public void showNextBus() {
        if (!isFirstAddSummaryView) {
            isShowing = false;
            this.removeAllViews();
            startAnimatorAtMainThread(((itemNum == 0 ? 1 : itemNum) + 1) * itemHeight, itemHeight, true);
        } else {
            addNextBusView();
        }
        isFirstAddSummaryView = false;
    }

    public void showAllBus(int id) {
        needShowTimetableItemList.clear();
        for (TodayTimetableGson data : todayTimetableGsonList) {
            if (data.getFrom() == id) {
                needShowTimetableItemList.add(data);
            }
        }
        itemNum = 0;
        for (TodayTimetableGson data : needShowTimetableItemList) {
            itemNum += data.getDeparture_time().size();
        }
        isShowing = true;
        this.removeAllViews();
        this.addView(selectView);
        final int end = ((itemNum == 0 ? 1 : itemNum) + 1) * itemHeight;
        startAnimatorAtMainThread(itemHeight, end, false);
    }

    private void startAnimatorAtMainThread(int start, final int end, final boolean isHideItem) {
        final ValueAnimator animator = ValueAnimator.ofInt(start, end);
        final ViewGroup.LayoutParams layoutParams = NextBusView.this.getLayoutParams();
        if (!isHideItem) {
            animator.setInterpolator(new OvershootInterpolator());
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) (Integer) animation.getAnimatedValue();
                layoutParams.height = value;
                NextBusView.this.setLayoutParams(layoutParams);
                if (value == end) {
                    if (isHideItem) {
                        addNextBusView();
                    } else {
                        addAllBusView();
                    }
                }
            }
        });
        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                animator.start();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void addAllBusView() {
        if (itemNum >= 1) {
            for (TodayTimetableGson data : needShowTimetableItemList) {
                String fromWhereToWhere = Datas.getAdress(data.getFrom()) + " → " + Datas.getAdress(data.getTo());
                for (String time : data.getDeparture_time()) {
                    @SuppressLint("InflateParams")
                    View view = LayoutInflater.from(MainActivity.getInstance()).inflate(R.layout.layout_timetable, null);
                    ((TextView) view.findViewById(R.id.textView)).setText(fromWhereToWhere + "  " + time);
                    this.addView(view);
                }
            }
        } else {
            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(MainActivity.getInstance()).inflate(R.layout.layout_timetable, null);
            ((TextView) view.findViewById(R.id.textView)).setText("无更多内容");
            this.addView(view);
        }
    }

    @SuppressLint("SetTextI18n")
    private void addNextBusView() {
        try {
            TodayTimetableGson dataZero = todayTimetableGsonList.get(0);
            String fromWhereToWhere = Datas.getAdress(dataZero.getFrom()) + " → " + Datas.getAdress(dataZero.getTo());
            String nextTime = getNextBusTime(dataZero.getDeparture_time());
            @SuppressLint("InflateParams") final View view = LayoutInflater.from(MainActivity.getInstance()).inflate(R.layout.layout_timetable, null);
            ((TextView) view.findViewById(R.id.textView)).setText(fromWhereToWhere + "  " + nextTime);
            MainActivity.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NextBusView.this.addView(view);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "TimetableLayout.addNextBusView: " + e.getMessage());
        }
    }

    private String getNextBusTime(List<String> timeArray) {
        String nextBusTime = "暂无";
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        Date date = new Date(System.currentTimeMillis());
        String currentTimeString = formatter.format(date);
        String[] curNums = currentTimeString.split(":");
        int curHour = Integer.parseInt(curNums[0]);
        int curMinute = Integer.parseInt(curNums[1]);
        for (String time : timeArray) {
            String[] nums = time.split(":");
            int hour = Integer.parseInt(nums[0]);
            int minute = Integer.parseInt(nums[1]);
            if (hour > curHour || (hour == curHour && minute >= curMinute)) {
                nextBusTime = time;
                break;
            }
        }
        return nextBusTime;
    }

    /*---------------------------------------------------------------------------------------------*/

    public NextBusView(Context context) {
        super(context);
        init();
    }

    public NextBusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NextBusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public boolean isShowing() {
        return isShowing;
    }

    public boolean isClickAble() {
        return isClickAble;
    }

    private void initSelectView() {
        selectView = new SelectView(getContext());
        List<Integer> idList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
        for (AdressGson adress : Datas.getAdressList()) {
            idList.add(adress.getId());
            nameList.add(adress.getName());
        }
        try {
            selectView.initData(idList, nameList);
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("地点列表获取失败");
        }
        selectView.setOnSelectListener(new SelectView.OnSelectListener() {
            @Override
            public void onSelect(int id) {
                showAllBus(id);
            }
        });
    }
}
