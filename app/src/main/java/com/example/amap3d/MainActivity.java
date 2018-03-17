package com.example.amap3d;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MapView mapView;
    private AMap aMap;
    private List<LatLng> latLngs;

    private OkHttpClient client;
    private Gson gson;
    private Request request;
    private boolean isFirstMove = true;

    private List<BusDataGson> busDataList;
    private HashMap<String, String> busDataMap;
    private List<BusPositionGson> busPositionList;
    private HashMap<String, LatLng> busPositionMap;

    private MqttConnectOptions mqttOptions;
    private MqttClient mqttClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        init();
        setAMap();
        setLocationStyle();
        getBusData();
//        requestData("http://111.231.201.179/android/bus", "getBusData");
        getBusPosition();
        linkMQTT();
    }

    private void getBusPosition() {
        new Thread(busPosition).start();
    }

    private void setLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(1000);
        myLocationStyle.showMyLocation(true);
        myLocationStyle.radiusFillColor(Color.parseColor("#00000000"));
        myLocationStyle.strokeColor(Color.parseColor("#00000000"));
//        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.icon_openmap_mark));//设置蓝点图标
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

        latLngs = new ArrayList<>();
        busPositionList = new ArrayList<>();
        busDataMap = new HashMap<>();
        busPositionMap = new HashMap<>();

        mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);
        mqttOptions.setConnectionTimeout(20);// 设置超时时间 单位为秒
        mqttOptions.setKeepAliveInterval(20);
    }

    private void addPoints() {
/*//        latLngs=new ArrayList<>();
//        List<ClusterItem> clusterItem = new ArrayList<ClusterItem>();
        for (BusPositionGson busPosition : busPositionList) {
            double longitude = Double.parseDouble(busPosition.getLng());
            double latitude = Double.parseDouble(busPosition.getLat());
//            Log.e("经纬度",busPosition.getLng()+" "+busPosition.getLat());
//            latLngs.add(new LatLng(latitude, longitude));
            LatLng latLng = new LatLng(latitude, longitude);
            aMap.addMarker(new MarkerOptions().position(latLng));
//            clusterItem.add(new RegionItem(latLng,""));
        }
//        ClusterOverlay mClusterOverlay = new ClusterOverlay(aMap, clusterItem,
//                dp2px(getApplicationContext(), 2),
//                getApplicationContext());*/
        Iterator iterator = busPositionMap.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) iterator.next();
//            Object key = entry.getKey();
            LatLng val = (LatLng)entry.getValue();
            aMap.addMarker(new MarkerOptions().position(val));
        }
    }

    public static int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void getBusData(/*String responseData*/) {
        new Thread(busData).start();
//        busDataList = gson.fromJson(responseData, new TypeToken<List<BusDataGson>>() {
//        }.getType());
//        toast(busDataList.get(3).getGPSDeviceIMEI());
    }

    private Runnable busPosition = new Runnable() {
        @Override
        public void run() {
            try {
                request = new Request.Builder()
                        .url("http://111.231.201.179/android/bus/location")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                if (responseData != null && responseCode.charAt(0) == '2') {
                    try {
                        busPositionList = gson.fromJson(responseData, new TypeToken<List<BusPositionGson>>() {
                        }.getType());
                        for (BusPositionGson position : busPositionList) {
                            double longitude = Double.parseDouble(position.getLng());
                            double latitude = Double.parseDouble(position.getLat());
                            busPositionMap.put(position.getGPSDeviceIMEI(), new LatLng(latitude, longitude));
                        }
                        addPoints();
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("数据解析错误");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("网络异常");
            }
        }
    };

    private Runnable busData = new Runnable() {
        @Override
        public void run() {
            try {
                request = new Request.Builder()
                        .url("http://111.231.201.179/android/bus")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                if (responseData != null && responseCode.charAt(0) == '2') {
                    try {
                        busDataList = gson.fromJson(responseData, new TypeToken<List<BusDataGson>>() {
                        }.getType());
                        for (BusDataGson data : busDataList) {
                            busDataMap.put(data.getGPSDeviceIMEI(), data.getBus_lineName() + "\n" + data.getBus_departureSite());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("数据获取失败");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("网络异常");
            }
        }
    };

    private void translate() {
        // 获取轨迹坐标点
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(39.906901, 116.397972));
        points.add(new LatLng(42.906901, 117.397982));

        LatLngBounds bounds = new LatLngBounds(points.get(0), points.get(points.size() - 2));
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

        SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher));// 设置滑动的图标

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
    }

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
        getBusData();
        busPositionMap.clear();
        getBusPosition();
        disconnect();
        linkMQTT();
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

    /* MQTT */
    private void linkMQTT() {
        new Thread(mqtt).start();
    }

    private Runnable mqtt = new Runnable() {
        @Override
        public void run() {
            try {
                request = new Request.Builder()
                        .url("http://111.231.201.179/bus/mqtt")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                if (responseData != null && responseCode.charAt(0) == '2') {
                    try {
                        AccountGson account = gson.fromJson(responseData, AccountGson.class);
                        String username = account.getUsername();
                        String password = account.getPassword();
                        TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                        @SuppressLint("MissingPermission") String ssn = tm.getSimSerialNumber();

                        mqttOptions.setUserName(username);
                        mqttOptions.setPassword(password.toCharArray());
                        mqttClient = new MqttClient("tcp://111.231.201.179:1880", ssn, new MemoryPersistence());

                        mqttClient.setCallback(mqttCallback);
                        mqttClient.connect(mqttOptions);
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("连接服务器失败");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private IMqttActionListener mqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {


        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {

        }
    };


    MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            toast(cause.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            toast(topic);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    /*private void requestData(String url, String method) {
        RequestRunnable runnable = new RequestRunnable();
        runnable.setUrl(url);
        runnable.setMethod(method);
        new Thread(runnable).start();
    }*/

    /*private class RequestRunnable implements Runnable {
        private String url = "";
        private String method = "";

        public void setUrl(String url) {
            this.url = url;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        @Override
        public void run() {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                if (responseData != null && responseCode.charAt(0) == '2') {
                    try {
                        Method m=MainActivity.class.getDeclaredMethod(method, new Class[]{String.class});
                        m.setAccessible(true);
                        m.invoke(MainActivity.class.newInstance(), new String[]{responseData});
                    } catch (Exception e) {
                        e.printStackTrace();
                        toast("数据解析错误");
                        Log.e("ERROR", e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("网络异常");
            }
        }
    }*/
}
