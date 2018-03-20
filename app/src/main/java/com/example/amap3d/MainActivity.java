package com.example.amap3d;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.example.amap3d.Gsons.AccountGson;
import com.example.amap3d.Gsons.BusDataGson;
import com.example.amap3d.Gsons.BusMoveGson;
import com.example.amap3d.Gsons.BusPositionGson;
import com.example.amap3d.MyView.MyButton;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
    private HashMap<String, Marker> markerMap;
    private List<BusPositionGson> busPositionList = new ArrayList<>();

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
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.me));
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

        markerMap = new HashMap<>();
        busDataMap = new HashMap<>();

        mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);
        mqttOptions.setConnectionTimeout(20);// 设置超时时间 单位为秒
        mqttOptions.setKeepAliveInterval(120);
    }

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


    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
//            Iterator iterator = new HashMap().entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry entry = (Map.Entry) iterator.next();
//                String key = (String) entry.getKey();
//                markerMap.get(key).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus1));
//            }
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
//                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus2));
            }
            return true;
        }
    };

    private Thread thread;

    private void initData() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                initBusData();
                initBusPosition();
                initPoints();
                initMQTT();
            }
        });
        thread.start();
    }

    private void initBusData() {
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

    private void initBusPosition() {
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

    private void initMQTT() {
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
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_PHONE_STATE}, 0x002);
                        thread.sleep(99999);
                    }
                    String ssn = tm.getSimSerialNumber();
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

        aMap.setInfoWindowAdapter(infoWindowAdapter);
    }

    AMap.InfoWindowAdapter infoWindowAdapter = new AMap.ImageInfoWindowAdapter() {
        View infoWindow = null;

        @Override
        public long getInfoWindowUpdateTime() {
            return 0;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            if (infoWindow == null) {
                infoWindow = LayoutInflater.from(getApplicationContext()).inflate(
                        R.layout.infowindow, null);
            }
            ((TextView) infoWindow.findViewById(R.id.text)).setText(marker.getTitle());
            Button button = infoWindow.findViewById(R.id.button);
            final String num = marker.getSnippet();
            button.setText(num);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog dialog = new AlertDialog.Builder(getApplicationContext())
                            .setTitle("拨号")
                            .setMessage("是否拨打 " + num)
                            .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                @SuppressLint("MissingPermission")
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CALL_PHONE}, 0x003);
                                        return;
                                    }
                                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + "18340096853"));
                                    startActivity(intent);
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    dialog.dismiss();
                                }
                            }).create();
                    dialog.show();
                }
            });
            return infoWindow;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh:
                ((MyButton) findViewById(R.id.refresh)).setRefreshing(true);
                refresh();
                break;
        }
    }

    private void refresh() {
        thread = null;
        aMap.clear();
        busDataMap.clear();
        busPositionList.clear();
        markerMap.clear();
        disconnect();
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

    private boolean isFristReconnect = true;

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
                List<BusMoveGson> busMoveGsons = gson.fromJson(message.toString(), new TypeToken<List<BusMoveGson>>() {
                }.getType());
                for (BusMoveGson busMoveGson : busMoveGsons) {
                    String key = busMoveGson.getGPSDeviceIMEI();
                    LatLng latLng = markerMap.get(key).getPosition();
                    double lat = latLng.latitude;
                    double lng = latLng.longitude;
                    if (markerMap.get(key).getTitle().charAt(0) == '3') {
                        Log.e("new", String.valueOf(busMoveGson.getLat()) + " " + String.valueOf(busMoveGson.getLng()));
                    }
                    markerMap.get(key).setPosition(new LatLng(busMoveGson.getLat(), busMoveGson.getLng()));
                    markerMap.get(key).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus2));
                    translate(new LatLng[]{new LatLng(lat, lng), markerMap.get(key).getPosition()}, key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };


    private void translate(LatLng[] latLngs, final String key) {
        final SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.bus_move));
        smoothMarker.setPoints(Arrays.asList(latLngs));
        smoothMarker.setTotalDuration(3);
        smoothMarker.startSmoothMove();
        markerMap.get(key).setVisible(false);
        smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
            @Override
            public void move(double v) {
                if (v == 0) {
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
        disconnect();
        System.exit(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0x002:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(thread!=null){
//                        thread.
                    }
                } else {
                    toast("未获得读取电话状态权限，无法连接服务器");
                    finish();
                }
                break;
            case 0x003:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast("未获得拨号权限，无法拨打电话");
                }
                break;
        }
        refresh();
    }
}
