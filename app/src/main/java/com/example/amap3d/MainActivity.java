package com.example.amap3d;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.amap3d.bean.UserBean;
import com.example.amap3d.common.Common;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.managers.AMapManager;
import com.example.amap3d.managers.BusDataManager;
import com.example.amap3d.managers.MQTTManager;
import com.example.amap3d.managers.UpdateManager;
import com.example.amap3d.managers.ViewManager;
import com.example.amap3d.ui.CircularLoading;
import com.google.gson.Gson;

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

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private MQTTManager mqttManager;
    private AMapManager aMapManager;
    private BusDataManager busDataManager;
    private ViewManager viewManager;
    private static Activity activity;
    private ExecutorService executorService;
    private Dialog mCircularLoading;
    private String oauth_token,oauth_verifier;
    private final String userInoUrl="http://bus.mysdnu.cn/login";

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
        getOauthCallback();
        requestUserInfo();
        mCircularLoading = CircularLoading.showLoadDialog(MainActivity.this, "加载中...", true);
        if (isNetworkAvailable()) {
            getAllData();
            update(null);
        }
    }

    private void getOauthCallback() {
        Intent intent=getIntent();
        Bundle bundle=intent.getExtras();
        String callback=bundle.getString("callback");
        String[] str1=callback.split("&oauth_verifier=");
        String str2=str1[0];
        oauth_verifier=str1[1];
        String[] str3=str2.split("oauth_token=");
        oauth_token=str3[1];
        Log.i("这是callback",callback);
        Log.i("这是两个参数",oauth_token+"......"+oauth_verifier);
    }

    private void requestUserInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    FormBody formBody=new FormBody
                            .Builder()
                            .add("oauth_token",oauth_token)
                            .add("oauth_verifier",oauth_verifier)
                            .build();
                    Request request = new Request
                            .Builder()
                            .post(formBody)
                            .url(userInoUrl)
                            .build();
                    Response response = null;
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        final String str=response.body().string();
                        Log.e("返回的数据",str);
                        Gson gson=new Gson();
                        Common.user= gson.fromJson(str,UserBean.class);
                        Log.i("这是user的所有信息",
                                Common.user.getDisplayName()
                                        +" "
                                        +Common.user.getId()
                                        +" "
                                        +Common.user.getUserName());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               CircularLoading.closeDialog(mCircularLoading);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
