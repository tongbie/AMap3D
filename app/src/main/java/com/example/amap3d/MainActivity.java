package com.example.amap3d;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.amap3d.datas.Datas;
import com.example.amap3d.managers.AMapManager;
import com.example.amap3d.managers.BusManager;
import com.example.amap3d.managers.MQTTManager;
import com.example.amap3d.managers.PeopleManager;
import com.example.amap3d.managers.UpdateManager;
import com.example.amap3d.managers.ViewManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static Activity activity;
    private ExecutorService refreshExecutorService;

    public static Activity getActivity() {
        return activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        Utils.hideTitleBar(this);
        setContentView(R.layout.activity_main);
        initView(savedInstanceState);
        if (isNetworkAvailable()) {
            getAllData();
            update(null);
        }

    }

    private void initView(Bundle savedInstanceState) {
        refreshExecutorService = Executors.newFixedThreadPool(1);
        ViewManager.getInstance().initView();
        AMapManager.getInstance().initMapView(savedInstanceState);
        if (AMapManager.aMap == null) {
            AMapManager.aMap = AMapManager.mapView.getMap();
        }
        AMapManager.getInstance().setAMap();
        AMapManager.getInstance().setLocationStyle();
    }

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

    public synchronized void getAllData() {
        refreshExecutorService.submit(getAllDataRunnable);
    }

    private Runnable getAllDataRunnable =new Runnable() {
        @Override
        public void run() {
            MQTTManager.getInstance().isShowMoving = true;
            BusManager.getInstance().setBusInformationToMap();
            Datas.busPositionList = BusManager.getInstance().getBusPosition();
            AMapManager.getInstance().addPoints();
            MQTTManager.getInstance().linkMQTT(MQTTManager.getInstance().mqttCallback);
            PeopleManager.getInstance().getAllPosition();
            PeopleManager.getInstance().getPeopleRemark(MQTTManager.deviceId);
            ViewManager.getInstance().refreshButton.setRefreshing(false);
            ViewManager.getInstance().isRefreshing = false;
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
        super.onDestroy();
        System.exit(0);
    }
}
