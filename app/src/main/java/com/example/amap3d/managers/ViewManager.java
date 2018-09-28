package com.example.amap3d.managers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amap3d.LoginActivity;
import com.example.amap3d.R;
import com.example.amap3d.datas.Fields;
import com.example.amap3d.utils.Utils;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.MainActivity;
import com.example.amap3d.views.ScrollLayout;
import com.example.amap3d.views.RefreshButton;
import com.example.amap3d.views.TimetableAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ViewManager implements View.OnClickListener, ScrollLayout.OnScrollLayoutStateChangeListener {
    @SuppressLint("StaticFieldLeak")
    private static ViewManager viewManager;

    private PopupMenu popupMenu;
    public RefreshButton refreshButton;
    private ScrollLayout scrollLayout;
    private TextView timetableHintText;
    private PopupWindow popupWindow;
    private ImageView timetableImage;
    private ImageView userImage;
    private TextView userNameText;
    private TextView userRemarkText;

    public boolean isRefreshing = false;

    private ViewManager() {
    }

    public static ViewManager getInstance() {
        if (viewManager == null) {
            viewManager = new ViewManager();
        }
        return viewManager;
    }

    public void initView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Activity mainActivity = MainActivity.getActivity();
                refreshButton = mainActivity.findViewById(R.id.refreshButton);
                refreshButton.setOnClickListener(ViewManager.this);
                timetableImage = mainActivity.findViewById(R.id.timetableImage);
                timetableHintText = mainActivity.findViewById(R.id.timetableHintText);
                scrollLayout = mainActivity.findViewById(R.id.othersScrollLayout);
                scrollLayout.setOnScrollLayoutStateChangeListener(ViewManager.this);
                userImage = mainActivity.findViewById(R.id.userImage);
                userNameText = mainActivity.findViewById(R.id.userNameText);
                userRemarkText = mainActivity.findViewById(R.id.userRemarkText);
                initPopupMenu();
                initUploadPositionRemarkWindow();
                requireBusTimetable();
                timetableHintText.setOnClickListener(ViewManager.this);
                userImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        login();
                    }
                });
                mainActivity.findViewById(R.id.uploadPositionLayout).setOnClickListener(ViewManager.this);
                mainActivity.findViewById(R.id.settingLayout).setOnClickListener(ViewManager.this);
            }
        }).start();
    }

    private ExecutorService loginService;

    private void login() {
        if (loginService == null) {
            loginService = Executors.newFixedThreadPool(1);
        }
        if (((ThreadPoolExecutor) loginService).getActiveCount() > 0) {
            return;
        }
        loginService.submit(new Runnable() {
            @Override
            public void run() {
                PeopleManager.getInstance().attemptLogin(true);
            }
        });
    }

    private void initPopupMenu() {
        popupMenu = new PopupMenu(MainActivity.getActivity().getApplicationContext(), null);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.timeTable:
                        scrollLayout.open();
                        break;
                    case R.id.upDate:
                        ((MainActivity) MainActivity.getActivity()).update("已是最新版本");
                        break;
                    case R.id.uploadPosition:
                        if (popupWindow == null) {
                            initUploadPositionRemarkWindow();
                        }
                        popupWindow.showAtLocation(MainActivity.getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                        break;
                    case R.id.logOut:
                        PeopleManager.getInstance().deleteKey();
                        Utils.clearUserInfo();
                        Intent intent = new Intent(MainActivity.getActivity(), LoginActivity.class);
                        intent.putExtra("isLogOut", 1);
                        MainActivity.getActivity().startActivity(intent);
                        break;
//                    case R.id.userImage:
//                        Toast.makeText(MainActivity.getActivity(), "登录中", Toast.LENGTH_SHORT).show();
//                        PeopleManager.getInstance().attemptLogin(true);
//                        break;
                    default:
                }
                return false;
            }
        });
    }

    public void setUserView(final boolean isLogin) {
        MainActivity.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isLogin) {
                    userImage.setImageResource(R.drawable.icon_login);
                    userNameText.setText(Datas.userInfo.getDisplayName());
                    userRemarkText.setText("备注 " + Datas.userInfo.getRemark());
                    Log.e("setUserView", Datas.userInfo.getUserName());
                } else {
                    userImage.setImageResource(R.drawable.icon_logout);
                    userNameText.setText("未登录");
                    userRemarkText.setText("备注 ");
                }
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void initUploadPositionRemarkWindow() {
        popupWindow = new PopupWindow(MainActivity.getActivity());
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setBackgroundDrawable(MainActivity.getActivity().getResources().getDrawable(android.R.color.transparent));
        int screenWidth = Utils.getScreenWidth(MainActivity.getActivity());
        popupWindow.setWidth(screenWidth * 4 / 5);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(MainActivity.getActivity().getApplicationContext()).inflate(R.layout.window_upload_position, null);
        final TextView wordCountTextView = view.findViewById(R.id.wordCountTextView);
        wordCountTextView.setText("(0/50)");
        final EditText editText = view.findViewById(R.id.uploadEditText);
        String positionRemark = StorageManager.get(Fields.STORAGE_REMARK);
        if (positionRemark != null) {
            editText.setText(positionRemark);
            wordCountTextView.setText("(" + positionRemark.length() + "/50)");
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
                wordCountTextView.setText("(" + content + "/50)");
            }
        });
        view.findViewById(R.id.completeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                String text = editText.getText().toString();
                StorageManager.storage(Fields.STORAGE_REMARK, text);
                if (text.length() > 50) {
                    Toast.makeText(MainActivity.getActivity(), "字数超过限制", Toast.LENGTH_SHORT).show();
                } else {
                    PeopleManager.getInstance().uploadRemark(text);
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
                if (((MainActivity) MainActivity.getActivity()).isNetworkAvailable()) {
                    refresh();
                }
                break;
            case R.id.uploadPositionLayout:
                if (popupWindow == null) {
                    initUploadPositionRemarkWindow();
                }
                popupWindow.showAtLocation(MainActivity.getActivity().getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                break;
            case R.id.settingLayout:

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
            case R.id.userImage:
                break;
            default:
        }
    }

    private void refresh() {
        if (isRefreshing) {
            return;
        }
        refreshButton.setRefreshing(true);
        Toast.makeText(MainActivity.getActivity(), "正在刷新...", Toast.LENGTH_SHORT).show();
        AMapManager.getInstance().removeAllMarker();
        Datas.clear();
        isRefreshing = true;
        ((MainActivity) MainActivity.getActivity()).getAllData();
    }

    @Override
    public void startingScrollUp() {
        timetableHintText.setText("显示地图");
    }

    @Override
    public void scrollDownEnd() {
        timetableHintText.setText("校车时刻");
    }

    private void requireBusTimetable() {
        timetableHintText.setText("数据加载中...");
        String data = "{\"千佛山校区 → 长清湖校区\":[{\"routeOrder\":1,\"routeTitle\":\"千佛山校区 → 长清湖校区\",\"routeStart\":\"学校北门\",\"timeOrder\":1,\"timeTitle\":\"7月7日～7月13日\",\"timeList\":\"7:10,13:00,18:00\"}],\"长清湖校区 → 千佛山校区\":[{\"routeOrder\":2,\"routeTitle\":\"长清湖校区 → 千佛山校区\",\"routeStart\":\"教学楼、大学生活动中心\",\"timeOrder\":1,\"timeTitle\":\"7月7日～7月13日\",\"timeList\":\"7:00  11:50  16:40,17:30,21:00\"}],\"龙泉山庄 →阳光舜城 → 长清湖校区\":[{\"routeOrder\":3,\"routeTitle\":\"龙泉山庄 →阳光舜城 → 长清湖校区\",\"routeStart\":\"龙泉山庄\",\"timeOrder\":1,\"timeTitle\":\"7月7日～7月13日\",\"timeList\":\"7:10,13:00\"}],\"长清湖校区 → 阳光舜城 → 龙泉山庄\":[{\"routeOrder\":4,\"routeTitle\":\"长清湖校区 → 阳光舜城 → 龙泉山庄\",\"routeStart\":\"教学楼、大学生活动中心\",\"timeOrder\":1,\"timeTitle\":\"7月7日～7月13日\",\"timeList\":\"11:50  17:30\"}]}";
        decodeJson(data);
    }

    private void decodeJson(String result) {
        if (result.length() == 0)
            return;
        List<Map<String, String>> timetableList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(result);
            Iterator<?> it = jsonObject.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                JSONArray jsonArray = jsonObject.getJSONArray(key);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobject = jsonArray.getJSONObject(i);
                    String routeTitle = jsonobject.getString("routeTitle");
                    String timeTitle = jsonobject.getString("timeTitle");
                    String timeList = jsonobject.getString("timeList");
                    Map<String, String> timetable = new HashMap<>();
                    timetable.put("routeTitle", routeTitle);
                    timetable.put("timeTitle", timeTitle);
                    timetable.put("timeList", timeList);
                    timetableList.add(timetable);
                }
            }
            RecyclerView recyclerView = MainActivity.getActivity().findViewById(R.id.recycleView);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.getActivity());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setAdapter(new TimetableAdapter(timetableList));
            timetableHintText.setText("校车时刻");
        } catch (Exception e) {
            timetableHintText.setText("校车时刻");
            e.printStackTrace();
        }
    }
}
