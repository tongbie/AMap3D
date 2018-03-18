package com.example.amap3d;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.amap3d.Gsons.AccountGson;
import com.example.amap3d.Gsons.BusDataGson;
import com.example.amap3d.Gsons.BusMoveGson;
import com.example.amap3d.Gsons.BusPositionGson;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MapView mapView;
    private AMap aMap;
    private OkHttpClient client;
    private Gson gson;
    private boolean isFirstMove = true;
    private MqttConnectOptions mqttOptions;
    private MqttClient mqttClient;

    private HashMap<String, String[]> busDataMap;
    private HashMap<String,Marker> markerMap;
    private List<BusPositionGson> busPositionList =new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        init();
        setAMap();
        setLocationStyle();
        initData();
    }

    private void setLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(1000);
        myLocationStyle.showMyLocation(true);
        myLocationStyle.radiusFillColor(Color.parseColor("#00000000"));
        myLocationStyle.strokeColor(Color.parseColor("#00000000"));
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.me));//设置蓝点图标
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setMyLocationEnabled(true);
    }

    private void init() {
        findViewById(R.id.refresh).setOnClickListener(this);
        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        gson = new Gson();

        markerMap=new HashMap<>();
        busDataMap = new HashMap<>();

        mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);
        mqttOptions.setConnectionTimeout(20);// 设置超时时间 单位为秒
        mqttOptions.setKeepAliveInterval(120);
    }

    private void initPoints() {
        if(busPositionList==null){
            toast("数据异常");
            return;
        }
        for(BusPositionGson busPosition:busPositionList){
            String key = busPosition.getGPSDeviceIMEI();
            LatLng latLng = new LatLng(Double.parseDouble(busPosition.getLat()),Double.parseDouble(busPosition.getLng()));
            Marker marker=aMap.addMarker(new MarkerOptions().position(latLng).title(busDataMap.get(key)[0]).snippet(busDataMap.get(key)[1]));
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus1));
            markerMap.put(key,marker);
        }
    }

//    List<BitmapDescriptor> icon;

    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
