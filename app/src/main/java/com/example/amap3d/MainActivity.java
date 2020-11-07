package com.example.amap3d;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.amap3d.datas.Datas;
import com.example.amap3d.managers.AMapManager;
import com.example.amap3d.managers.BusManager;
import com.example.amap3d.managers.MQTTManager;
import com.example.amap3d.managers.PeopleManager;
import com.example.amap3d.managers.UpdateManager;
import com.example.amap3d.managers.ViewManager;
import com.example.amap3d.utils.Utils;
import com.example.amap3d.views.ScrollLayout;
import com.example.amap3d.views.ofoMenuView.OfoMenuManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    private static Activity activity;
    private ExecutorService taskExecutorService;

    public static Activity getInstance() {
        return activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity = this;
//        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
//        layoutParams.layoutAnimationParameters
//        layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;


        setContentView(R.layout.activity_main);
        initView(savedInstanceState);
        if (isNetworkAvailable()) {
            requireAllData();
            update(null);
        }
    }

    private void initView(Bundle savedInstanceState) {
        taskExecutorService = Executors.newCachedThreadPool();
        ViewManager.getInstance().initViewInNewThread();
        AMapManager.getInstance().initMapView(savedInstanceState);
        if (AMapManager.aMap == null) {
            AMapManager.aMap = AMapManager.mapView.getMap();
        }
        taskExecutorService.execute(setAMapRunnable);
    }

    private Runnable setAMapRunnable=new Runnable() {
        @Override
        public void run() {
            AMapManager.getInstance().setAMap();
            AMapManager.getInstance().setLocationStyle();
        }
    };

    public void update(final String text) {
        if (isNetworkAvailable()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int updateState = UpdateManager.isNeedUpdate(getApplicationContext());
                    if (updateState == UpdateManager.UPDATE_NOT_NEED) {
                        if (text != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        return;
                    }
                    UpdateManager.getInstance().dealWithUpdateState(updateState);
                }
            }).start();
        }
    }

    public synchronized void requireAllData() {
//        int threadCount = ((ThreadPoolExecutor) taskExecutorService).getActiveCount();
        taskExecutorService.execute(requireMainDataRunnable);
        taskExecutorService.execute(requireOtherDataRunnable);
    }

    private Runnable requireMainDataRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                MQTTManager.getInstance().isShowMoving = true;
                BusManager.getInstance().requireBusInformation();
                Datas.setBusPositionList(BusManager.getInstance().requireBusPosition());
                AMapManager.getInstance().addBusMarker();
                MQTTManager.getInstance().linkMQTT(MQTTManager.getInstance().mqttCallback);
                ViewManager.getInstance().refreshButton.setRefreshing(false);
                ViewManager.getInstance().isRefreshing = false;
            } catch (Exception e) {
                Utils.uiToast("数据获取失败");
                e.printStackTrace();
            }
        }
    };

    private Runnable requireOtherDataRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                ViewManager.getInstance().setTimetableHintTextInUiThread("数据加载中...");
                PeopleManager.getInstance().requireAllPosition();
                PeopleManager.getInstance().requireUserInfo();
                BusManager.getInstance().requireAddressAndTodayTimetable();
                BusManager.getInstance().requireAllBusTimetable();
            } catch (Exception e) {
                Utils.uiToast("数据获取失败");
                e.printStackTrace();
            }
        }
    };

    public boolean isNetworkAvailable() {
        boolean networkState = true;
        if (!Utils.checkNetworkState(this)) {
            Toast.makeText(this, "网络连接不可用", Toast.LENGTH_SHORT).show();
            networkState = false;
        }
        return networkState;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0x003:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Utils.uiToast("未获得拨号权限，无法拨打电话");
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AMapManager.mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AMapManager.mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AMapManager.mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        AMapManager.getInstance().destroy();
        PeopleManager.getInstance().destroy();
        MQTTManager.getInstance().destroy();
        Datas.destroy();
        activity = null;
        taskExecutorService.shutdown();
        super.onDestroy();
        System.exit(0);
    }

    private long lastBackClickTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        OfoMenuManager ofoMenuManager = ViewManager.getInstance().ofoMenuManager;
        if (ofoMenuManager.isOpen()) {
            ofoMenuManager.close();
            return true;
        }
        ScrollLayout scrollLayout = ViewManager.getInstance().scrollLayout;
        if (scrollLayout.isOpen()) {
            scrollLayout.close();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if ((System.currentTimeMillis() - lastBackClickTime) > 1500) {
                Toast.makeText(MainActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                lastBackClickTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
