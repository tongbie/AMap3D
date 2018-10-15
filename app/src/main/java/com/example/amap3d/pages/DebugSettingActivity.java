package com.example.amap3d.pages;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.datas.Fields;
import com.example.amap3d.managers.MQTTManager;
import com.example.amap3d.managers.StorageManager;
import com.example.amap3d.utils.Utils;
import com.example.amap3d.views.SwitchView;

public class DebugSettingActivity extends AppCompatActivity implements View.OnClickListener {
    private SwitchView callSwitch;
    private SwitchView receiveSwitch;
    private PopupWindow popupWindow;
    private FrameLayout feedBackLayout;
    private int currentIndex = 0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_setting);
        initView();
        getSettings();
        initPopWindow();
    }

    private void initView() {
        callSwitch = findViewById(R.id.callSwitch);
        callSwitch.setOnClickListener(this);
        receiveSwitch = findViewById(R.id.receiveSwitch);
        feedBackLayout =findViewById(R.id.feedBackLayout);
        receiveSwitch.setOnClickListener(this);
        feedBackLayout.setOnClickListener(this);
        findViewById(R.id.backImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void getSettings() {
        if (StorageManager.getSetting(Fields.SETTING_CALL)) {
            callSwitch.setOpen();
        }
        if (StorageManager.getSetting(Fields.SETTING_RECEIVE)) {
            receiveSwitch.setOpen();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.callSwitch:
                if (callSwitch.isOpen()) {
                    callSwitch.close();
                    StorageManager.storage(Fields.SETTING_CALL, "close", Fields.STORAGE_SETTINGS);
                } else {
                    StorageManager.storage(Fields.SETTING_CALL, "open", Fields.STORAGE_SETTINGS);
                    callSwitch.open();
                }
                break;
            case R.id.receiveSwitch:
                if (receiveSwitch.isOpen()) {
                    receiveSwitch.close();
                    StorageManager.storage(Fields.SETTING_RECEIVE, "close", Fields.STORAGE_SETTINGS);
                    MQTTManager.getInstance().unSubscribeTopic("BusMoveLis");
                } else {
                    popupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
                    currentIndex = 1;
                }
                break;
            case R.id.feedBackLayout:
                startActivity(new Intent(DebugSettingActivity.this,FeedBackActivity.class));
                break;
            default:
        }
    }

    private void initPopWindow() {
        popupWindow = new PopupWindow(this);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        int screenWidth = Utils.getScreenWidth(this);
        popupWindow.setWidth(screenWidth * 4 / 5);
        View view = LayoutInflater.from(MainActivity.getInstance().getApplicationContext()).inflate(R.layout.window_password, null);
        final EditText passwordEdit = view.findViewById(R.id.passwordEdit);
        view.findViewById(R.id.submitButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isTrue = "bietong".equals(passwordEdit.getText().toString());
                switch (currentIndex) {
                    case 1:
                        if (isTrue) {
                            popupWindow.dismiss();
                            StorageManager.storage(Fields.SETTING_RECEIVE, "open", Fields.STORAGE_SETTINGS);
                            MQTTManager.getInstance().subscribeTopic("BusMoveLis");
                            receiveSwitch.open();
                        } else {
                            passwordEdit.setText("密码错误");
                        }
                        break;
                    default:
                }
            }
        });
        popupWindow.setContentView(view);
    }
}
