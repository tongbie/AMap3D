package com.example.amap3d;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.amap3d.Gsons.BusDataGson;
import com.example.amap3d.Gsons.BusMoveGson;
import com.example.amap3d.Gsons.BusPositionGson;
import com.example.amap3d.Managers.AMapManager;
import com.example.amap3d.Managers.MQTTManager;
import com.example.amap3d.Views.RefreshButton;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MapView mapView;//地图控件
    private AMap aMap;//定位对象
    public static MQTTManager mqttManager;
    public static AMapManager aMapManager;

    private HashMap<String, String[]> busDataMap;//校车信息
    private HashMap<String, Marker> markerMap;//校车定位点
    private List<BusPositionGson> busPositionList = new ArrayList<>();//校车位置

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

    private void init() {
        mqttManager = new MQTTManager(getApplicationContext(), MainActivity.this);
        aMapManager = new AMapManager(getApplicationContext(), MainActivity.this);
        findViewById(R.id.refresh).setOnClickListener(this);
        markerMap = new HashMap<>();
        busDataMap = new HashMap<>();
    }

    private boolean isFirstMove = true;//用以判断在启动时移动地图至定位点

    private void setAMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));//缩放
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
        aMap.setOnMarkerClickListener(aMapManager.markerClickListener);

        UiSettings mUiSettings;//地图不可旋转，3D
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setRotateGesturesEnabled(false);
        mUiSettings.setTiltGesturesEnabled(false);

        aMap.setInfoWindowAdapter(aMapManager.infoWindowAdapter);
        aMap.setOnInfoWindowClickListener(aMapManager.infoWindowClickListener);
    }

    private void setLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(1500);//连续定位间隔
        myLocationStyle.showMyLocation(true);
        myLocationStyle.radiusFillColor(Color.parseColor("#00000000"));//定位精度圈透明
        myLocationStyle.strokeColor(Color.parseColor("#00000000"));//定位精度圈边缘透明
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.me));
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setMyLocationEnabled(true);
    }

    /* 获取校车信息 */
    private void initBusData() {
        try {
            Request request = new Request.Builder()
                    .url("http://111.231.201.179/android/bus")
                    .build();
            Response response = mqttManager.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    Log.e("busDataRunnable", responseData);
                    List<BusDataGson> busDatas = mqttManager.gson.fromJson(responseData, new TypeToken<List<BusDataGson>>() {
                    }.getType());
                    for (BusDataGson busData : busDatas) {
                        String key = busData.getGPSDeviceIMEI();
                        String title = busData.getBus_lineName() + "\n" + busData.getBus_departureSite();
                        String snippet = Pattern.compile("[^0-9]").matcher(busData.getBus_arriveSite()).replaceAll("");
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

    /* 添加校车定位点 */
    private void initPoints() {
        if (busPositionList == null) {
            toast("数据异常");
            return;
        }
        for (BusPositionGson busPosition : busPositionList) {
            String key = busPosition.getGPSDeviceIMEI();
            LatLng latLng = new LatLng(Double.parseDouble(busPosition.getLat()), Double.parseDouble(busPosition.getLng()));
            Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title(busDataMap.get(key)[0]).snippet(busDataMap.get(key)[1]));
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus1));
            markerMap.put(key, marker);
        }
    }

    /* 获取校车位置 */
    private void initBusPosition() {
        try {
            Request request = new Request.Builder()
                    .url("http://111.231.201.179/android/bus/location")
                    .build();
            Response response = mqttManager.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    Log.e("busPositionRunnable", responseData);
                    busPositionList = mqttManager.gson.fromJson(responseData, new TypeToken<List<BusPositionGson>>() {
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

    private Thread thread;

    /* 获取数据，用以刷新 */
    private void initData() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                initBusData();
                initBusPosition();
                initPoints();
                mqttManager.linkMQTT(mqttCallback);
                ((RefreshButton) findViewById(R.id.refresh)).setRefreshing(false);
                isRefreshing = false;
            }
        });
        thread.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh:
                ((RefreshButton) v).setRefreshing(true);
                toast("正在刷新...");
                refresh();
                break;
        }
    }

    private boolean isRefreshing = false;

    private void refresh() {
        if (isRefreshing) {
            return;
        }
        thread = null;
        aMap.clear();
        busDataMap.clear();
        busPositionList.clear();
        markerMap.clear();
        mqttManager.disconnect();
        isRefreshing = true;
        isFristReconnect = true;
        initData();
    }

    private boolean isFristReconnect = true;//掉线只进行一次重连

    /* MQTT接收消息回调 */
    MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            if (isFristReconnect) {
                toast("正在尝试重连");
                isFristReconnect = false;
                refresh();
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                List<BusMoveGson> busMoveGsons = mqttManager.gson.fromJson(message.toString(), new TypeToken<List<BusMoveGson>>() {
                }.getType());
                for (BusMoveGson busMoveGson : busMoveGsons) {
                    String key = busMoveGson.getGPSDeviceIMEI();
                    LatLng latLng = markerMap.get(key).getPosition();
                    double lat = latLng.latitude;
                    double lng = latLng.longitude;
                    markerMap.get(key).setPosition(new LatLng(busMoveGson.getLat(), busMoveGson.getLng()));
                    markerMap.get(key).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus2));
                    moveMarker(new LatLng[]{new LatLng(lat, lng), markerMap.get(key).getPosition()}, key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    /* 设置平滑移动点 */
    private void moveMarker(LatLng[] latLngs, final String key) {
        //TODO：移动点是一个新的对象，不能添加信息
        final SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.bus_move));
        smoothMarker.setPoints(Arrays.asList(latLngs));
        smoothMarker.setTotalDuration(3);
        smoothMarker.startSmoothMove();
        markerMap.get(key).setVisible(false);
        smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
            @Override
            public void move(double v) {
                if (v == 0) {//参数v为距终点距离
                    smoothMarker.stopMove();
                    smoothMarker.removeMarker();
                    markerMap.get(key).setVisible(true);
                }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0x003:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast("未获得拨号权限，无法拨打电话");
                }
                break;
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
        mqttManager.disconnect();
        System.exit(0);
    }
}
