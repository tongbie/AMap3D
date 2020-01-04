package com.example.amap3d.views.Timetable;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.gsons.TodayTimetableGson;
import com.example.amap3d.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NextBusRecycler extends RecyclerView {

    private int itemNum = 0;
    private int itemHeight = 0;
    private boolean isShowing = false;
    private boolean isClickAble = false;
    private boolean isFirstAddSummaryView = true;

    private List<TodayTimetableGson> todayTimetableGsonList;

    private static final int DATA_SHOW_SUMMARY = 0;
    private static final int DATA_SHOW_ALL = 1;

    public NextBusRecycler(Context context) {
        super(context);
        init();
    }

    public NextBusRecycler(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NextBusRecycler(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void init() {
        itemHeight = Utils.px(MainActivity.getInstance(), 45);
        this.setBackground(getResources().getDrawable(R.drawable.bg_shadow));
        this.setElevation(Utils.px(MainActivity.getInstance(), 3));
        this.setLayoutManager(new LinearLayoutManager(NextBusRecycler.this.getContext(), LinearLayoutManager.VERTICAL, false));
    }

    @SuppressLint("SetTextI18n")
    public void setData(List<TodayTimetableGson> todayTimetableGsonList) {
        try {
            this.todayTimetableGsonList = todayTimetableGsonList;
            itemNum = 0;
            for (TodayTimetableGson item : todayTimetableGsonList) {
                itemNum += item.getDeparture_time().size();
            }
            addSummaryView();
            isClickAble = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "TimetableLayout.setData: " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    public void addSummaryView() {
        if (!isFirstAddSummaryView) {
            isShowing = false;
            startAnimatorAtMainThreadThenSendMessage(NextBusRecycler.this, getMaxHeight(), itemHeight, false, DATA_SHOW_SUMMARY);
        }
        isFirstAddSummaryView = false;
    }

    public void addAllView() {
        isShowing = true;
        startAnimatorAtMainThreadThenSendMessage(NextBusRecycler.this, itemHeight, getMaxHeight(), true, DATA_SHOW_ALL);
    }

    private void startAnimatorAtMainThreadThenSendMessage(final View view, int start, final int end, boolean isAmimatorJump, final int message) {
        NextBusRecycler.this.removeAllViews();
        final ValueAnimator animator = ValueAnimator.ofInt(start, end);
        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (isAmimatorJump) {
            animator.setInterpolator(new OvershootInterpolator());
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) (Integer) animation.getAnimatedValue();
                layoutParams.height = value;
                view.setLayoutParams(layoutParams);
                if (value == end) {
                    showDataHandler.sendEmptyMessage(message);
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

    private Handler showDataHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case DATA_SHOW_SUMMARY:
                    try {
                        TodayTimetableGson dataZero = todayTimetableGsonList.get(0);
                        String fromWhereToWhere = Datas.getAdress(dataZero.getFrom()) + " → " + Datas.getAdress(dataZero.getTo());
                        String nextTime = getNextBusTime(dataZero.getDeparture_time());
                        @SuppressLint("InflateParams") final View view = LayoutInflater.from(MainActivity.getInstance()).inflate(R.layout.layout_timetable, null);
                        ((TextView) view.findViewById(R.id.textView)).setText(fromWhereToWhere + "  " + nextTime);
                        MainActivity.getInstance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                NextBusRecycler.this.addView(view);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("ERROR: ", "TimetableLayout.showNextBus: " + e.getMessage());
                    }
                    break;
                case DATA_SHOW_ALL:
                    MainActivity.getInstance().runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            try {
                                for (TodayTimetableGson data : todayTimetableGsonList) {
                                    String fromWhereToWhere = Datas.getAdress(data.getFrom()) + " → " + Datas.getAdress(data.getTo());
                                    for (String time : data.getDeparture_time()) {
                                        @SuppressLint("InflateParams") View view = LayoutInflater.from(MainActivity.getInstance()).inflate(R.layout.layout_timetable, null);
                                        ((TextView) view.findViewById(R.id.textView)).setText(fromWhereToWhere + "  " + time);
                                        NextBusRecycler.this.addView(view);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("ERROR: ", "TimetableLayout.showAllBus: " + e.getMessage());
                            }
                        }
                    });
                    break;
                default:
            }
            return false;
        }
    });

    public boolean isShowing() {
        return isShowing;
    }

    public int getMaxHeight() {
        return itemNum * itemHeight;
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
            if (hour > curHour && minute > curMinute) {
                nextBusTime = time;
                break;
            }
        }
        return nextBusTime;
    }

    public boolean isClickAble() {
        return isClickAble;
    }
}
