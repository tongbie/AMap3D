package com.example.amap3d.managers;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amap3d.Utils;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.MainActivity;
import com.example.amap3d.views.ScrollLayout;
import com.example.amap3d.R;
import com.example.amap3d.views.MenuButton;
import com.example.amap3d.views.RefreshButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewManager implements View.OnClickListener, ScrollLayout.OnScrollLayoutStateChangeListener {
    private static ViewManager viewManager;
    public PopupMenu popupMenu;
    public RefreshButton refreshButton;
    public ScrollLayout scrollLayout;
    public TextView textView;
    public RecyclerView recyclerView;
    public PopupWindow popupWindow;
    private ExecutorService executorService;

    public boolean isRefreshing = false;
    private boolean isWriteAble = true;

    private ViewManager() {
        executorService = Executors.newFixedThreadPool(1);
    }

    public static ViewManager getInstance() {
        if (viewManager == null) {
            viewManager = new ViewManager();
        }
        return viewManager;
    }

    public void initView() {
        refreshButton = MainActivity.getActivity().findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(this);
        MainActivity.getActivity().findViewById(R.id.menuButton).setOnClickListener(this);
        textView = MainActivity.getActivity().findViewById(R.id.textView);
        scrollLayout = MainActivity.getActivity().findViewById(R.id.othersScrollLayout);
        scrollLayout.setOnScrollLayoutStateChangeListener(this);
        recyclerView = MainActivity.getActivity().findViewById(R.id.recycleView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        initPopupMenu();
        initUploadPositionRemarkWindow();
    }

    private void initPopupMenu() {
        final MenuButton menuButton = MainActivity.getActivity().findViewById(R.id.menuButton);
        popupMenu = new PopupMenu(MainActivity.getActivity().getApplicationContext(), menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.main, popupMenu.getMenu());
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                menuButton.setIsShow(0);
            }
        });
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
                }
                return false;
            }
        });
    }

    private void initUploadPositionRemarkWindow() {
        popupWindow = new PopupWindow(MainActivity.getActivity());
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setBackgroundDrawable(MainActivity.getActivity().getResources().getDrawable(android.R.color.transparent));
        int screenWidth = Utils.getScreenWidth(MainActivity.getActivity());
        popupWindow.setWidth(screenWidth * 4 / 5);

        View view = LayoutInflater.from(MainActivity.getActivity().getApplicationContext()).inflate(R.layout.window_upload_position, null);
        final TextView wordCountTextView = view.findViewById(R.id.wordCountTextView);
        wordCountTextView.setText("(0/30)");
        final EditText editText = view.findViewById(R.id.uploadEditText);
        String positionRemark = StorageManager.get("positionRemark");
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

            @Override
            public void afterTextChanged(Editable s) {
                int content = editText.getText().length();
                if (content > 30) {
                    isWriteAble = false;
                }
                wordCountTextView.setText("(" + content + "/30)");
            }
        });
        //TODO:备注完成点击事件
        view.findViewById(R.id.completeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                final String text = editText.getText().toString();
                StorageManager.storage("positionRemark", text);
                PeopleManager.getInstance().setRemark(text);
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
            case R.id.menuButton:
                MenuButton menuButton = ((MenuButton) view);
                if (menuButton.getIsShow() != 1) {
                    menuButton.setIsShow(1);
                } else if (menuButton.getIsShow() == 1) {
                    menuButton.setIsShow(0);
                }
                popupMenu.show();
                break;
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
        textView.setText("点击或下拉返回");
    }

    @Override
    public void scrollDownEnd() {
        textView.setText("上滑查看班车时刻");
    }
}
