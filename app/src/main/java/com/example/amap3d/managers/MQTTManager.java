package com.example.amap3d.managers;

import android.os.Build;
import android.util.Log;

import com.example.amap3d.gsons.MQTTAccountGson;
import com.example.amap3d.utils.Utils;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.SocketTimeoutException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/3/20.
 */

public class MQTTManager {
    private static MQTTManager mqttManager;
    private MqttConnectOptions mqttOptions;
    public MqttClient mqttClient;
    public static String deviceId;
    private static final String applyForMqttAccountURL = "http://bus.mysdnu.cn/bus/mqtt";
    private static final String linkMqttURL = "tcp://bus.mysdnu.cn:1880";
    private static final String mqttTopic = "BusMoveList";
    public boolean isShowMoving = true;

    public static MQTTManager getInstance() {
        if (mqttManager == null) {
            mqttManager = new MQTTManager();
        }
        return mqttManager;
    }

    private MQTTManager() {
        mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);
        mqttOptions.setConnectionTimeout(10);//超时时间20s
        mqttOptions.setKeepAliveInterval(120);//心跳时间，用以服务端判断客户端在线状态

        deviceId = "35" + Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10;
    }

    /* 连接MQTT服务器 */
    public synchronized void linkMQTT(MqttCallback mqttCallback) {
        if (mqttClient != null && mqttClient.isConnected()) {
            return;
        }
        try {
            Request request = new Request.Builder()
                    .url(applyForMqttAccountURL)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseCode.charAt(0) == '2') {
                    MQTTAccountGson account = Utils.gson.fromJson(responseData, MQTTAccountGson.class);
                    String username = account.getUsername();
                    String password = account.getPassword();
                    //断线不支持重连，必须申请新的username
                    mqttOptions.setUserName(username);
                    mqttOptions.setPassword(password.toCharArray());
                    mqttClient = new MqttClient(linkMqttURL, deviceId, new MemoryPersistence());
                    mqttClient.setCallback(mqttCallback);
                    mqttClient.connect(mqttOptions);
                    subscribeTopic(mqttTopic);
                    subscribeTopic(PeopleManager.uploadPositionTitle);
            }
        } catch (SocketTimeoutException e) {
            Utils.uiToast("连接超时，请检查网络设置");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("连接服务器失败");
        }
    }

    /* 订阅消息 */
    private void subscribeTopic(String topic) {
        if (mqttClient != null) {
            try {
                mqttClient.subscribe(topic, 0);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e("subscribeTopic", e.getMessage());
            }
        }
    }

    public void publish(String topic, String msg, boolean isRetained) {
        try {
            int qos = 0;
            if (mqttClient != null) {
                MqttMessage message = new MqttMessage();
                message.setQos(qos);
                message.setRetained(isRetained);
                message.setPayload(msg.getBytes());
                mqttClient.publish(topic, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Utils.uiToast("失去连接，请刷新重试");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            if (isShowMoving && topic.equals(mqttTopic)) {
                BusManager.getInstance().moveBus(message);
            } else if (topic.equals(PeopleManager.uploadPositionTitle)) {
                PeopleManager.getInstance().mqttUpload(message);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    /* 断开连接 */
    private void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy() {
        mqttClient = null;
        mqttOptions = null;
        disconnect();
    }
}