//            if(icon==null){
//                icon=marker.getIcons();
//            }
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
//                marker.setIcon(icon);
            } else {
                marker.showInfoWindow();
//                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.icon_openmap_mark));
            }
            return true;
        }
    };

    private void initData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                initBusData();
                initBusPosition();
                initPoints();
                initMQTT();
            }
        }).start();
    }

    private void initBusData(){
        try {
            Request request = new Request.Builder()
                    .url("http://111.231.201.179/android/bus")
                    .build();
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    Log.e("busDataRunnable", responseData);
                    List<BusDataGson> busDatas = gson.fromJson(responseData, new TypeToken<List<BusDataGson>>() {
                    }.getType());
                    for (BusDataGson busData : busDatas) {
                        String key = busData.getGPSDeviceIMEI();
                        String title = busData.getBus_lineName() + "\n" + busData.getBus_departureSite();
                        String snippet = busData.getBus_arriveSite();
                        busDataMap.put(key, new String[]{title, snippet});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    toast("数据异常");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("数据获取失败");
        }
    }

    private void initBusPosition(){
        try {
            Request request = new Request.Builder()
                    .url("http://111.231.201.179/android/bus/location")
                    .build();
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    Log.e("busPositionRunnable", responseData);
                    busPositionList = gson.fromJson(responseData, new TypeToken<List<BusPositionGson>>() {
                    }.getType());
                } catch (Exception e) {
                    e.printStackTrace();
                    toast("数据异常");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("数据获取失败");
        }
    }

    private void initMQTT(){
        try {
            Request request = new Request.Builder()
                    .url("http://111.231.201.179/bus/mqtt")
                    .build();
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    Log.e("mqttRunnable", responseData);
                    AccountGson account = gson.fromJson(responseData, AccountGson.class);
                    String username = account.getUsername();
                    String password = account.getPassword();
                    TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                    @SuppressLint("MissingPermission") String ssn = tm.getSimSerialNumber();

                    mqttOptions.setUserName(username);
                    mqttOptions.setPassword(password.toCharArray());
                    mqttClient = new MqttClient("tcp://111.231.201.179:1880", ssn, new MemoryPersistence());
                    if (mqttClient.isConnected()) {
                        toast("服务器已连接");
                    } else {
                        mqttClient.setCallback(mqttCallback);
                        mqttClient.connect(mqttOptions);
                        subscribeMsg("BusMoveList", 0);
                        if (!mqttClient.isConnected())
                            throw new Exception("throw");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    toast("连接服务器失败");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void subscribeMsg(String topic, int qos) {
        if (mqttClient != null) {
            int[] Qos = {qos};
            String[] topic1 = {topic};
            try {
                mqttClient.subscribe(topic1, Qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void translate() {
        // 获取轨迹坐标点
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(39.906901, 116.397972));
        points.add(new LatLng(42.906901, 117.397982));

        LatLngBounds bounds = new LatLngBounds(points.get(0), points.get(points.size() - 2));
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

        SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
//        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher));// 设置滑动的图标

        LatLng drivePoint = points.get(0);
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(points, drivePoint);
        points.set(pair.first, drivePoint);
        List<LatLng> subList = points.subList(pair.first, points.size());

        smoothMarker.setPoints(subList);// 设置滑动的轨迹左边点
        smoothMarker.setTotalDuration(40);// 设置滑动的总时间
        smoothMarker.startSmoothMove();// 开始滑动
    }

    private void setAMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (!isFirstMove) {
                    aMap.setOnMyLocationChangeListener(null);
                }
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                isFirstMove = false;
            }
        });
        aMap.setOnMarkerClickListener(markerClickListener);

        UiSettings mUiSettings;
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setRotateGesturesEnabled(false);
        mUiSettings.setTiltGesturesEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh:
                refresh();
                break;
        }
    }

    private void refresh() {
        busDataMap.clear();
        busPositionList.clear();
        markerMap.clear();
        initData();
    }

    private void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            toast(topic);
            Log.e("mqttmessage", message.toString());
            try {
                List<BusMoveGson> busMoveGsons=gson.fromJson(message.toString(), new TypeToken<List<BusMoveGson>>() {
                }.getType());
                for(BusMoveGson busMoveGson:busMoveGsons){
                    String key=busMoveGson.getGPSDeviceIMEI();
                    markerMap.get(key).remove();
                    markerMap.remove(key);
                    Marker marker=aMap.addMarker(new MarkerOptions()
                            .position(new LatLng(busMoveGson.getLat(),busMoveGson.getLng()))
                            .title(busDataMap.get(key)[0])
                            .snippet(busDataMap.get(key)[1]));
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus1));
                    markerMap.put(key,marker);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    private void toast(final String text) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    /*private void getBusPosition() {
        new Thread(busPosition).start();
    }*/

    /*private Runnable mqtt = new Runnable() {
        @Override
        public void run() {
            try {
                Request request = new Request.Builder()
                        .url("http://111.231.201.179/bus/mqtt")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                if (responseData != null && responseCode.charAt(0) == '2') {
                    try {
                        Log.e("mqttRunnable", responseData);
                        AccountGson account = gson.fromJson(responseData, AccountGson.class);
                        String username = account.getUsername();
                        String password = account.getPassword();
                        TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                        @SuppressLint("MissingPermission") String ssn = tm.getSimSerialNumber();

                        mqttOptions.setUserName(username);
                        mqttOptions.setPassword(password.toCharArray());
                        mqttClient = new MqttClient("tcp://111.231.201.179:1880", ssn, new MemoryPersistence());
                        if (mqttClient.isConnected()) {
                            toast("服务器已连接");
                        } else {
                            mqttClient.setCallback(mqttCallback);
                            mqttClient.connect(mqttOptions);
                            subscribeMsg("BusMoveList", 0);
                            if (!mqttClient.isConnected())
                                throw new Exception("throw");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("连接服务器失败");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };*/

    /*private void linkMQTT() {
        new Thread(mqtt).start();
    }*/

    /*private Runnable busData = new Runnable() {
        @Override
        public void run() {
            try {
                Request request = new Request.Builder()
                        .url("http://111.231.201.179/android/bus")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                if (responseData != null && responseCode.charAt(0) == '2') {
                    try {
                        Log.e("busDataRunnable", responseData);
                        List<BusDataGson> busDatas = gson.fromJson(responseData, new TypeToken<List<BusDataGson>>() {
                        }.getType());
                        for (BusDataGson busData : busDatas) {
                            String key = busData.getGPSDeviceIMEI();
                            String title = busData.getBus_lineName() + "\n" + busData.getBus_departureSite();
                            String snippet = busData.getBus_arriveSite();
                            busDataMap.put(key, new String[]{title, snippet});
                            getBusPosition();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("数据异常");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("数据获取失败");
            }
        }
    };*/

    /*private void getBusData() {
        new Thread(busData).start();
    }*/

    /*private Runnable busPosition = new Runnable() {
        @Override
        public void run() {
            try {
                Request request = new Request.Builder()
                        .url("http://111.231.201.179/android/bus/location")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                if (responseData != null && responseCode.charAt(0) == '2') {
                    try {
                        Log.e("busPositionRunnable", responseData);
                        busPositionList = gson.fromJson(responseData, new TypeToken<List<BusPositionGson>>() {
                        }.getType());
                        addPoints();
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("数据异常");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("数据获取失败");
            }
        }
    };*/
}
