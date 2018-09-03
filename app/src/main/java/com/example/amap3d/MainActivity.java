package com.example.amap3d;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.amap3d.datas.Datas;
import com.example.amap3d.managers.AMapManager;
import com.example.amap3d.managers.BusDataManager;
import com.example.amap3d.managers.MQTTManager;
import com.example.amap3d.managers.UpdateManager;
import com.example.amap3d.managers.ViewManager;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private MQTTManager mqttManager;
    private AMapManager aMapManager;
    private BusDataManager busDataManager;
    private ViewManager viewManager;
    private static Activity activity;
    private ExecutorService executorService;

    public static Activity getActivity() {
        return activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        Utils.hideTitleBar(this);
        setContentView(R.layout.activity_main);
        initManagers();
        initView(savedInstanceState);
        initObject();
        if (isNetworkAvailable()) {
            getAllData();
            update(null);
        }
    }

    private void initView(Bundle savedInstanceState) {
        viewManager.initView();
        aMapManager.initMapView(savedInstanceState);
        if (AMapManager.aMap == null) {
            AMapManager.aMap = AMapManager.mapView.getMap();
        }
        aMapManager.setAMap(AMapManager.aMap);
        aMapManager.setLocationStyle(AMapManager.aMap);
    }

    private void initObject() {
        executorService= Executors.newFixedThreadPool(1);
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

    private void initManagers() {
        mqttManager = MQTTManager.getInstance();
        aMapManager = AMapManager.getInstance();
        busDataManager = BusDataManager.getInstance();
        viewManager = new ViewManager();
    }


    public synchronized void getAllData() {
        executorService.submit(getAllDataRunnable);
    }

    private Runnable getAllDataRunnable =new Runnable() {
        @Override
        public void run() {
            mqttManager.isShowMoving = true;
            busDataManager.setBusInformationToMap();
            Datas.busPositionList = busDataManager.getBusPosition();
            aMapManager.addPoints(AMapManager.aMap);
            mqttManager.linkMQTT(mqttManager.mqttCallback);
            viewManager.refreshButton.setRefreshing(false);
            viewManager.isRefreshing = false;
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
        aMapManager.destroy();
        mqttManager.destroy();
        Datas.destroy();
        activity = null;
        super.onDestroy();
        System.exit(0);
    }
}
