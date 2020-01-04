package com.example.amap3d.managers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amap3d.pages.DebugSettingActivity;
import com.example.amap3d.LoginActivity;
import com.example.amap3d.R;
import com.example.amap3d.pages.ReviseRemarkActivity;
import com.example.amap3d.datas.Fields;
import com.example.amap3d.utils.Utils;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.MainActivity;
import com.example.amap3d.views.ScrollLayout;
import com.example.amap3d.views.RefreshButton;
import com.example.amap3d.views.TimetableAdapter;
import com.example.amap3d.views.Timetable.NextBusView;
import com.example.amap3d.views.ofoMenuView.MenuBrawable;
import com.example.amap3d.views.ofoMenuView.OfoContentLayout;
import com.example.amap3d.views.ofoMenuView.OfoMenuManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ViewManager implements View.OnClickListener, ScrollLayout.OnScrollLayoutStateChangeListener {
    @SuppressLint("StaticFieldLeak")
    private long lastRefreshStamp = 0;

    public NextBusView nextBusView;
    public RefreshButton refreshButton;
    public ScrollLayout scrollLayout;
    private TextView timetableHintText;
    private PopupWindow popupWindow;
    private ImageView timetableImage;
    private ImageView userImage;
    private TextView userNameText;
    private TextView userPositionText;

    public boolean isRefreshing = false;

    private ViewManager() {
    }

    private static class ViewManagerFactory {
        public static ViewManager instance = new ViewManager();
    }

    public static ViewManager getInstance() {
        return ViewManagerFactory.instance;
    }

    public void initViewInNewThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Activity mainActivity = MainActivity.getInstance();
                nextBusView = mainActivity.findViewById(R.id.timetableLayout);
                nextBusView.setOnClickListener(ViewManager.this);
                refreshButton = mainActivity.findViewById(R.id.refreshButton);
                refreshButton.setOnClickListener(ViewManager.this);
                mainActivity.findViewById(R.id.aMapButton).setOnClickListener(ViewManager.this);
                timetableImage = mainActivity.findViewById(R.id.timetableImage);
                timetableHintText = mainActivity.findViewById(R.id.timetableHintText);
                scrollLayout = mainActivity.findViewById(R.id.othersScrollLayout);
                scrollLayout.setOnScrollLayoutStateChangeListener(ViewManager.this);
                userImage = mainActivity.findViewById(R.id.userImage);
                userNameText = mainActivity.findViewById(R.id.userNameText);
                userPositionText = mainActivity.findViewById(R.id.userPositionText);
                initUploadRemarkWindow();
                timetableHintText.setOnClickListener(ViewManager.this);
                userImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loginWithJudge(null);
                        closeScrollLayoutWithJudge();
                    }
                });
                mainActivity.findViewById(R.id.uploadPositionLayout).setOnClickListener(ViewManager.this);
                mainActivity.findViewById(R.id.settingLayout).setOnClickListener(ViewManager.this);
                initOfoMenuView(mainActivity);
            }
        }).start();
    }

    private void loginWithJudge(String nullAbleText) {
        if (!Utils.isLogin()) {
            MainActivity.getInstance().startActivity(new Intent(MainActivity.getInstance(), LoginActivity.class));
        } else {
            if (nullAbleText != null) {
                Toast.makeText(MainActivity.getInstance(), nullAbleText, Toast.LENGTH_SHORT).show();
            }
        }
    }

    void setUserPosition(String userPosition) {
        userPositionText.setText(userPosition);
    }

    @SuppressLint("SetTextI18n")
    private void initUploadRemarkWindow() {
        popupWindow = new PopupWindow(MainActivity.getInstance());
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setBackgroundDrawable(MainActivity.getInstance().getResources().getDrawable(android.R.color.transparent));
        int screenWidth = Utils.getScreenWidth(MainActivity.getInstance());
        popupWindow.setWidth(screenWidth * 4 / 5);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(MainActivity.getInstance().getApplicationContext()).inflate(R.layout.window_upload_position, null);
        final TextView wordCountTextView = view.findViewById(R.id.wordCountTextView);
        wordCountTextView.setText("(0/30)");
        final EditText editText = view.findViewById(R.id.uploadEditText);
        String positionRemark = StorageManager.get(Fields.STORAGE_REMARK);
        if (positionRemark != null) {
            editText.setText(positionRemark);
            wordCountTextView.setText("(" + positionRemark.length() + "/30)");
        }
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return false;
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void afterTextChanged(Editable s) {
                int content = editText.getText().length();
                wordCountTextView.setText("(" + content + "/30)");
            }
        });
        view.findViewById(R.id.completeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                String text = editText.getText().toString();
                StorageManager.storage(Fields.STORAGE_REMARK, text);
                if (text.length() > 30) {
                    Toast.makeText(MainActivity.getInstance(), "字数超过限制", Toast.LENGTH_SHORT).show();
                } else {
                    PeopleManager.getInstance().uploadRemark(text, true);
                }
            }
        });

        view.findViewById(R.id.cancleButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        popupWindow.setContentView(view);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refreshButton:
                if (((MainActivity) MainActivity.getInstance()).isNetworkAvailable()) {
                    if (System.currentTimeMillis() - lastRefreshStamp > 1000) {
                        refresh();
                    }
                    lastRefreshStamp = System.currentTimeMillis();
                }
                break;
            case R.id.uploadPositionLayout:
                if (popupWindow == null) {
                    initUploadRemarkWindow();
                }
                popupWindow.showAtLocation(MainActivity.getInstance().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                break;
            case R.id.settingLayout:
                closeScrollLayoutWithJudge();
                if (ofoMenuManager != null && !ofoMenuManager.isOpen()) {
                    ofoMenuManager.open();
                }
                break;
            case R.id.timetableHintText:
                if (scrollLayout.isOpen()) {
                    scrollLayout.close();
                    timetableImage.setImageResource(R.drawable.icon_timetable);
                } else {
                    scrollLayout.open();
                    timetableImage.setImageResource(R.drawable.icon_map);
                }
                break;
            case R.id.aMapButton:
                AMapManager.getInstance().backToMyPosition();
                break;
            case R.id.timetableLayout:
                if(nextBusView.isClickAble()) {
                    if (nextBusView.isShowing()) {
                        nextBusView.showNextBus();
                    } else {
                        nextBusView.showAllBus(Datas.getCurrentSelectorId());
                    }
                }
            default:
        }
    }

    private void closeScrollLayoutWithJudge() {
        if (scrollLayout.isOpen()) {
            scrollLayout.close();
        }
    }

    private void refresh() {
        if (isRefreshing) {
            return;
        }
        refreshButton.setRefreshing(true);
        Toast.makeText(MainActivity.getInstance(), "正在刷新...", Toast.LENGTH_SHORT).show();
        AMapManager.getInstance().removeAllMarker();
        Datas.clear();
        isRefreshing = true;
        ((MainActivity) MainActivity.getInstance()).requireAllData();
    }

    @Override
    public void startingScrollUp(int currentHeight) {
        timetableHintText.setText("显示地图");
    }

    @Override
    public void scrollDownEnd(int currentHeight) {
        timetableHintText.setText("校车时刻");
    }

    void setBusTimetableInUiThread(final List<Map<String, String>> timetableList) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.getInstance());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final RecyclerView recyclerView = MainActivity.getInstance().findViewById(R.id.recycleView);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(new TimetableAdapter(timetableList));
                timetableHintText.setText("校车时刻");
            }
        });
    }

    public OfoMenuManager ofoMenuManager;

    private void initOfoMenuView(final Activity mainActivity) {
        Window window = mainActivity.getWindow();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        ofoMenuManager = new OfoMenuManager.Builder(mainActivity)
                .setRadian(MenuBrawable.CONVEX)
                .setOfoBackColor(android.R.color.holo_blue_light)
                .setOfoPosition(R.dimen.ofo_menu_height)
                .addItemContentView(R.layout.ofo_item_login)
                .addItemContentView(R.layout.ofo_item_update)
                .addItemContentView(R.layout.ofo_item_remark)
                .addItemContentView(R.layout.ofo_itme_position)
                .addItemContentView(R.layout.ofo_item_debug)
                .addItemContentView(R.layout.ofo_item_logout)
                .build();
        ((ViewGroup) mainActivity.findViewById(android.R.id.content)).addView(ofoMenuManager.getRootView());
        ofoMenuManager.setUserIcon(R.drawable.icon_logout);
        ofoMenuManager.setOnItemClickListener(new OfoContentLayout.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                switch (position) {
                    case 0:
                        loginWithJudge("已登录");
                        break;
                    case 1:
                        ((MainActivity) MainActivity.getInstance()).update("已是最新版本");
                        break;
                    case 2:
                        MainActivity.getInstance().startActivity(new Intent(MainActivity.getInstance(), ReviseRemarkActivity.class));
                        break;
                    case 3:
                        PeopleManager.getInstance().uploadRemark(StorageManager.get(Fields.STORAGE_REMARK), true);
                        break;
                    case 4:
                        MainActivity.getInstance().startActivity(new Intent(MainActivity.getInstance(), DebugSettingActivity.class));
                        break;
                    case 5:
                        StorageManager.delete(Fields.STORAGE_COOKIE);
                        Intent intent = new Intent(MainActivity.getInstance(), LoginActivity.class);
                        intent.putExtra("isLogOut", 1);
                        MainActivity.getInstance().startActivity(intent);
                        break;
                    default:
                }
            }
        });
    }

    public void setTimetableHintTextInUiThread(final String text) {
        if (timetableHintText != null) {
            MainActivity.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timetableHintText.setText(text);
                }
            });
        }
    }

    void setUserViews(final boolean isLogin) {
        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isLogin) {
                    userImage.setImageResource(R.drawable.icon_login);
                    ofoMenuManager.setUserIcon(R.drawable.icon_login);
                    userNameText.setText(Datas.getUserInfo().getDisplayName());
                } else {
                    userImage.setImageResource(R.drawable.icon_logout);
                    ofoMenuManager.setUserIcon(R.drawable.icon_logout);
                    userNameText.setText("未登录");
                }
            }
        });
    }
}
