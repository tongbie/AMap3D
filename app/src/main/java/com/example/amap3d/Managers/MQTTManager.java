package com.example.amap3d.Managers;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.example.amap3d.Datas;
import com.example.amap3d.Gsons.BusMoveGson;
import com.example.amap3d.Gsons.MQTTAccountGson;
import com.example.amap3d.R;
import com.example.amap3d.Utils;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.SocketTimeoutException;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/3/20.
 */

public class MQTTManager {
    private MqttConnectOptions mqttOptions;
    private MqttClient mqttClient;
    private String clientId;
    private String applyForMqttAccountURL = "http://bus.mysdnu.cn/bus/mqtt";
    private String linkMqttURL = "tcp://bus.mysdnu.cn:1880";
    private String mqttTopic="BusMoveList";

    public MQTTManager() {
        mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);
        mqttOptions.setConnectionTimeout(20);//超时时间20s
        mqttOptions.setKeepAliveInterval(120);//心跳时间，用以服务端判断客户端在线状态

        clientId = "35" + Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10;

        try {
            mqttClient = new MqttClient(linkMqttURL, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
            Utils.uiToast("Mqtt连接创建失败");
        }
    }

    /* 连接MQTT服务器 */
    public synchronized void linkMQTT(MqttCallback mqttCallback) {
        if (mqttClient.isConnected()) {
            Utils.uiToast("服务器已连接");
            return;
        }
        try {
            Request request = new Request.Builder()
                    .url(applyForMqttAccountURL)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    MQTTAccountGson account = Utils.gson.fromJson(responseData, MQTTAccountGson.class);
                    String username = account.getUsername();
                    String password = account.getPassword();
                    //断线不支持重连，必须申请新的username
                    mqttOptions.setUserName(username);
                    mqttOptions.setPassword(password.toCharArray());
                    mqttClient = new MqttClient(linkMqttURL, clientId, new MemoryPersistence());
                    mqttClient.setCallback(mqttCallback);
                    mqttClient.connect(mqttOptions);
                    subscribeMqttTopic(mqttTopic, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.uiToast("连接服务器失败");
                }
            }
        }catch (SocketTimeoutException e){
            Utils.uiToast("连接超时，请检查网络设置");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 订阅消息 */
    public void subscribeMqttTopic(String topic, int qos) {
        if (mqttClient != null) {
            try {
                mqttClient.subscribe(topic, qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Utils.uiToast("连接失败，请刷新重试");
            Log.e("MqttConnectionLost",cause.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                List<BusMoveGson> busMoveGsons = Utils.gson.fromJson(message.toString(), new TypeToken<List<BusMoveGson>>() {
                }.getType());
                for (BusMoveGson busMoveGson : busMoveGsons) {
                    String key = busMoveGson.getGPSDeviceIMEI();
                    LatLng latLng = Datas.busMarkerMap.get(key).getPosition();
                    double lat = latLng.latitude;
                    double lng = latLng.longitude;
                    Datas.busMarkerMap.get(key).setPosition(new LatLng(busMoveGson.getLat(), busMoveGson.getLng()));
                    Datas.busMarkerMap.get(key).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus2));
                    AMapManager.moveMarker(AMapManager.aMap, new LatLng[]{new LatLng(lat, lng), Datas.busMarkerMap.get(key).getPosition()}, key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    /* 断开连接 */
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy(){
        disconnect();
    }
}
